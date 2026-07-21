/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Hardware model dispatch endpoint and device-accessible model download settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.hardware-dispatch")
public class VlsHardwareDispatchProperties {

	private String endpointUrl = "http://192.168.88.98:8888/vlsDeviceInfo/latest-training-model";

	private String modelDownloadBaseUrl = "http://192.168.88.31:8080";

	private Integer requestTimeoutMillis = 10000;
}
