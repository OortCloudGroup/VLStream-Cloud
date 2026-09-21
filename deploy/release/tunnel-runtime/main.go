package main

import (
	"context"
	"errors"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	if len(os.Args) == 2 && os.Args[1] == "--healthcheck" {
		if err := runHealthcheck(); err != nil {
			log.Printf("healthcheck failed: %v", err)
			os.Exit(1)
		}
		return
	}
	cfg, err := loadRuntimeConfig()
	if err != nil {
		log.Fatalf("invalid tunnel runtime configuration: %v", err)
	}
	backend := newBackendClient(cfg)
	controller := newRatholeController(cfg, backend)
	gateway := newTunnelGateway(cfg, backend)

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()
	go controller.run(ctx)

	server := &http.Server{
		Addr:              cfg.GatewayListenAddress,
		Handler:           gateway,
		ReadHeaderTimeout: 15 * time.Second,
		IdleTimeout:       5 * time.Minute,
		MaxHeaderBytes:    64 * 1024,
	}
	go func() {
		<-ctx.Done()
		shutdownContext, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer shutdownCancel()
		_ = server.Shutdown(shutdownContext)
	}()

	log.Printf("starting %s", gateway)
	if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("tunnel gateway stopped unexpectedly: %v", err)
	}
}

func runHealthcheck() error {
	_, port, err := net.SplitHostPort(env("TUNNEL_GATEWAY_LISTEN_ADDRESS", "0.0.0.0:8088"))
	if err != nil {
		return err
	}
	client := &http.Client{Timeout: 2 * time.Second}
	response, err := client.Get("http://127.0.0.1:" + port + "/__health")
	if err != nil {
		return err
	}
	defer response.Body.Close()
	_, _ = io.Copy(io.Discard, io.LimitReader(response.Body, 1024))
	if response.StatusCode != http.StatusOK {
		return errors.New("gateway health endpoint is not ready")
	}
	return nil
}
