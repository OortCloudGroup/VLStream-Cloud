/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Firmware package storage and signed URL settings. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.firmware")
public class VlsFirmwareProperties {

	/** Empty means the current default sys_oss_config entry. */
	private String ossConfigKey;

	/**
	 * MinIO endpoint reachable by browsers and cameras. Empty means the OSS endpoint.
	 * The host is signed and therefore must not be rewritten after URL generation.
	 */
	private String publicEndpoint;

	/** Platform HTTP base URL reachable by cameras for short OTA download links. */
	private String platformBaseUrl;

	private Integer uploadUrlTtlSeconds = 600;

	private Integer downloadUrlTtlSeconds = 1800;

	private Integer otaDownloadUrlTtlSeconds = 7200;

	/** Maximum silence after VERIFYING/INSTALLING/REBOOTING before the task times out. */
	private Integer otaTaskInactivityTimeoutMinutes = 30;

	private Long maxPackageBytes = 160L * 1024L * 1024L;
}
