/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsWebRtcProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides public WebRTC configuration and real gateway health information.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsWebRtcGatewayService {

	private final VlsWebRtcProperties properties;
	private volatile ProbeResult cachedProbe;

	/**
	 * Builds the browser-safe runtime configuration without exposing the internal container URL.
	 */
	public Map<String, Object> getPublicConfig() {
		ProbeResult probe = probe(false);
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("enabled", properties.isEnabled());
		config.put("available", probe.isAvailable());
		config.put("serverUrl", normalizeBaseUrl(properties.getPublicBaseUrl()));
		config.put("provider", StringUtils.defaultIfBlank(properties.getProvider(), "webrtc-streamer"));
		config.put("checkedAt", probe.getCheckedAt());
		return config;
	}

	/**
	 * Forces a fresh gateway check for the explicit status endpoint.
	 */
	public Map<String, Object> getStatus() {
		ProbeResult probe = probe(true);
		Map<String, Object> status = new LinkedHashMap<>();
		status.put("enabled", properties.isEnabled());
		status.put("available", probe.isAvailable());
		status.put("provider", StringUtils.defaultIfBlank(properties.getProvider(), "webrtc-streamer"));
		status.put("httpStatus", probe.getHttpStatus());
		status.put("latencyMillis", probe.getLatencyMillis());
		status.put("checkedAt", probe.getCheckedAt());
		status.put("message", probe.getMessage());
		return status;
	}

	/**
	 * Uses a short-lived cache for UI configuration and bypasses it for explicit status checks.
	 */
	private ProbeResult probe(boolean force) {
		if (!properties.isEnabled()) {
			return ProbeResult.disabled();
		}
		long now = System.currentTimeMillis();
		ProbeResult current = cachedProbe;
		if (!force && current != null && now - current.getCheckedAtMillis() < Math.max(0L, properties.getHealthCacheMillis())) {
			return current;
		}
		synchronized (this) {
			current = cachedProbe;
			if (!force && current != null && now - current.getCheckedAtMillis() < Math.max(0L, properties.getHealthCacheMillis())) {
				return current;
			}
			cachedProbe = executeProbe();
			return cachedProbe;
		}
	}

	/**
	 * Performs a bounded HTTP GET against a lightweight WebRTC-streamer endpoint.
	 */
	private ProbeResult executeProbe() {
		long startedAt = System.currentTimeMillis();
		HttpURLConnection connection = null;
		try {
			URL healthUrl = new URL(buildHealthUrl());
			connection = (HttpURLConnection) healthUrl.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(Math.max(100, properties.getConnectTimeoutMillis()));
			connection.setReadTimeout(Math.max(100, properties.getReadTimeoutMillis()));
			connection.setUseCaches(false);
			connection.setRequestProperty("User-Agent", "VLStream-WebRTC-Health/1.0");
			int status = connection.getResponseCode();
			long latency = System.currentTimeMillis() - startedAt;
			boolean available = status >= 200 && status < 400;
			return ProbeResult.completed(available, status, latency,
				available ? "WebRTC-streamer可用" : "WebRTC-streamer返回HTTP " + status);
		} catch (IOException exception) {
			long latency = System.currentTimeMillis() - startedAt;
			log.debug("WebRTC-streamer health probe failed: {}", exception.getMessage());
			return ProbeResult.completed(false, null, latency, "WebRTC-streamer不可用: " + exception.getMessage());
		} catch (RuntimeException exception) {
			long latency = System.currentTimeMillis() - startedAt;
			log.warn("WebRTC-streamer health configuration is invalid: {}", exception.getMessage());
			return ProbeResult.completed(false, null, latency, "WebRTC-streamer配置无效: " + exception.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Joins the internal base URL and health path without producing duplicate separators.
	 */
	private String buildHealthUrl() {
		String baseUrl = normalizeBaseUrl(properties.getInternalBaseUrl());
		if (StringUtils.isBlank(baseUrl)) {
			throw new IllegalArgumentException("vlstream.webrtc.internal-base-url不能为空");
		}
		String healthPath = StringUtils.defaultIfBlank(properties.getHealthPath(), "/api/getMediaList");
		return baseUrl + (healthPath.startsWith("/") ? healthPath : "/" + healthPath);
	}

	/**
	 * Removes trailing slashes while preserving a root-relative or absolute URL.
	 */
	private String normalizeBaseUrl(String value) {
		return StringUtils.removeEnd(StringUtils.trimToEmpty(value), "/");
	}

	/** Immutable result of one gateway health probe. */
	private static final class ProbeResult {
		private final boolean available;
		private final Integer httpStatus;
		private final long latencyMillis;
		private final long checkedAtMillis;
		private final String message;

		/** Creates one immutable probe result. */
		private ProbeResult(boolean available, Integer httpStatus, long latencyMillis, long checkedAtMillis, String message) {
			this.available = available;
			this.httpStatus = httpStatus;
			this.latencyMillis = latencyMillis;
			this.checkedAtMillis = checkedAtMillis;
			this.message = message;
		}

		/** Creates a result for an intentionally disabled integration. */
		private static ProbeResult disabled() {
			return completed(false, null, 0L, "WebRTC播放未启用");
		}

		/** Creates a timestamped result for a completed check. */
		private static ProbeResult completed(boolean available, Integer httpStatus, long latencyMillis, String message) {
			return new ProbeResult(available, httpStatus, latencyMillis, System.currentTimeMillis(), message);
		}

		/** Returns whether the media gateway responded successfully. */
		private boolean isAvailable() {
			return available;
		}

		/** Returns the HTTP response status when a response was received. */
		private Integer getHttpStatus() {
			return httpStatus;
		}

		/** Returns the measured end-to-end probe latency. */
		private long getLatencyMillis() {
			return latencyMillis;
		}

		/** Returns the epoch timestamp used for cache expiry. */
		private long getCheckedAtMillis() {
			return checkedAtMillis;
		}

		/** Returns an ISO-8601 timestamp for API clients. */
		private String getCheckedAt() {
			return Instant.ofEpochMilli(checkedAtMillis).toString();
		}

		/** Returns a human-readable status explanation. */
		private String getMessage() {
			return message;
		}
	}
}
