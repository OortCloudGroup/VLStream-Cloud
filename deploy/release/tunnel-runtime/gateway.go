package main

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"errors"
	"fmt"
	"log"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"
)

const gatewayCookieName = "__Host-vls_ipc_session"
const maxCachedGatewaySessions = 4096

type headerStrippingTransport struct {
	base http.RoundTripper
}

func (transport headerStrippingTransport) RoundTrip(request *http.Request) (*http.Response, error) {
	request.Header.Del("Forwarded")
	request.Header.Del("X-Forwarded-For")
	request.Header.Del("X-Real-IP")
	request.Header.Del("X-Tunnel-Control-Token")
	request.Header.Del("X-Tunnel-Gateway-Token")
	return transport.base.RoundTrip(request)
}

type cachedGatewaySession struct {
	route sessionRoute
}

type tunnelGateway struct {
	cfg     runtimeConfig
	backend *backendClient
	now     func() time.Time

	mu       sync.RWMutex
	sessions map[string]cachedGatewaySession
}

func newTunnelGateway(cfg runtimeConfig, backend *backendClient) *tunnelGateway {
	return &tunnelGateway{
		cfg:      cfg,
		backend:  backend,
		now:      time.Now,
		sessions: make(map[string]cachedGatewaySession),
	}
}

func (gateway *tunnelGateway) ServeHTTP(response http.ResponseWriter, request *http.Request) {
	response.Header().Set("Referrer-Policy", "no-referrer")
	response.Header().Set("X-Content-Type-Options", "nosniff")
	if request.URL.Path == "/__health" {
		if !isLoopbackHealthHost(request.Host) {
			http.NotFound(response, request)
			return
		}
		response.Header().Set("Cache-Control", "no-store")
		response.WriteHeader(http.StatusOK)
		_, _ = response.Write([]byte("ok"))
		return
	}
	sessionID, err := gateway.sessionIDFromHost(request.Host)
	if err != nil {
		http.Error(response, "invalid remote-management host", http.StatusBadRequest)
		return
	}
	if strings.HasPrefix(request.URL.Path, "/s/") {
		gateway.bootstrap(response, request, sessionID)
		return
	}
	gateway.proxy(response, request, sessionID)
}

