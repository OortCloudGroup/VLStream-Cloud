/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** WVP service-to-service device lookup settings. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.wvp-device")
public class WvpVlStreamDeviceProperties {

	private String baseUrl = "http://127.0.0.1:9080";
	private int connectTimeoutMillis = 3000;
	private int readTimeoutMillis = 5000;
}
