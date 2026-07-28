/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Model dispatch download-link and acknowledgement settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.model-dispatch")
public class VlsModelDispatchProperties {

	/**
	 * Base URL reachable by field devices, without a trailing slash.
	 */
	private String publicBaseUrl;

	/**
	 * HMAC secret used to sign short-lived model download URLs.
	 */
	private String signingSecret;

	/**
	 * Download URL validity in seconds.
	 */
	private Long downloadUrlTtlSeconds = 1800L;

	/**
	 * Stable MQTT client ID for the acknowledgement subscriber.
	 */
	private String mqttClientId = "vls-model-dispatch-backend";
}
