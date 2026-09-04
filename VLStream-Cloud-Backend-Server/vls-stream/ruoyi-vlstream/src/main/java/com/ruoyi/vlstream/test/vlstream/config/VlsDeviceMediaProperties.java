/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Device event media upload settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.device-media")
public class VlsDeviceMediaProperties {

	/**
	 * OSS config key. Empty means the current default OSS configuration.
	 */
	private String ossConfigKey;

	/**
	 * Browser/device-accessible S3 API endpoint used while calculating the signature.
	 * It must not be replaced by a reverse proxy after the URL has been signed.
	 */
	private String publicEndpoint;

	/**
	 * PUT URL validity in seconds.
	 */
	private Integer uploadUrlTtlSeconds = 600;

	/**
	 * Maximum event image size in bytes.
	 */
	private Long maxImageBytes = 10L * 1024L * 1024L;

	/**
	 * Temporary LAN-only switch. Production must set this to false and enable device authentication.
	 */
	private Boolean allowUnauthenticated = false;
}
