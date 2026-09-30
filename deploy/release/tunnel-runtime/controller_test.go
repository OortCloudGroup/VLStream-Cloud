package main

import (
	"strings"
	"testing"
)

func TestRenderRatholeConfigIncludesOnlyEnabledLoopbackRoutes(t *testing.T) {
	cfg := runtimeConfig{
		RatholeListenAddress: "0.0.0.0:2333",
		NoisePrivateKey:      "server-private-key-value-0123456789",
	}
	routes := []desiredRoute{
		{
			EndpointID:       "1",
			ServiceName:      "ipc_1",
			ServiceToken:     "service-token-value-012345678901234",
			BindAddress:      "127.0.0.1:61000",
			ConfigGeneration: 2,
			DesiredState:     "ENABLED",
		},
		{
			EndpointID:       "2",
			ServiceName:      "ipc_2",
			ServiceToken:     "service-token-value-987654321098765",
			BindAddress:      "127.0.0.1:61001",
			ConfigGeneration: 3,
			DesiredState:     "DISABLED",
		},
	}
	config, err := renderRatholeConfig(cfg, routes)
	if err != nil {
		t.Fatalf("render config: %v", err)
	}
	text := string(config)
	if !strings.Contains(text, "[server.services.ipc_1]") {
		t.Fatal("enabled service was not rendered")
	}
	if strings.Contains(text, "ipc_2") {
		t.Fatal("disabled service must not remain in the rathole server config")
	}
	if !strings.Contains(text, `bind_addr = "127.0.0.1:61000"`) {
		t.Fatal("service must bind to its loopback address")
	}
}

func TestRenderRatholeConfigRejectsPublicServiceBinding(t *testing.T) {
	cfg := runtimeConfig{
		RatholeListenAddress: "0.0.0.0:2333",
		NoisePrivateKey:      "server-private-key-value-0123456789",
	}
	_, err := renderRatholeConfig(cfg, []desiredRoute{{
		EndpointID:       "1",
		ServiceName:      "ipc_1",
		ServiceToken:     "service-token-value-012345678901234",
		BindAddress:      "0.0.0.0:61000",
		ConfigGeneration: 1,
		DesiredState:     "ENABLED",
	}})
	if err == nil {
		t.Fatal("public service binding must be rejected")
	}
}

func TestRenderRatholeConfigKeepsRequiredServicesTableWithoutActiveRoutes(t *testing.T) {
	cfg := runtimeConfig{RatholeListenAddress: "127.0.0.1:2333", NoisePrivateKey: "test-key"}
	for _, routes := range [][]desiredRoute{nil, {{ServiceName: "disabled", DesiredState: "DISABLED"}}} {
		config, err := renderRatholeConfig(cfg, routes)
		if err != nil {
			t.Fatal(err)
		}
		if !strings.Contains(string(config), "[server.services]\n") || strings.Contains(string(config), "[server.services.") {
			t.Fatal("rathole requires an explicit empty services table before any device is enrolled")
		}
	}
}
