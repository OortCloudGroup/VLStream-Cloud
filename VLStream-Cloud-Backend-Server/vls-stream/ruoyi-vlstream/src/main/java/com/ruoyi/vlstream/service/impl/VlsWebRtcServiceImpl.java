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
        status.put("running", enabled);
        status.put("activeCount", activeStreams.size());
        status.put("checkedAt", new Date());
        return status;
    }

    @Override
    public Map<String, Object> refresh() {
        Map<String, Object> status = getStatus();
        status.put("refreshed", true);
        status.put("message", "WebRTC configuration refreshed");
        return status;
    }

    @Override
    public Map<String, Object> startStream(String deviceId, String rtspUrl, Map<String, Object> options) {
        if (!StringUtils.hasText(rtspUrl)) {
            Map<String, Object> status = getStatus();
            status.put("started", true);
            status.put("message", "WebRTC service adapter is ready");
            return status;
        }

        String streamId = StringUtils.hasText(deviceId) ? deviceId.trim() : "stream_" + System.currentTimeMillis();
        Map<String, Object> stream = new LinkedHashMap<String, Object>();
        stream.put("streamId", streamId);
        stream.put("deviceId", streamId);
        stream.put("rtspUrl", rtspUrl);
        stream.put("streamUrl", rtspUrl);
        stream.put("webrtcUrl", normalizeServerUrl() + "/webrtcstreamer.html?video=" + encode(rtspUrl));
        stream.put("serverUrl", normalizeServerUrl());
        stream.put("active", true);
        stream.put("status", "playing");
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
            int stoppedCount = activeStreams.size();
            activeStreams.clear();
            result.put("stopped", true);
            result.put("stoppedCount", stoppedCount);
            result.put("message", "All WebRTC streams stopped");
            return result;
        }

        Map<String, Object> removed = activeStreams.remove(streamId.trim());
        result.put("streamId", streamId);
        result.put("deviceId", streamId);
        result.put("stopped", removed != null);
        result.put("active", false);
        result.put("message", removed != null ? "WebRTC stream stopped" : "WebRTC stream is not active");
        return result;
    }

    @Override
    public Map<String, Object> validateRtspStream(String rtspUrl) {
        boolean valid = StringUtils.hasText(rtspUrl)
            && (rtspUrl.trim().toLowerCase().startsWith("rtsp://") || rtspUrl.trim().toLowerCase().startsWith("rtsps://"));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rtspUrl", rtspUrl);
        result.put("valid", valid);
        result.put("available", valid);
        result.put("message", valid ? "RTSP URL format is valid" : "RTSP URL must start with rtsp:// or rtsps://");
        return result;
    }

    @Override
    public List<Map<String, Object>> getActiveStreams() {
        List<Map<String, Object>> streams = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> stream : activeStreams.values()) {
            streams.add(copy(stream));
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
}
