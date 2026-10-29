package main

import (
	"bufio"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"
)

type upgradeFixture struct {
	t            *testing.T
	mu           sync.Mutex
	routes       map[string]sessionRoute
	denied       map[string]int
	hang         bool
	validations  int
	renewals     int
	blockEntered chan struct{}
	blockRelease chan struct{}
	gateway      *tunnelGateway
	server       *httptest.Server
	upstream     *httptest.Server
	backend      *httptest.Server
}

func newUpgradeFixture(t *testing.T, ttl time.Duration) *upgradeFixture {
	f := &upgradeFixture{t: t, routes: make(map[string]sessionRoute), denied: make(map[string]int)}
	f.upstream = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if !isWebSocketUpgrade(r) {
			_, _ = w.Write([]byte("http-ok"))
			return
		}
		conn, rw, err := w.(http.Hijacker).Hijack()
		if err != nil {
			return
		}
		defer conn.Close()
		sum := sha1.Sum([]byte(r.Header.Get("Sec-WebSocket-Key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
		_, _ = fmt.Fprintf(rw, "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: %s\r\n\r\n", base64.StdEncoding.EncodeToString(sum[:]))
		_ = rw.Flush()
		for {
			header := make([]byte, 2)
			if _, err = io.ReadFull(rw, header); err != nil {
				return
			}
			length := int(header[1] & 127)
			if length > 125 || header[1]&128 == 0 {
				return
			}
			mask := make([]byte, 4)
			if _, err = io.ReadFull(rw, mask); err != nil {
				return
			}
			body := make([]byte, length)
			if _, err = io.ReadFull(rw, body); err != nil {
				return
			}
			for i := range body {
				body[i] ^= mask[i%4]
			}
			if _, err = conn.Write(append([]byte{0x81, byte(length)}, body...)); err != nil {
				return
			}
		}
	}))
	_, rawPort, _ := net.SplitHostPort(strings.TrimPrefix(f.upstream.URL, "http://"))
	port, _ := strconv.Atoi(rawPort)
	for _, id := range []string{"a", "b"} {
		f.routes[id] = sessionRoute{SessionID: id, EndpointID: "same-fixture-device", UpstreamHost: "127.0.0.1", UpstreamPort: port, ExpiresAt: time.Now().Add(ttl)}
	}
	f.backend = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("X-Tunnel-Gateway-Token") != "fixture-gateway-token" {
			http.Error(w, "denied", 401)
			return
		}
		id := r.Header.Get("X-Tunnel-Session-Id")
		f.mu.Lock()
		route, found := f.routes[id]
		status := f.denied[id]
		validate := r.URL.Path == "/vlsTunnel/internal/access-sessions/validate"
		if validate {
			f.validations++
		} else if r.URL.Path == "/vlsTunnel/internal/access-sessions/renew" {
			f.renewals++
		} else {
			f.mu.Unlock()
			http.NotFound(w, r)
			return
		}
		hang := validate && f.hang
		entered, release := f.blockEntered, f.blockRelease
		if validate && entered != nil {
			f.blockEntered = nil
		} else {
			entered = nil
		}
		f.mu.Unlock()
		if hang {
			<-r.Context().Done()
			return
		}
		if entered != nil {
			close(entered)
			select {
			case <-release:
			case <-r.Context().Done():
				return
			}
		}
		if !found || status != 0 {
			if status == 0 {
				status = 401
			}
			http.Error(w, "fixture denied", status)
			return
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"code": 200, "data": map[string]any{"sessionId": id, "endpointId": route.EndpointID, "upstreamHost": route.UpstreamHost, "upstreamPort": route.UpstreamPort, "expiresAt": route.ExpiresAt.UTC().Format(time.RFC3339Nano)}})
	}))
	f.gateway = newTunnelGateway(runtimeConfig{BackendBaseURL: f.backend.URL, GatewayToken: "fixture-gateway-token", GatewayHostSuffix: ".ipc.vls.test", PublicHTTPS: true}, newBackendClient(runtimeConfig{BackendBaseURL: f.backend.URL, GatewayToken: "fixture-gateway-token"}))
	f.gateway.upgradeRecheckInterval = 20 * time.Millisecond
	f.gateway.upgradeValidationTimeout = 60 * time.Millisecond
	for id, route := range f.routes {
		f.gateway.sessions["cookie-"+id] = cachedGatewaySession{route: route}
	}
	f.server = httptest.NewServer(f.gateway)
	t.Cleanup(func() { f.gateway.closeAllUpgrades(); f.server.Close(); f.backend.Close(); f.upstream.Close() })
	return f
}

