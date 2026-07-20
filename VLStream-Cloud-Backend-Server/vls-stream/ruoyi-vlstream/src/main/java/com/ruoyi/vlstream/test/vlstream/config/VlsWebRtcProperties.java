/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebRTC-streamer integration settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.webrtc")
public class VlsWebRtcProperties {

	/** Whether real-time WebRTC playback is enabled. */
	private boolean enabled = true;

	/** Media gateway implementation exposed to the frontend. */
	private String provider = "webrtc-streamer";

	/** Browser-visible URL, normally routed by the frontend reverse proxy. */
	private String publicBaseUrl = "/bus/webrtc-streamer-server";

	/** Backend-only URL used for health checks inside Docker or Kubernetes. */
	private String internalBaseUrl = "http://127.0.0.1:8000";

	/** Lightweight WebRTC-streamer endpoint used as the health probe. */
	private String healthPath = "/api/getMediaList";

	/** TCP connection timeout for a health probe. */
	private int connectTimeoutMillis = 1000;

	/** Response timeout for a health probe. */
	private int readTimeoutMillis = 1500;

	/** Cache duration used by the config endpoint to avoid probing on every UI request. */
	private long healthCacheMillis = 5000L;
}
