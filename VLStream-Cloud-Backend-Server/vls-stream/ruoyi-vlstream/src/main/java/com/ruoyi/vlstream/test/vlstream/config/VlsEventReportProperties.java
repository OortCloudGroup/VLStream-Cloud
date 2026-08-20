/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for anonymous third-party event delivery. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.event-report")
public class VlsEventReportProperties {

	private Boolean enabled = Boolean.TRUE;
	private String url;
	private Integer batchSize = 50;
	private Integer timeoutMillis = 5000;
	private Integer maxRetries = 10;
	private Long baseRetryMillis = 10000L;
	private Long maxRetryMillis = 1800000L;
	private Long staleLockMillis = 60000L;
	private Integer mediaUrlTtlSeconds = 3600;
}
