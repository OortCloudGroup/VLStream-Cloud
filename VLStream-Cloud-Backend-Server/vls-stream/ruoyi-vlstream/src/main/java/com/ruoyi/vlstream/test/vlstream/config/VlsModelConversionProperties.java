/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Remote model conversion tool paths and HiSilicon target settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.model-conversion")
public class VlsModelConversionProperties {

	private String hisiliconExporterScript = "/data/work/svp_pc/exporter/export_hisilicon_yolov8.py";

	private String atcEnvScript = "/data/work/svp_pc/Ascend/ascend-toolkit/svp_latest/x86_64-linux/script/setenv.sh";

	private String insertOpConfig = "/data/work/svp_pc/V3.0.0.12/sample/yolo/insert_op.cfg";

	private String socVersion = "Hi3519DV500";

	private Integer calibrationImageCount = 20;
}
