/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vlstream.training-container")
public class VlsTrainingContainerProperties {
	private String image = "vlstream/yolo-training:8.3.240";
	private Integer gpuIndex = 0;
	private String gpuUuid = "GPU-46808c9f-cd48-1feb-5304-5342999ca622";
	private String cpuLimit = "12";
	private String memoryLimit = "12g";
	private String shmSize = "4g";
	private Integer workers = 2;
	private String hostDataDir = "/data/work";
	private String ultralyticsConfigDir = "/data/work/ultralytics_yolov8-main/.ultralytics";
	private long scheduleIntervalMillis = 3000L;
}
