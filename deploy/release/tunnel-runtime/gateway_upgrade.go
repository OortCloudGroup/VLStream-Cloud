package main

import (
	"context"
	"errors"
	"net/http"
	"strings"
	"time"
)

func isWebSocketUpgrade(request *http.Request) bool {
	if !strings.EqualFold(request.Header.Get("Upgrade"), "websocket") {
		return false
	}
	return isProtocolUpgrade(request)
}

func isProtocolUpgrade(request *http.Request) bool {
	if strings.TrimSpace(request.Header.Get("Upgrade")) == "" {
		return false
	}
	for _, value := range request.Header.Values("Connection") {
		for _, token := range strings.Split(value, ",") {
			if strings.EqualFold(strings.TrimSpace(token), "upgrade") {
				return true
			}
		}
	}
	return false
}

func sameSessionRoute(a, b sessionRoute) bool {
	return a.SessionID == b.SessionID && a.EndpointID == b.EndpointID &&
		a.UpstreamHost == b.UpstreamHost && a.UpstreamPort == b.UpstreamPort
}

func (gateway *tunnelGateway) upgradeSnapshot(cookie string, expected sessionRoute) (sessionRoute, bool) {
	gateway.mu.RLock()
	defer gateway.mu.RUnlock()
	cached, found := gateway.sessions[cookie]
	return cached.route, found && sameSessionRoute(cached.route, expected) &&
		cached.route.ExpiresAt.After(gateway.now())
}

// This endpoint never renews the backend TTL. Only ordinary authenticated HTTP
// activity (including the initial Upgrade) may extend the idle deadline.
func (gateway *tunnelGateway) validateUpgrade(ctx context.Context, cookie string, expected sessionRoute) error {
	snapshot, valid := gateway.upgradeSnapshot(cookie, expected)
	if !valid {
		return errors.New("upgraded session is no longer cached or live")
	}
	timeout := gateway.upgradeValidationTimeout
	if timeout <= 0 || timeout > 3*time.Second {
		timeout = 3 * time.Second
	}
	budgetDeadline := time.Now().Add(timeout)
	var route sessionRoute
	for attempt := 0; ; attempt++ {
		remaining := time.Until(budgetDeadline)
		if untilExpiry := snapshot.ExpiresAt.Sub(gateway.now()); untilExpiry < remaining {
			remaining = untilExpiry
		}
		checkContext, cancel := context.WithTimeout(ctx, remaining)
		var err error
		route, err = gateway.backend.validateSession(checkContext, expected.SessionID)
		cancel()
		if err == nil {
			break
		}
		newer, live := gateway.upgradeSnapshot(cookie, expected)
		// A concurrent authenticated HTTP renewal may outlive the deadline of
		// an already in-flight validation. Retry once inside the SAME budget;
		// never retry a revocation/auth/availability failure or extend the TTL.
		if attempt == 0 && ctx.Err() == nil && live && time.Until(budgetDeadline) > 0 &&
			newer.ExpiresAt.After(snapshot.ExpiresAt) &&
			(errors.Is(err, context.DeadlineExceeded) || errors.Is(err, errSessionRouteExpired)) {
			snapshot = newer
			continue
		}
		return err
	}
	if !sameSessionRoute(route, expected) || !route.ExpiresAt.After(gateway.now()) {
		return errors.New("upgraded session route or expiry changed")
	}
	gateway.mu.Lock()
	defer gateway.mu.Unlock()
	current, found := gateway.sessions[cookie]
	if !found || !sameSessionRoute(current.route, expected) || !current.route.ExpiresAt.After(gateway.now()) {
		return errors.New("upgraded session was invalidated during validation")
	}
	// Do not undo a concurrent HTTP renewal with an older validation response.
	// A newer server deadline is authoritative but is never created by this read.
	if !current.route.ExpiresAt.After(snapshot.ExpiresAt) {
		current.route.ExpiresAt = route.ExpiresAt
		gateway.sessions[cookie] = current
	}
	return nil
}

func (gateway *tunnelGateway) startUpgradeGuard(ctx context.Context, cancel context.CancelFunc,
	cookie string, route sessionRoute) (uint64, error) {
	// Recheck after upstream has offered 101, before it reaches the browser.
	checkStarted := time.Now()
	if err := gateway.validateUpgrade(ctx, cookie, route); err != nil {
		return 0, err
	}
	gateway.mu.Lock()
	cached, found := gateway.sessions[cookie]
	if !found || !sameSessionRoute(cached.route, route) || !cached.route.ExpiresAt.After(gateway.now()) {
		gateway.mu.Unlock()
		return 0, errors.New("session invalidated before upgrade")
	}
	gateway.upgradeSequence++
	id := gateway.upgradeSequence
	if gateway.upgrades[cookie] == nil {
		gateway.upgrades[cookie] = make(map[uint64]context.CancelFunc)
	}
	gateway.upgrades[cookie][id] = cancel
	gateway.mu.Unlock()
	go gateway.watchUpgrade(ctx, cancel, cookie, route, checkStarted)
	return id, nil
}

func (gateway *tunnelGateway) watchUpgrade(ctx context.Context, cancel context.CancelFunc,
	cookie string, route sessionRoute, lastCheckStarted time.Time) {
	defer cancel()
	interval := gateway.upgradeRecheckInterval
	if interval <= 0 || interval > 5*time.Second {
		interval = 5 * time.Second
	}
	for {
		current, valid := gateway.upgradeSnapshot(cookie, route)
		if !valid {
			gateway.removeSession(cookie)
			return
		}
		// Start-to-start cadence: backend latency must not add another full
		// interval to the revocation bound (default 5s + at most 3s request).
		wait := time.Until(lastCheckStarted.Add(interval))
		if wait < 0 {
			wait = 0
		}
		if remaining := current.ExpiresAt.Sub(gateway.now()); remaining < wait {
			wait = remaining
		}
		timer := time.NewTimer(wait)
		select {
		case <-ctx.Done():
			timer.Stop()
			return
		case <-timer.C:
		}
		lastCheckStarted = time.Now()
		if err := gateway.validateUpgrade(ctx, cookie, route); err != nil {
			if ctx.Err() != nil {
				return // Normal client close must not invalidate its other sockets.
			}
			// Cancellation closes the reverse proxy's upgraded backend connection,
			// which closes the hijacked client too (stdlib ReverseProxy contract).
			gateway.removeSession(cookie)
			return
		}
	}
}

func (gateway *tunnelGateway) unregisterUpgrade(cookie string, id uint64) {
	if id == 0 {
		return
	}
	gateway.mu.Lock()
	defer gateway.mu.Unlock()
	delete(gateway.upgrades[cookie], id)
	if len(gateway.upgrades[cookie]) == 0 {
		delete(gateway.upgrades, cookie)
	}
}

func (gateway *tunnelGateway) closeAllUpgrades() {
	gateway.mu.Lock()
	defer gateway.mu.Unlock()
	for cookie := range gateway.upgrades {
		gateway.removeSessionLocked(cookie)
	}
}
