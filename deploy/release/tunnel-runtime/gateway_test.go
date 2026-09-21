package main

import (
	"encoding/json"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"
)

func TestGatewayBootstrapsHostBoundSessionAndProxiesToLoopback(t *testing.T) {
	const sessionID = "8f4f33db-1707-470d-9f68-b35c1fa06ca5"
	const accessToken = "one-time-access-token"
	const gatewayToken = "gateway-token-0123456789-0123456789"

	var upstreamHost string
	var upstreamForwardedFor string
	upstream := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		upstreamHost = request.Host
		upstreamForwardedFor = request.Header.Get("X-Forwarded-For")
		http.SetCookie(response, &http.Cookie{
			Name:   "ipc-login",
			Value:  "session",
			Domain: "192.168.1.100",
			Path:   "/",
		})
		_, _ = response.Write([]byte("ipc-ok"))
	}))
	defer upstream.Close()
	_, rawPort, err := net.SplitHostPort(strings.TrimPrefix(upstream.URL, "http://"))
	if err != nil {
		t.Fatal(err)
	}
	upstreamPort, _ := strconv.Atoi(rawPort)

	backend := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		if request.Header.Get("X-Tunnel-Gateway-Token") != gatewayToken {
			http.Error(response, "missing gateway token", http.StatusUnauthorized)
			return
		}
		if request.Header.Get("X-Tunnel-Access-Token") != accessToken {
			http.Error(response, "wrong access token", http.StatusUnauthorized)
			return
		}
		_ = json.NewEncoder(response).Encode(map[string]any{
			"code": 200,
			"data": map[string]any{
				"sessionId":    sessionID,
				"endpointId":   "1",
				"upstreamHost": "127.0.0.1",
				"upstreamPort": upstreamPort,
				"expiresAt":    time.Now().Add(time.Minute).UTC().Format(time.RFC3339),
			},
		})
	}))
	defer backend.Close()

	cfg := runtimeConfig{
		BackendBaseURL:    backend.URL,
		ControlToken:      "control-token-0123456789-0123456789",
		GatewayToken:      gatewayToken,
		GatewayHostSuffix: ".ipc.test",
		PublicHTTPS:       true,
	}
	gateway := newTunnelGateway(cfg, newBackendClient(cfg))

	bootstrap := httptest.NewRequest(http.MethodGet,
		"https://"+sessionID+".ipc.test/s/"+accessToken, nil)
	bootstrap.Host = sessionID + ".ipc.test"
	bootstrapResponse := httptest.NewRecorder()
	gateway.ServeHTTP(bootstrapResponse, bootstrap)
	if bootstrapResponse.Code != http.StatusSeeOther {
		t.Fatalf("bootstrap status: got %d body=%s", bootstrapResponse.Code, bootstrapResponse.Body.String())
	}
	cookies := bootstrapResponse.Result().Cookies()
	if len(cookies) != 1 || cookies[0].Name != gatewayCookieName {
		t.Fatal("gateway did not issue its host-bound browser cookie")
	}
	if !cookies[0].Secure || !cookies[0].HttpOnly {
		t.Fatal("gateway browser cookie must be Secure and HttpOnly")
	}
	if gateway.cachedSessionCount() != 1 {
		t.Fatal("gateway did not cache the resolved route")
	}

	proxyRequest := httptest.NewRequest(http.MethodGet,
		"https://"+sessionID+".ipc.test/assets/app.js", nil)
	proxyRequest.Host = sessionID + ".ipc.test"
	proxyRequest.AddCookie(cookies[0])
	proxyResponse := httptest.NewRecorder()
	gateway.ServeHTTP(proxyResponse, proxyRequest)
	if proxyResponse.Code != http.StatusOK {
		t.Fatalf("proxy status: got %d body=%s", proxyResponse.Code, proxyResponse.Body.String())
	}
	body, _ := io.ReadAll(proxyResponse.Result().Body)
	if string(body) != "ipc-ok" {
		t.Fatalf("unexpected proxy body: %s", body)
	}
	if upstreamHost != sessionID+".ipc.test" {
		t.Fatalf("public host was not preserved for the IPC application: %s", upstreamHost)
	}
	if upstreamForwardedFor != "" {
		t.Fatalf("browser IP forwarding header leaked to IPC: %s", upstreamForwardedFor)
	}
	setCookie := proxyResponse.Header().Get("Set-Cookie")
	if strings.Contains(strings.ToLower(setCookie), "domain=") {
		t.Fatalf("private IPC cookie domain leaked to browser: %s", setCookie)
	}
}

