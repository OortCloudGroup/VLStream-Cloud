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
	issuedAt := time.Now().UTC().Truncate(time.Second)
	expiresAt := issuedAt.Add(10 * time.Hour)
	now := issuedAt

	var upstreamHost string
	var upstreamForwardedFor string
	var upstreamCookie string
	upstream := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		upstreamHost = request.Host
		upstreamForwardedFor = request.Header.Get("X-Forwarded-For")
		upstreamCookie = request.Header.Get("Cookie")
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
		switch request.URL.Path {
		case "/vlsTunnel/internal/access-sessions/resolve":
			if request.Header.Get("X-Tunnel-Access-Token") != accessToken {
				http.Error(response, "wrong access token", http.StatusUnauthorized)
				return
			}
		case "/vlsTunnel/internal/access-sessions/renew":
			if request.Header.Get("X-Tunnel-Session-Id") != sessionID || request.Header.Get("X-Tunnel-Access-Token") != "" {
				http.Error(response, "renewal must use session identity, not the one-time token", http.StatusUnauthorized)
				return
			}
			expiresAt = now.Add(10 * time.Hour)
		default:
			http.NotFound(response, request)
			return
		}
		_ = json.NewEncoder(response).Encode(map[string]any{
			"code": 200,
			"data": map[string]any{
				"sessionId":    sessionID,
				"endpointId":   "1",
				"upstreamHost": "127.0.0.1",
				"upstreamPort": upstreamPort,
				"expiresAt":    expiresAt.Format(time.RFC3339),
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
	gateway.now = func() time.Time { return now }

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
	if cookies[0].MaxAge != 36000 || !cookies[0].Expires.Equal(expiresAt) {
		t.Fatal("gateway cookie must preserve the ten-hour backend session expiry")
	}
	if gateway.cachedSessionCount() != 1 {
		t.Fatal("gateway did not cache the resolved route")
	}

	now = issuedAt.Add(9 * time.Hour)
	proxyRequest := httptest.NewRequest(http.MethodGet,
		"https://"+sessionID+".ipc.test/assets/app.js", nil)
	proxyRequest.Host = sessionID + ".ipc.test"
	proxyRequest.AddCookie(cookies[0])
	proxyRequest.AddCookie(&http.Cookie{Name: "ipc-login", Value: "vendor-session"})
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
	if strings.Contains(upstreamCookie, gatewayCookieName) || !strings.Contains(upstreamCookie, "ipc-login=vendor-session") {
		t.Fatalf("only the IPC login cookie may be forwarded: %s", upstreamCookie)
	}
	var renewedCookie *http.Cookie
	for _, cookie := range proxyResponse.Result().Cookies() {
		if cookie.Name == gatewayCookieName {
			renewedCookie = cookie
		}
	}
	if renewedCookie == nil || renewedCookie.MaxAge != 36000 ||
		!renewedCookie.Expires.Equal(issuedAt.Add(19*time.Hour)) {
		t.Fatal("activity at hour nine must renew the browser cookie until hour nineteen")
	}
	if proxyResponse.Header().Get("Cache-Control") != "no-store" {
		t.Fatal("proxy responses must reach the gateway again to renew activity")
	}
	setCookie := proxyResponse.Header().Get("Set-Cookie")
	if strings.Contains(strings.ToLower(setCookie), "domain=") {
		t.Fatalf("private IPC cookie domain leaked to browser: %s", setCookie)
	}

	now = issuedAt.Add(10 * time.Hour)
	continuedRequest := httptest.NewRequest(http.MethodGet,
		"https://"+sessionID+".ipc.test/", nil)
	continuedRequest.AddCookie(renewedCookie)
	continuedResponse := httptest.NewRecorder()
	gateway.ServeHTTP(continuedResponse, continuedRequest)
	if continuedResponse.Code != http.StatusOK {
		t.Fatal("renewed session must remain valid past its original expiry")
	}

	now = expiresAt
	expiredRequest := httptest.NewRequest(http.MethodGet,
		"https://"+sessionID+".ipc.test/assets/app.js", nil)
	expiredRequest.AddCookie(cookies[0])
	expiredResponse := httptest.NewRecorder()
	gateway.ServeHTTP(expiredResponse, expiredRequest)
	if expiredResponse.Code != http.StatusUnauthorized || gateway.cachedSessionCount() != 0 {
		t.Fatal("gateway must reject and evict the session after ten hours without requests")
	}
}

func TestGatewayCannotRenewRevokedOrUnavailableSession(t *testing.T) {
	for _, status := range []int{http.StatusUnauthorized, http.StatusServiceUnavailable} {
		t.Run(strconv.Itoa(status), func(t *testing.T) {
			backend := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
				if request.URL.Path != "/vlsTunnel/internal/access-sessions/renew" {
					t.Errorf("unexpected backend path: %s", request.URL.Path)
				}
				http.Error(response, "unavailable", status)
			}))
			defer backend.Close()
			cfg := runtimeConfig{BackendBaseURL: backend.URL, GatewayHostSuffix: ".ipc.test"}
			gateway := newTunnelGateway(cfg, newBackendClient(cfg))
			gateway.sessions["browser-cookie"] = cachedGatewaySession{route: sessionRoute{
				SessionID: "session-1", EndpointID: "1", UpstreamHost: "127.0.0.1",
				UpstreamPort: 61000, ExpiresAt: time.Now().Add(time.Hour),
			}}
			request := httptest.NewRequest(http.MethodGet, "https://session-1.ipc.test/", nil)
			request.AddCookie(&http.Cookie{Name: gatewayCookieName, Value: "browser-cookie"})
			response := httptest.NewRecorder()
			gateway.ServeHTTP(response, request)
			if response.Code != http.StatusUnauthorized || gateway.cachedSessionCount() != 0 || len(response.Result().Cookies()) != 0 {
				t.Fatal("failed renewal must not extend or keep a browser session")
			}
		})
	}
}

func TestGatewayRejectsCookieOnDifferentSessionHostBeforeRenewing(t *testing.T) {
	gateway := newTunnelGateway(runtimeConfig{GatewayHostSuffix: ".ipc.test"}, nil)
	gateway.sessions["cookie-a"] = cachedGatewaySession{route: sessionRoute{
		SessionID: "session-a", EndpointID: "1", UpstreamHost: "127.0.0.1",
		UpstreamPort: 61000, ExpiresAt: time.Now().Add(time.Hour),
	}}
	request := httptest.NewRequest(http.MethodGet, "https://session-b.ipc.test/", nil)
	request.AddCookie(&http.Cookie{Name: gatewayCookieName, Value: "cookie-a"})
	response := httptest.NewRecorder()
	gateway.ServeHTTP(response, request)
	if response.Code != http.StatusUnauthorized {
		t.Fatal("a cookie from another host must not reach the renewal API")
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