func (gateway *tunnelGateway) bootstrap(response http.ResponseWriter, request *http.Request,
	sessionID string) {
	if request.Method != http.MethodGet {
		http.Error(response, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	token := strings.TrimPrefix(request.URL.Path, "/s/")
	if token == "" || strings.Contains(token, "/") || len(token) > 256 {
		http.Error(response, "invalid remote-management token", http.StatusBadRequest)
		return
	}
	ctx, cancel := context.WithTimeout(request.Context(), 10*time.Second)
	route, err := gateway.backend.resolveSession(ctx, token)
	cancel()
	if err != nil {
		log.Printf("remote-management bootstrap rejected for session %s: %v", sessionID, err)
		http.Error(response, "remote-management session is unavailable", http.StatusUnauthorized)
		return
	}
	if route.SessionID != sessionID {
		http.Error(response, "remote-management host does not match the session", http.StatusForbidden)
		return
	}
	cookieToken, err := randomURLToken(32)
	if err != nil {
		http.Error(response, "cannot create browser session", http.StatusInternalServerError)
		return
	}
	gateway.mu.Lock()
	gateway.removeExpiredLocked()
	if len(gateway.sessions) >= maxCachedGatewaySessions {
		gateway.mu.Unlock()
		http.Error(response, "remote-management gateway is busy", http.StatusServiceUnavailable)
		return
	}
	gateway.sessions[cookieToken] = cachedGatewaySession{route: route}
	gateway.mu.Unlock()

	http.SetCookie(response, &http.Cookie{
		Name:     gatewayCookieName,
		Value:    cookieToken,
		Path:     "/",
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
		Expires:  route.ExpiresAt,
		MaxAge:   maxAgeSeconds(gateway.now(), route.ExpiresAt),
	})
	response.Header().Set("Cache-Control", "no-store")
	response.Header().Set("Pragma", "no-cache")
	http.Redirect(response, request, "/", http.StatusSeeOther)
}

func (gateway *tunnelGateway) proxy(response http.ResponseWriter, request *http.Request,
	sessionID string) {
	cookie, err := request.Cookie(gatewayCookieName)
	if err != nil || cookie.Value == "" {
		http.Error(response, "remote-management browser session is required", http.StatusUnauthorized)
		return
	}
	gateway.mu.RLock()
	cached, found := gateway.sessions[cookie.Value]
	gateway.mu.RUnlock()
	if !found || cached.route.SessionID != sessionID || !cached.route.ExpiresAt.After(gateway.now()) {
		gateway.removeSession(cookie.Value)
		http.Error(response, "remote-management browser session expired", http.StatusUnauthorized)
		return
	}
	if err := validateSessionRoute(cached.route); err != nil {
		gateway.removeSession(cookie.Value)
		http.Error(response, "remote-management upstream is invalid", http.StatusBadGateway)
		return
	}

	target := &url.URL{
		Scheme: "http",
		Host: net.JoinHostPort(netIP(cached.route.UpstreamHost),
			strconv.Itoa(cached.route.UpstreamPort)),
	}
	proxy := httputil.NewSingleHostReverseProxy(target)
	proxy.Transport = headerStrippingTransport{base: http.DefaultTransport}
	originalDirector := proxy.Director
	originalHost := request.Host
	proxy.Director = func(upstreamRequest *http.Request) {
		originalDirector(upstreamRequest)
		upstreamRequest.Host = originalHost
		upstreamRequest.Header.Set("X-Forwarded-Host", originalHost)
		upstreamRequest.Header.Set("X-Forwarded-Proto", publicScheme(gateway.cfg))
		upstreamRequest.Header.Del("Forwarded")
		upstreamRequest.Header.Del("X-Forwarded-For")
		upstreamRequest.Header.Del("X-Tunnel-Control-Token")
		upstreamRequest.Header.Del("X-Tunnel-Gateway-Token")
	}
	proxy.ErrorHandler = func(writer http.ResponseWriter, _ *http.Request, proxyError error) {
		log.Printf("IPC management upstream failed for endpoint %s: %v",
			cached.route.EndpointID, proxyError)
		http.Error(writer, "IPC management backend is unavailable", http.StatusBadGateway)
	}
	proxy.ModifyResponse = func(upstreamResponse *http.Response) error {
		rewriteLocation(upstreamResponse, originalHost, publicScheme(gateway.cfg), target.Host)
		rewriteCookies(upstreamResponse, true)
		upstreamResponse.Header.Set("Referrer-Policy", "no-referrer")
		upstreamResponse.Header.Set("X-Content-Type-Options", "nosniff")
		return nil
	}
	proxy.ServeHTTP(response, request)
}

func (gateway *tunnelGateway) sessionIDFromHost(rawHost string) (string, error) {
	host := strings.ToLower(strings.TrimSpace(rawHost))
	if parsedHost, _, err := net.SplitHostPort(host); err == nil {
		host = parsedHost
	}
	if !strings.HasSuffix(host, gateway.cfg.GatewayHostSuffix) {
		return "", errors.New("host suffix does not match")
	}
	sessionID := strings.TrimSuffix(host, gateway.cfg.GatewayHostSuffix)
	if sessionID == "" || strings.Contains(sessionID, ".") || len(sessionID) > 64 {
		return "", errors.New("session label is invalid")
	}
	for _, char := range sessionID {
		if (char < 'a' || char > 'z') && (char < '0' || char > '9') && char != '-' {
			return "", errors.New("session label contains invalid characters")
		}
	}
	return sessionID, nil
}

func (gateway *tunnelGateway) removeSession(cookieToken string) {
	gateway.mu.Lock()
	delete(gateway.sessions, cookieToken)
	gateway.mu.Unlock()
}

func (gateway *tunnelGateway) removeExpiredLocked() {
	now := gateway.now()
	for token, session := range gateway.sessions {
		if !session.route.ExpiresAt.After(now) {
			delete(gateway.sessions, token)
		}
	}
}

func randomURLToken(bytesCount int) (string, error) {
	value := make([]byte, bytesCount)
	if _, err := rand.Read(value); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(value), nil
}

func maxAgeSeconds(now, expiresAt time.Time) int {
	seconds := int(expiresAt.Sub(now).Seconds())
	if seconds < 1 {
		return 1
	}
	return seconds
}

func publicScheme(cfg runtimeConfig) string {
	if cfg.PublicHTTPS {
		return "https"
	}
	return "http"
}

func isLoopbackHealthHost(rawHost string) bool {
	host := strings.TrimSpace(rawHost)
	if parsedHost, _, err := net.SplitHostPort(host); err == nil {
		host = parsedHost
	}
	return host == "127.0.0.1" || host == "::1" || strings.EqualFold(host, "localhost")
}

func rewriteLocation(response *http.Response, publicHost, scheme, upstreamHost string) {
	location := response.Header.Get("Location")
	if location == "" {
		return
	}
	parsed, err := url.Parse(location)
	if err != nil || !parsed.IsAbs() || !shouldRewriteLocationHost(parsed.Host, upstreamHost) {
		return
	}
	parsed.Scheme = scheme
	parsed.Host = publicHost
	response.Header.Set("Location", parsed.String())
}

func shouldRewriteLocationHost(locationHost, upstreamHost string) bool {
	host := locationHost
	if parsedHost, _, err := net.SplitHostPort(locationHost); err == nil {
		host = parsedHost
	}
	if strings.EqualFold(locationHost, upstreamHost) || strings.EqualFold(host, "localhost") {
		return true
	}
	ip := net.ParseIP(host)
	return ip != nil && (ip.IsLoopback() || ip.IsPrivate() || ip.IsLinkLocalUnicast())
}

func rewriteCookies(response *http.Response, secure bool) {
	cookies := response.Cookies()
	if len(cookies) == 0 {
		return
	}
	response.Header.Del("Set-Cookie")
	for _, cookie := range cookies {
		cookie.Domain = ""
		if cookie.Path == "" {
			cookie.Path = "/"
		}
		if secure {
			cookie.Secure = true
		}
		response.Header.Add("Set-Cookie", cookie.String())
	}
}

func (gateway *tunnelGateway) cachedSessionCount() int {
	gateway.mu.RLock()
	defer gateway.mu.RUnlock()
	return len(gateway.sessions)
}

func (gateway *tunnelGateway) String() string {
	return fmt.Sprintf("gateway(listen=%s, suffix=%s)",
		gateway.cfg.GatewayListenAddress, gateway.cfg.GatewayHostSuffix)
}
