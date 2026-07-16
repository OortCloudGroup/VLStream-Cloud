/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.service.IVlsWebRtcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URI;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration-backed WebRTC compatibility adapter for VLStream-Web.
 */
@Service
public class VlsWebRtcServiceImpl implements IVlsWebRtcService {

    private static final String DEFAULT_SERVER_URL = "http://oort.oortcloudsmart.com:21410/bus/webrtc-streamer-server";

    @Value("${vls.webrtc.server-url:" + DEFAULT_SERVER_URL + "}")
    private String serverUrl;

    @Value("${vls.webrtc.enabled:true}")
    private boolean enabled;

    @Value("${vls.webrtc.available:true}")
    private boolean available;

    private final Map<String, Map<String, Object>> activeStreams = new ConcurrentHashMap<String, Map<String, Object>>();

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> config = baseState();
        config.put("iceServers", Arrays.asList("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"));
        config.put("mode", "config-adapter");
        return config;
    }

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = baseState();
        boolean reachable = enabled && isServerReachable();
        status.put("running", reachable);
        status.put("available", reachable);
        status.put("activeCount", 0);
        status.put("preparedCount", activeStreams.size());
        status.put("checkedAt", new Date());
        if (!reachable) {
            status.put("message", enabled ? "WebRTC server is unreachable" : "WebRTC is disabled");
        }
        return status;
    }

    @Override
    public Map<String, Object> refresh() {
        Map<String, Object> status = getStatus();
        boolean reachable = Boolean.TRUE.equals(status.get("running"));
        status.put("refreshed", reachable);
        status.put("message", reachable ? "WebRTC server reachability check succeeded"
            : String.valueOf(status.get("message")));
        return status;
    }

    @Override
    public Map<String, Object> startStream(String deviceId, String rtspUrl, Map<String, Object> options) {
        if (!enabled) {
            throw new IllegalStateException("WebRTC is disabled");
        }
        if (!StringUtils.hasText(rtspUrl)) {
            throw new IllegalArgumentException("rtspUrl is required; no stream was started");
        }
        Map<String, Object> validation = validateRtspStream(rtspUrl);
        if (!Boolean.TRUE.equals(validation.get("available"))) {
            throw new IllegalStateException(String.valueOf(validation.get("message")));
        }
        if (!isServerReachable()) {
            throw new IllegalStateException("WebRTC server is unreachable; no playback URL was prepared");
        }

        String streamId = StringUtils.hasText(deviceId) ? deviceId.trim() : "stream_" + System.currentTimeMillis();
        Map<String, Object> stream = new LinkedHashMap<String, Object>();
        stream.put("streamId", streamId);
        stream.put("deviceId", streamId);
        stream.put("rtspUrl", rtspUrl);
        stream.put("streamUrl", rtspUrl);
        stream.put("webrtcUrl", normalizeServerUrl() + "/webrtcstreamer.html?video=" + encode(rtspUrl));
        stream.put("serverUrl", normalizeServerUrl());
        stream.put("active", false);
        stream.put("prepared", true);
        stream.put("status", "prepared");
        stream.put("message", "WebRTC server and RTSP endpoint are reachable; browser negotiation is still required");
        stream.put("startTime", new Date());
        if (options != null) {
            stream.put("options", new LinkedHashMap<String, Object>(options));
        }
        activeStreams.put(streamId, stream);
        return copy(stream);
    }

    @Override
    public Map<String, Object> stopStream(String streamId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (!StringUtils.hasText(streamId)) {
            throw new IllegalArgumentException("streamId is required; browser-owned WebRTC sessions were not stopped");
        }

        Map<String, Object> removed = activeStreams.remove(streamId.trim());
        result.put("streamId", streamId);
        result.put("deviceId", streamId);
        result.put("stopped", false);
        result.put("registrationRemoved", removed != null);
        result.put("active", false);
        result.put("message", removed != null
            ? "Prepared playback registration removed; the browser must close its own WebRTC peer connection"
            : "Prepared playback registration does not exist");
        return result;
    }

    @Override
    public Map<String, Object> validateRtspStream(String rtspUrl) {
        boolean valid = StringUtils.hasText(rtspUrl)
            && (rtspUrl.trim().toLowerCase().startsWith("rtsp://") || rtspUrl.trim().toLowerCase().startsWith("rtsps://"));
        boolean reachable = valid && isRtspReachable(rtspUrl);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rtspUrl", rtspUrl);
        result.put("valid", valid);
        result.put("available", reachable);
        result.put("message", !valid ? "RTSP URL must start with rtsp:// or rtsps://"
            : reachable ? "RTSP TCP endpoint is reachable" : "RTSP TCP endpoint is unreachable");
        return result;
    }

    @Override
    public List<Map<String, Object>> getActiveStreams() {
        List<Map<String, Object>> streams = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> stream : activeStreams.values()) {
            if (Boolean.TRUE.equals(stream.get("active"))) {
                streams.add(copy(stream));
            }
        }
        return streams;
    }

    @Override
    public Map<String, Object> checkStream(String streamId) {
        if (StringUtils.hasText(streamId)) {
            Map<String, Object> stream = activeStreams.get(streamId.trim());
            if (stream != null) {
                return copy(stream);
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("streamId", streamId);
        result.put("deviceId", streamId);
        result.put("active", false);
        result.put("status", "stopped");
        result.put("serverUrl", normalizeServerUrl());
        return result;
    }

    private Map<String, Object> baseState() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("serverUrl", normalizeServerUrl());
        state.put("webrtcServerUrl", normalizeServerUrl());
        state.put("baseUrl", normalizeServerUrl());
        state.put("available", enabled && available);
        state.put("enabled", enabled);
        return state;
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<String, Object>(source);
    }

    private String normalizeServerUrl() {
        String value = StringUtils.hasText(serverUrl) ? serverUrl.trim() : DEFAULT_SERVER_URL;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not supported", ex);
        }
    }

    /** Probe the configured WebRTC HTTP server instead of trusting configuration flags. */
    private boolean isServerReachable() {
        if (!available) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(normalizeServerUrl()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int status = connection.getResponseCode();
            return status >= 200 && status < 500;
        } catch (Exception ex) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Probe the RTSP TCP endpoint without claiming that media negotiation succeeded. */
    private boolean isRtspReachable(String rtspUrl) {
        Socket socket = new Socket();
        try {
            URI uri = URI.create(rtspUrl.trim());
            if (!StringUtils.hasText(uri.getHost())) {
                return false;
            }
            socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 554), 5000);
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
                // Closing a completed probe must not change its result.
            }
        }
    }
}
