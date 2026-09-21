package main

import (
	"errors"
	"fmt"
	"net"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

type runtimeConfig struct {
	BackendBaseURL       string
	ControlToken         string
	GatewayToken         string
	GatewayListenAddress string
	GatewayHostSuffix    string
	PublicHTTPS          bool
	RatholeBinary        string
	RatholeConfigPath    string
	RatholeListenAddress string
	NoisePrivateKey      string
	PollInterval         time.Duration
	StaleAfter           time.Duration
}

func loadRuntimeConfig() (runtimeConfig, error) {
	cfg := runtimeConfig{
		BackendBaseURL:       strings.TrimRight(env("TUNNEL_BACKEND_BASE_URL", "http://backend:8080"), "/"),
		ControlToken:         strings.TrimSpace(os.Getenv("TUNNEL_CONTROL_API_TOKEN")),
		GatewayToken:         strings.TrimSpace(os.Getenv("TUNNEL_GATEWAY_API_TOKEN")),
		GatewayListenAddress: env("TUNNEL_GATEWAY_LISTEN_ADDRESS", "0.0.0.0:8088"),
		GatewayHostSuffix:    strings.ToLower(strings.TrimSpace(os.Getenv("TUNNEL_GATEWAY_HOST_SUFFIX"))),
		PublicHTTPS:          envBool("TUNNEL_PUBLIC_HTTPS", true),
		RatholeBinary:        env("TUNNEL_RATHOLE_BINARY", "/usr/local/bin/rathole"),
		RatholeConfigPath:    env("TUNNEL_RATHOLE_CONFIG_PATH", "/var/lib/vls-tunnel/server.toml"),
		RatholeListenAddress: env("TUNNEL_RATHOLE_LISTEN_ADDRESS", "0.0.0.0:2333"),
		NoisePrivateKey:      strings.TrimSpace(os.Getenv("TUNNEL_RATHOLE_NOISE_PRIVATE_KEY")),
		PollInterval:         envDuration("TUNNEL_CONTROL_POLL_INTERVAL", 10*time.Second),
		StaleAfter:           envDuration("TUNNEL_CONTROL_STALE_AFTER", 2*time.Minute),
	}

	if parsed, err := url.Parse(cfg.BackendBaseURL); err != nil || parsed.Host == "" ||
		(parsed.Scheme != "http" && parsed.Scheme != "https") {
		return runtimeConfig{}, errors.New("TUNNEL_BACKEND_BASE_URL must be an absolute HTTP(S) URL")
	}
	if len(cfg.ControlToken) < 32 || len(cfg.GatewayToken) < 32 {
		return runtimeConfig{}, errors.New("control and gateway API tokens must each contain at least 32 characters")
	}
	if cfg.ControlToken == cfg.GatewayToken {
		return runtimeConfig{}, errors.New("control and gateway API tokens must be different")
	}
	if !cfg.PublicHTTPS {
		return runtimeConfig{}, errors.New("TUNNEL_PUBLIC_HTTPS must remain true for IPC remote management")
	}
	if !strings.HasPrefix(cfg.GatewayHostSuffix, ".") || len(cfg.GatewayHostSuffix) < 4 {
		return runtimeConfig{}, errors.New("TUNNEL_GATEWAY_HOST_SUFFIX must start with a dot")
	}
	if err := validateListenAddress(cfg.GatewayListenAddress); err != nil {
		return runtimeConfig{}, fmt.Errorf("invalid gateway listen address: %w", err)
	}
	if err := validateListenAddress(cfg.RatholeListenAddress); err != nil {
		return runtimeConfig{}, fmt.Errorf("invalid rathole listen address: %w", err)
	}
	if len(cfg.NoisePrivateKey) < 32 {
		return runtimeConfig{}, errors.New("TUNNEL_RATHOLE_NOISE_PRIVATE_KEY is missing or too short")
	}
	if cfg.PollInterval < time.Second || cfg.PollInterval > time.Minute {
		return runtimeConfig{}, errors.New("control poll interval must be between 1s and 1m")
	}
	if cfg.StaleAfter < cfg.PollInterval*2 {
		return runtimeConfig{}, errors.New("control stale timeout must be at least two poll intervals")
	}
	if !filepath.IsAbs(cfg.RatholeConfigPath) {
		return runtimeConfig{}, errors.New("TUNNEL_RATHOLE_CONFIG_PATH must be absolute")
	}
	return cfg, nil
}

func validateListenAddress(value string) error {
	host, rawPort, err := net.SplitHostPort(value)
	if err != nil {
		return err
	}
	if strings.TrimSpace(host) == "" {
		return errors.New("host is empty")
	}
	port, err := strconv.Atoi(rawPort)
	if err != nil || port < 1 || port > 65535 {
		return errors.New("port is outside 1-65535")
	}
	return nil
}

func env(name, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(name)); value != "" {
		return value
	}
	return fallback
}

func envBool(name string, fallback bool) bool {
	value := strings.TrimSpace(os.Getenv(name))
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	return err == nil && parsed
}

func envDuration(name string, fallback time.Duration) time.Duration {
	value := strings.TrimSpace(os.Getenv(name))
	if value == "" {
		return fallback
	}
	parsed, err := time.ParseDuration(value)
	if err != nil {
		return fallback
	}
	return parsed
}
