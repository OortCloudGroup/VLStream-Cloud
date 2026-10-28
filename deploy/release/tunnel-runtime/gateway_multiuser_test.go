package main

import (
	"encoding/json"
	"net"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"
)

// Both users may access the same endpoint, but neither URL nor Cookie transfers
// the other user's gateway session. All identities and HTTP peers are fixtures.
func TestGatewayIndependentUsersSameEndpointFailClosed(t *testing.T) {
	const suffix = ".ipc.vls.test"
	const sessionA, sessionB = "user-a-session", "user-b-session"
	const tokenA, tokenB = "fixture-start-a", "fixture-start-b"
	const gatewayToken = "fixture-gateway-012345678901234567890123456789"
	now := time.Now().UTC().Truncate(time.Second)
	expiry := now.Add(time.Hour)
	upstreamCalls := 0
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		upstreamCalls++
		_, _ = w.Write([]byte("fixture-ipc-web"))
	}))
	defer upstream.Close()
	_, rawPort, err := net.SplitHostPort(strings.TrimPrefix(upstream.URL, "http://"))
	if err != nil {
		t.Fatal(err)
	}
	port, _ := strconv.Atoi(rawPort)
	var mu sync.Mutex
	consumed := map[string]bool{}
	revokedA := false
	renewCalls := 0
	backend := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		defer mu.Unlock()
		if r.Header.Get("X-Tunnel-Gateway-Token") != gatewayToken {
			http.Error(w, "fixture gateway auth failed", http.StatusUnauthorized)
			return
		}
		session := ""
		switch r.URL.Path {
		case "/vlsTunnel/internal/access-sessions/resolve":
			token := r.Header.Get("X-Tunnel-Access-Token")
			if token == tokenA {
				session = sessionA
			} else if token == tokenB {
				session = sessionB
			}
			if session == "" || consumed[token] {
				http.Error(w, "fixture token invalid or consumed", http.StatusUnauthorized)
				return
			}
			consumed[token] = true
		case "/vlsTunnel/internal/access-sessions/renew":
			renewCalls++
			session = r.Header.Get("X-Tunnel-Session-Id")
			if (session != sessionA && session != sessionB) || (session == sessionA && revokedA) {
				http.Error(w, "fixture session revoked", http.StatusUnauthorized)
				return
			}
		default:
			http.NotFound(w, r)
			return
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"code": 200, "data": map[string]any{
			"sessionId": session, "endpointId": "same-device-fixture",
			"upstreamHost": "127.0.0.1", "upstreamPort": port,
			"expiresAt": expiry.Format(time.RFC3339),
		}})
	}))
	defer backend.Close()
	cfg := runtimeConfig{BackendBaseURL: backend.URL, GatewayToken: gatewayToken,
		GatewayHostSuffix: suffix, PublicHTTPS: true}
	gateway := newTunnelGateway(cfg, newBackendClient(cfg))
	gateway.now = func() time.Time { return now }
	request := func(session, path string, cookie *http.Cookie) *httptest.ResponseRecorder {
		r := httptest.NewRequest(http.MethodGet, "https://"+session+suffix+path, nil)
		if cookie != nil {
			r.AddCookie(cookie)
		}
		w := httptest.NewRecorder()
		gateway.ServeHTTP(w, r)
		return w
	}
	for _, session := range []string{sessionA, sessionB} {
		if got := request(session, "/", nil).Code; got != http.StatusUnauthorized {
			t.Fatalf("copied final URL must require its own browser Cookie: %d", got)
		}
	}
	bootA := request(sessionA, "/s/"+tokenA, nil)
	bootB := request(sessionB, "/s/"+tokenB, nil)
	if bootA.Code != http.StatusSeeOther || bootB.Code != http.StatusSeeOther {
		t.Fatalf("separate authorized bootstraps failed: %d/%d", bootA.Code, bootB.Code)
	}
	cookieA, cookieB := bootA.Result().Cookies()[0], bootB.Result().Cookies()[0]
	if cookieA.Value == cookieB.Value || cookieA.Domain != "" || cookieB.Domain != "" ||
		!cookieA.Secure || !cookieA.HttpOnly || cookieA.Path != "/" {
		t.Fatal("users require independent Secure/HttpOnly host-only Cookies")
	}
	if got := request(sessionA, "/s/"+tokenA, nil).Code; got != http.StatusUnauthorized {
		t.Fatalf("consumed startup URL must not be reusable: %d", got)
	}
	if got := request(sessionB, "/", cookieA).Code; got != http.StatusUnauthorized {
		t.Fatalf("cross-host Cookie must be rejected: %d", got)
	}
	if renewCalls != 0 || upstreamCalls != 0 {
		t.Fatal("unauthorized requests must not reach backend renewal or IPC")
	}
	if request(sessionA, "/", cookieA).Code != http.StatusOK ||
		request(sessionB, "/", cookieB).Code != http.StatusOK {
		t.Fatal("each user's own session must reach the same endpoint independently")
	}
	mu.Lock()
	revokedA = true
	mu.Unlock()
	if got := request(sessionA, "/", cookieA).Code; got != http.StatusUnauthorized {
		t.Fatalf("revoked user A session must fail closed: %d", got)
	}
	if got := request(sessionB, "/", cookieB).Code; got != http.StatusOK {
		t.Fatalf("revoking A must not close B's independent session: %d", got)
	}
	now = expiry.Add(time.Second)
	if got := request(sessionB, "/", cookieB).Code; got != http.StatusUnauthorized {
		t.Fatalf("expired B session must fail closed: %d", got)
	}
}
