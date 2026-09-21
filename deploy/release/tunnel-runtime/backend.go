package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type apiResponse[T any] struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
	Data T      `json:"data"`
}

type desiredRoute struct {
	EndpointID       string `json:"endpointId"`
	ServiceName      string `json:"serviceName"`
	ServiceToken     string `json:"serviceToken"`
	BindAddress      string `json:"bindAddr"`
	ConfigGeneration int64  `json:"configGeneration"`
	DesiredState     string `json:"desiredState"`
}

type sessionRoute struct {
	SessionID    string
	EndpointID   string
	UpstreamHost string
	UpstreamPort int
	ExpiresAt    time.Time
}

type backendClient struct {
	baseURL      string
	controlToken string
	gatewayToken string
	client       *http.Client
}

func newBackendClient(cfg runtimeConfig) *backendClient {
	return &backendClient{
		baseURL:      cfg.BackendBaseURL,
		controlToken: cfg.ControlToken,
		gatewayToken: cfg.GatewayToken,
		client: &http.Client{
			Timeout: 10 * time.Second,
			CheckRedirect: func(_ *http.Request, _ []*http.Request) error {
				return http.ErrUseLastResponse
			},
		},
	}
}

func (client *backendClient) fetchRoutes(ctx context.Context) ([]desiredRoute, error) {
	var response apiResponse[[]desiredRoute]
	if err := client.doJSON(ctx, http.MethodGet, "/vlsTunnel/internal/routes",
		map[string]string{"X-Tunnel-Control-Token": client.controlToken}, nil, &response); err != nil {
		return nil, err
	}
	if response.Code != http.StatusOK {
		return nil, fmt.Errorf("control plane rejected route query: code=%d", response.Code)
	}
	return response.Data, nil
}

func (client *backendClient) reportRouteStatus(ctx context.Context, route desiredRoute,
	status, message string) error {
	request := map[string]any{
		"configGeneration": route.ConfigGeneration,
		"routeStatus":      status,
	}
	if message != "" {
		request["errorMessage"] = message
	}
	var response apiResponse[json.RawMessage]
	path := "/vlsTunnel/internal/routes/" + route.EndpointID + "/status"
	if err := client.doJSON(ctx, http.MethodPost, path,
		map[string]string{"X-Tunnel-Control-Token": client.controlToken}, request, &response); err != nil {
		return err
	}
	if response.Code != http.StatusOK {
		return fmt.Errorf("control plane rejected route status: code=%d", response.Code)
	}
	return nil
}

func (client *backendClient) resolveSession(ctx context.Context, token string) (sessionRoute, error) {
	var response struct {
		Code int    `json:"code"`
		Msg  string `json:"msg"`
		Data struct {
			SessionID    string          `json:"sessionId"`
			EndpointID   string          `json:"endpointId"`
			UpstreamHost string          `json:"upstreamHost"`
			UpstreamPort int             `json:"upstreamPort"`
			ExpiresAt    json.RawMessage `json:"expiresAt"`
		} `json:"data"`
	}
	if err := client.doJSON(ctx, http.MethodPost, "/vlsTunnel/internal/access-sessions/resolve",
		map[string]string{
			"X-Tunnel-Gateway-Token": client.gatewayToken,
			"X-Tunnel-Access-Token":  token,
		}, nil, &response); err != nil {
		return sessionRoute{}, err
	}
	if response.Code != http.StatusOK {
		return sessionRoute{}, fmt.Errorf("access session was rejected: code=%d", response.Code)
	}
	expiresAt, err := parseAPITime(response.Data.ExpiresAt)
	if err != nil {
		return sessionRoute{}, fmt.Errorf("invalid access-session expiry: %w", err)
	}
	route := sessionRoute{
		SessionID:    response.Data.SessionID,
		EndpointID:   response.Data.EndpointID,
		UpstreamHost: response.Data.UpstreamHost,
		UpstreamPort: response.Data.UpstreamPort,
		ExpiresAt:    expiresAt,
	}
	if err := validateSessionRoute(route); err != nil {
		return sessionRoute{}, err
	}
	return route, nil
}

func (client *backendClient) doJSON(ctx context.Context, method, path string,
	headers map[string]string, requestBody any, responseBody any) error {
	var body io.Reader
	if requestBody != nil {
		encoded, err := json.Marshal(requestBody)
		if err != nil {
			return err
		}
		body = bytes.NewReader(encoded)
	}
	request, err := http.NewRequestWithContext(ctx, method, client.baseURL+path, body)
	if err != nil {
		return err
	}
	request.Header.Set("Accept", "application/json")
	for name, value := range headers {
		request.Header.Set(name, value)
	}
	if requestBody != nil {
		request.Header.Set("Content-Type", "application/json")
	}
	response, err := client.client.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	limited := io.LimitReader(response.Body, 1024*1024)
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		_, _ = io.Copy(io.Discard, limited)
		return fmt.Errorf("backend returned HTTP %d", response.StatusCode)
	}
	if err := json.NewDecoder(limited).Decode(responseBody); err != nil {
		return fmt.Errorf("cannot decode backend response: %w", err)
	}
	return nil
}

func validateSessionRoute(route sessionRoute) error {
	if route.SessionID == "" || route.EndpointID == "" {
		return errors.New("backend route is missing session or endpoint identity")
	}
	host := netIP(strings.TrimSpace(route.UpstreamHost))
	if host != "127.0.0.1" && host != "::1" {
		return errors.New("backend route does not target a loopback address")
	}
	if route.UpstreamPort < 1 || route.UpstreamPort > 65535 {
		return errors.New("backend route port is outside 1-65535")
	}
	if !route.ExpiresAt.After(time.Now()) {
		return errors.New("backend route has already expired")
	}
	return nil
}

func netIP(host string) string {
	if host == "localhost" {
		return "127.0.0.1"
	}
	return host
}

func parseAPITime(raw json.RawMessage) (time.Time, error) {
	if len(raw) == 0 || string(raw) == "null" {
		return time.Time{}, errors.New("empty time")
	}
	if raw[0] >= '0' && raw[0] <= '9' {
		var milliseconds int64
		if err := json.Unmarshal(raw, &milliseconds); err != nil {
			return time.Time{}, err
		}
		return time.UnixMilli(milliseconds), nil
	}
	var value string
	if err := json.Unmarshal(raw, &value); err != nil {
		return time.Time{}, err
	}
	formats := []string{
		time.RFC3339Nano,
		"2006-01-02 15:04:05",
		"2006-01-02T15:04:05.000-0700",
	}
	for _, format := range formats {
		if parsed, err := time.Parse(format, value); err == nil {
			return parsed, nil
		}
	}
	if milliseconds, err := strconv.ParseInt(value, 10, 64); err == nil {
		return time.UnixMilli(milliseconds), nil
	}
	return time.Time{}, fmt.Errorf("unsupported time format %q", value)
}
