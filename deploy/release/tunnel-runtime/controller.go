package main

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"log"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

var serviceNamePattern = regexp.MustCompile(`^[A-Za-z0-9_-]{1,128}$`)

type ratholeController struct {
	cfg     runtimeConfig
	backend *backendClient

	mu           sync.Mutex
	command      *exec.Cmd
	commandDone  chan error
	configDigest string
	lastSuccess  time.Time
}

func newRatholeController(cfg runtimeConfig, backend *backendClient) *ratholeController {
	return &ratholeController{cfg: cfg, backend: backend}
}

func (controller *ratholeController) run(ctx context.Context) {
	ticker := time.NewTicker(controller.cfg.PollInterval)
	defer ticker.Stop()
	defer controller.stopProcess()

	for {
		if err := controller.reconcile(ctx); err != nil {
			log.Printf("tunnel route reconciliation failed: %v", err)
			controller.stopWhenStale()
		}
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}

func (controller *ratholeController) reconcile(ctx context.Context) error {
	routes, err := controller.backend.fetchRoutes(ctx)
	if err != nil {
		return err
	}
	configBytes, err := renderRatholeConfig(controller.cfg, routes)
	if err != nil {
		controller.reportAll(ctx, routes, "FAILED", err.Error())
		return err
	}
	digest := sha256.Sum256(configBytes)
	digestText := hex.EncodeToString(digest[:])

	controller.mu.Lock()
	processRunning := controller.processRunningLocked()
	sameConfig := controller.configDigest == digestText
	controller.mu.Unlock()

	if !processRunning || !sameConfig {
		if err := controller.installAndRestart(configBytes, digestText); err != nil {
			controller.reportAll(ctx, routes, "FAILED", err.Error())
			return err
		}
	}

	controller.reportAll(ctx, routes, "APPLIED", "")
	controller.mu.Lock()
	controller.lastSuccess = time.Now()
	controller.mu.Unlock()
	return nil
}

func (controller *ratholeController) installAndRestart(configBytes []byte, digest string) error {
	if err := os.MkdirAll(filepath.Dir(controller.cfg.RatholeConfigPath), 0700); err != nil {
		return fmt.Errorf("cannot create rathole config directory: %w", err)
	}
	tempPath := controller.cfg.RatholeConfigPath + ".tmp"
	if err := os.WriteFile(tempPath, configBytes, 0600); err != nil {
		return fmt.Errorf("cannot write temporary rathole config: %w", err)
	}
	if err := os.Chmod(tempPath, 0600); err != nil {
		return fmt.Errorf("cannot secure temporary rathole config: %w", err)
	}
	if err := os.Rename(tempPath, controller.cfg.RatholeConfigPath); err != nil {
		return fmt.Errorf("cannot atomically install rathole config: %w", err)
	}

	controller.stopProcess()
	command := exec.Command(controller.cfg.RatholeBinary, "--server", controller.cfg.RatholeConfigPath)
	command.Stdout = os.Stdout
	command.Stderr = os.Stderr
	if err := command.Start(); err != nil {
		return fmt.Errorf("cannot start rathole: %w", err)
	}
	done := make(chan error, 1)
	go func() {
		done <- command.Wait()
	}()
	select {
	case err := <-done:
		if err == nil {
			err = errors.New("rathole exited before becoming ready")
		}
		return err
	case <-time.After(time.Second):
	}

	controller.mu.Lock()
	controller.command = command
	controller.commandDone = done
	controller.configDigest = digest
	controller.mu.Unlock()
	return nil
}

func (controller *ratholeController) stopWhenStale() {
	controller.mu.Lock()
	lastSuccess := controller.lastSuccess
	controller.mu.Unlock()
	if lastSuccess.IsZero() || time.Since(lastSuccess) >= controller.cfg.StaleAfter {
		log.Printf("control plane has been stale for %s; stopping rathole fail-closed",
			controller.cfg.StaleAfter)
		controller.stopProcess()
	}
}

func (controller *ratholeController) processRunningLocked() bool {
	if controller.command == nil || controller.commandDone == nil {
		return false
	}
	select {
	case err := <-controller.commandDone:
		log.Printf("rathole exited: %v", err)
		controller.command = nil
		controller.commandDone = nil
		return false
	default:
		return true
	}
}

func (controller *ratholeController) stopProcess() {
	controller.mu.Lock()
	command := controller.command
	done := controller.commandDone
	controller.command = nil
	controller.commandDone = nil
	controller.mu.Unlock()
	if command == nil || command.Process == nil {
		return
	}
	_ = command.Process.Signal(os.Interrupt)
	if done != nil {
		select {
		case <-done:
			return
		case <-time.After(5 * time.Second):
		}
	}
	_ = command.Process.Kill()
	if done != nil {
		select {
		case <-done:
		case <-time.After(2 * time.Second):
		}
	}
}

func (controller *ratholeController) reportAll(ctx context.Context, routes []desiredRoute,
	status, message string) {
	for _, route := range routes {
		reportCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
		err := controller.backend.reportRouteStatus(reportCtx, route, status,
			truncate(message, 240))
		cancel()
		if err != nil {
			log.Printf("cannot report route status for endpoint %s: %v", route.EndpointID, err)
		}
	}
}

func renderRatholeConfig(cfg runtimeConfig, routes []desiredRoute) ([]byte, error) {
	var output bytes.Buffer
	fmt.Fprintf(&output, "[server]\n")
	fmt.Fprintf(&output, "bind_addr = %s\n", tomlString(cfg.RatholeListenAddress))
	fmt.Fprintf(&output, "heartbeat_interval = 30\n\n")
	fmt.Fprintf(&output, "[server.transport]\n")
	fmt.Fprintf(&output, "type = \"noise\"\n\n")
	fmt.Fprintf(&output, "[server.transport.noise]\n")
	fmt.Fprintf(&output, "local_private_key = %s\n", tomlString(cfg.NoisePrivateKey))

	sorted := append([]desiredRoute(nil), routes...)
	sort.Slice(sorted, func(i, j int) bool {
		return sorted[i].ServiceName < sorted[j].ServiceName
	})
	seenNames := make(map[string]bool)
	seenPorts := make(map[int]bool)
	for _, route := range sorted {
		if route.DesiredState != "ENABLED" {
			continue
		}
		if !serviceNamePattern.MatchString(route.ServiceName) {
			return nil, fmt.Errorf("endpoint %s has an invalid service name", route.EndpointID)
		}
		if len(route.ServiceToken) < 32 {
			return nil, fmt.Errorf("endpoint %s has a short service token", route.EndpointID)
		}
		host, rawPort, err := net.SplitHostPort(route.BindAddress)
		if err != nil {
			return nil, fmt.Errorf("endpoint %s has an invalid bind address", route.EndpointID)
		}
		if host != "127.0.0.1" && host != "::1" && host != "localhost" {
			return nil, fmt.Errorf("endpoint %s bind address is not loopback", route.EndpointID)
		}
		port, err := strconv.Atoi(rawPort)
		if err != nil || port < 1024 || port > 65535 {
			return nil, fmt.Errorf("endpoint %s has an invalid bind port", route.EndpointID)
		}
		if seenNames[route.ServiceName] || seenPorts[port] {
			return nil, fmt.Errorf("duplicate rathole service name or bind port")
		}
		seenNames[route.ServiceName] = true
		seenPorts[port] = true

		fmt.Fprintf(&output, "\n[server.services.%s]\n", route.ServiceName)
		fmt.Fprintf(&output, "type = \"tcp\"\n")
		fmt.Fprintf(&output, "token = %s\n", tomlString(route.ServiceToken))
		fmt.Fprintf(&output, "bind_addr = %s\n", tomlString(route.BindAddress))
	}
	return output.Bytes(), nil
}

func tomlString(value string) string {
	replacer := strings.NewReplacer("\\", "\\\\", "\"", "\\\"", "\n", "\\n", "\r", "\\r")
	return "\"" + replacer.Replace(value) + "\""
}

func truncate(value string, limit int) string {
	if len(value) <= limit {
		return value
	}
	return value[:limit]
}