func TestValidateSessionRouteRejectsNonLoopbackUpstream(t *testing.T) {
	err := validateSessionRoute(sessionRoute{
		SessionID:    "session",
		EndpointID:   "endpoint",
		UpstreamHost: "10.0.0.8",
		UpstreamPort: 80,
		ExpiresAt:    time.Now().Add(time.Minute),
	})
	if err == nil {
		t.Fatal("gateway must reject non-loopback upstreams")
	}
}

func TestHealthEndpointIsNotExposedOnWildcardHost(t *testing.T) {
	cfg := runtimeConfig{GatewayHostSuffix: ".ipc.test"}
	gateway := newTunnelGateway(cfg, nil)
	publicRequest := httptest.NewRequest(http.MethodGet, "https://session.ipc.test/__health", nil)
	publicRequest.Host = "session.ipc.test"
	publicResponse := httptest.NewRecorder()
	gateway.ServeHTTP(publicResponse, publicRequest)
	if publicResponse.Code != http.StatusNotFound {
		t.Fatalf("public health endpoint should be hidden, got %d", publicResponse.Code)
	}

	localRequest := httptest.NewRequest(http.MethodGet, "http://127.0.0.1:8088/__health", nil)
	localRequest.Host = "127.0.0.1:8088"
	localResponse := httptest.NewRecorder()
	gateway.ServeHTTP(localResponse, localRequest)
	if localResponse.Code != http.StatusOK {
		t.Fatalf("loopback health endpoint should be available, got %d", localResponse.Code)
	}
}

func TestRedirectRewriteKeepsExternalDestinations(t *testing.T) {
	privateResponse := &http.Response{Header: http.Header{"Location": []string{"http://192.168.1.100/login"}}}
	rewriteLocation(privateResponse, "session.ipc.test", "https", "127.0.0.1:61000")
	if got := privateResponse.Header.Get("Location"); got != "https://session.ipc.test/login" {
		t.Fatalf("private redirect was not rewritten: %s", got)
	}

	externalResponse := &http.Response{Header: http.Header{"Location": []string{"https://accounts.example.com/login"}}}
	rewriteLocation(externalResponse, "session.ipc.test", "https", "127.0.0.1:61000")
	if got := externalResponse.Header.Get("Location"); got != "https://accounts.example.com/login" {
		t.Fatalf("external redirect must remain unchanged: %s", got)
	}
}

func TestGatewayRejectsTokenOnWrongSessionHost(t *testing.T) {
	const expectedSession = "8f4f33db-1707-470d-9f68-b35c1fa06ca5"
	backend := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, _ *http.Request) {
		_ = json.NewEncoder(response).Encode(map[string]any{
			"code": 200,
			"data": map[string]any{
				"sessionId":    expectedSession,
				"endpointId":   "1",
				"upstreamHost": "127.0.0.1",
				"upstreamPort": 61000,
				"expiresAt":    time.Now().Add(time.Minute).UTC().Format(time.RFC3339),
			},
		})
	}))
	defer backend.Close()
	cfg := runtimeConfig{
		BackendBaseURL:    backend.URL,
		ControlToken:      "control-token-0123456789-0123456789",
		GatewayToken:      "gateway-token-0123456789-0123456789",
		GatewayHostSuffix: ".ipc.test",
	}
	gateway := newTunnelGateway(cfg, newBackendClient(cfg))
	request := httptest.NewRequest(http.MethodGet,
		"https://different-session.ipc.test/s/token", nil)
	request.Host = "different-session.ipc.test"
	response := httptest.NewRecorder()
	gateway.ServeHTTP(response, request)
	if response.Code != http.StatusForbidden {
		t.Fatalf("wrong session host should be forbidden, got %d", response.Code)
	}
}