type upgradePeer struct {
	net.Conn
	reader *bufio.Reader
}

func (f *upgradeFixture) open(id string, cookie bool) (*upgradePeer, int) {
	conn, err := net.DialTimeout("tcp", strings.TrimPrefix(f.server.URL, "http://"), time.Second)
	if err != nil {
		f.t.Fatal(err)
	}
	head := "GET /ws HTTP/1.1\r\nHost: " + id + ".ipc.vls.test\r\nConnection: Upgrade\r\nUpgrade: websocket\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
	if cookie {
		head += "Cookie: " + gatewayCookieName + "=cookie-" + id + "\r\n"
	}
	_, _ = io.WriteString(conn, head+"\r\n")
	reader := bufio.NewReader(conn)
	_ = conn.SetReadDeadline(time.Now().Add(time.Second))
	response, err := http.ReadResponse(reader, nil)
	if err != nil {
		conn.Close()
		f.t.Fatal(err)
	}
	if response.StatusCode != 101 {
		conn.Close()
		return nil, response.StatusCode
	}
	_ = conn.SetReadDeadline(time.Time{})
	f.t.Cleanup(func() { conn.Close() })
	return &upgradePeer{Conn: conn, reader: reader}, 101
}

func (p *upgradePeer) echo(t *testing.T, text string) {
	t.Helper()
	_ = p.SetDeadline(time.Now().Add(time.Second))
	mask := []byte{1, 2, 3, 4}
	data := []byte(text)
	frame := []byte{0x81, 0x80 | byte(len(data))}
	frame = append(frame, mask...)
	for i, b := range data {
		frame = append(frame, b^mask[i%4])
	}
	if _, err := p.Write(frame); err != nil {
		t.Fatal(err)
	}
	header := make([]byte, 2)
	if _, err := io.ReadFull(p.reader, header); err != nil {
		t.Fatal(err)
	}
	body := make([]byte, int(header[1]&127))
	if _, err := io.ReadFull(p.reader, body); err != nil {
		t.Fatal(err)
	}
	if string(body) != text {
		t.Fatalf("echo mismatch: %q", body)
	}
	_ = p.SetDeadline(time.Time{})
}

func (p *upgradePeer) expectClosed(t *testing.T) {
	t.Helper()
	_ = p.SetReadDeadline(time.Now().Add(time.Second))
	_, err := p.reader.ReadByte()
	if err == nil {
		t.Fatal("upgraded connection is still readable")
	}
	if timeout, ok := err.(net.Error); ok && timeout.Timeout() {
		t.Fatal("upgraded connection did not close within bound")
	}
}

func TestUpgradeRejectsBefore101WithoutCookieOrLiveSession(t *testing.T) {
	f := newUpgradeFixture(t, time.Second)
	if _, status := f.open("a", false); status != 401 {
		t.Fatalf("anonymous upgrade: %d", status)
	}
	f.mu.Lock()
	f.denied["a"] = 401
	f.mu.Unlock()
	if _, status := f.open("a", true); status == 101 {
		t.Fatal("revoked session upgraded")
	}
}

func TestUpgradeClosesOnRevocationOfflineRouteChangeAndValidationFailure(t *testing.T) {
	for _, state := range []string{"revoked", "offline", "route-change", "unavailable", "timeout"} {
		t.Run(state, func(t *testing.T) {
			f := newUpgradeFixture(t, 2*time.Second)
			peer, status := f.open("a", true)
			if status != 101 {
				t.Fatal(status)
			}
			peer.echo(t, "before")
			f.mu.Lock()
			switch state {
			case "revoked":
				f.denied["a"] = 401
			case "offline":
				f.denied["a"] = 503
			case "unavailable":
				f.denied["a"] = 404
			case "timeout":
				f.hang = true
			case "route-change":
				r := f.routes["a"]
				r.UpstreamPort = 1
				f.routes["a"] = r
			}
			f.mu.Unlock()
			peer.expectClosed(t)
		})
	}
}

func TestUpgradeClosesAtExpiryWithoutExtendingIdleTtl(t *testing.T) {
	f := newUpgradeFixture(t, 220*time.Millisecond)
	peer, status := f.open("a", true)
	if status != 101 {
		t.Fatal(status)
	}
	peer.echo(t, "alive")
	peer.expectClosed(t)
	f.mu.Lock()
	checks, renewals := f.validations, f.renewals
	f.mu.Unlock()
	if checks < 2 || renewals != 1 {
		t.Fatalf("WS must recheck without renewing: validate=%d renew=%d", checks, renewals)
	}
}

func TestUpgradeIndependentSocketsAndUsers(t *testing.T) {
	f := newUpgradeFixture(t, 2*time.Second)
	a1, _ := f.open("a", true)
	a2, _ := f.open("a", true)
	b, _ := f.open("b", true)
	a1.Close()
	time.Sleep(70 * time.Millisecond)
	a2.echo(t, "a-still-valid")
	b.echo(t, "b-valid")
	f.mu.Lock()
	f.denied["a"] = 401
	f.mu.Unlock()
	a2.expectClosed(t)
	b.echo(t, "b-independent")
}

func TestConcurrentHttpRenewalSurvivesStaleUpgradeValidation(t *testing.T) {
	f := newUpgradeFixture(t, 350*time.Millisecond)
	peer, status := f.open("a", true)
	if status != 101 {
		t.Fatal(status)
	}
	entered, release := make(chan struct{}), make(chan struct{})
	f.mu.Lock()
	f.blockEntered = entered
	f.blockRelease = release
	f.mu.Unlock()
	select {
	case <-entered:
	case <-time.After(time.Second):
		t.Fatal("validation not entered")
	}
	f.mu.Lock()
	r := f.routes["a"]
	r.ExpiresAt = time.Now().Add(2 * time.Second)
	f.routes["a"] = r
	f.mu.Unlock()
	request, _ := http.NewRequest(http.MethodGet, f.server.URL+"/keepalive", nil)
	request.Host = "a.ipc.vls.test"
	request.AddCookie(&http.Cookie{Name: gatewayCookieName, Value: "cookie-a"})
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		close(release)
		t.Fatal(err)
	}
	response.Body.Close()
	close(release)
	if response.StatusCode != 200 {
		t.Fatal(response.StatusCode)
	}
	time.Sleep(400 * time.Millisecond)
	peer.echo(t, "renewed-without-interruption")
}

func TestValidationRejectsUpgradeAfterUpstreamOffers101(t *testing.T) {
	f := newUpgradeFixture(t, time.Second)
	f.mu.Lock()
	f.hang = true
	f.mu.Unlock()
	if _, status := f.open("a", true); status == 101 {
		t.Fatal("101 sent without live read-only validation")
	}
}

func TestConcurrentRenewalOutlivesInflightOldExpiryDeadline(t *testing.T) {
	f := newUpgradeFixture(t, 220*time.Millisecond)
	f.gateway.upgradeValidationTimeout = 500 * time.Millisecond
	peer, status := f.open("a", true)
	if status != 101 {
		t.Fatal(status)
	}
	entered, release := make(chan struct{}), make(chan struct{})
	f.mu.Lock()
	f.blockEntered = entered
	f.blockRelease = release
	f.mu.Unlock()
	select {
	case <-entered:
	case <-time.After(time.Second):
		t.Fatal("validation not entered")
	}
	f.mu.Lock()
	route := f.routes["a"]
	route.ExpiresAt = time.Now().Add(2 * time.Second)
	f.routes["a"] = route
	f.mu.Unlock()
	request, _ := http.NewRequest(http.MethodGet, f.server.URL+"/activity", nil)
	request.Host = "a.ipc.vls.test"
	request.AddCookie(&http.Cookie{Name: gatewayCookieName, Value: "cookie-a"})
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		close(release)
		t.Fatal(err)
	}
	response.Body.Close()
	time.Sleep(260 * time.Millisecond) // The pending read used the old, now-expired deadline.
	close(release)
	peer.echo(t, "actual-http-activity-preserves-live-socket")
}

func TestGatewayShutdownClosesAllUpgradedUsers(t *testing.T) {
	f := newUpgradeFixture(t, time.Second)
	a, _ := f.open("a", true)
	b, _ := f.open("b", true)
	f.gateway.closeAllUpgrades()
	a.expectClosed(t)
	b.expectClosed(t)
}

func TestUpgradeGuardCannotBeBypassedWithAnother101Protocol(t *testing.T) {
	r := httptest.NewRequest(http.MethodGet, "https://a.ipc.vls.test/", nil)
	r.Header.Set("Connection", "keep-alive, UpGrAdE")
	r.Header.Set("Upgrade", "other-protocol")
	if !isProtocolUpgrade(r) {
		t.Fatal("all 101 protocols need the same expiry guard")
	}
	r.Header.Del("Upgrade")
	if isProtocolUpgrade(r) {
		t.Fatal("ordinary HTTP must not create a guard")
	}
}
