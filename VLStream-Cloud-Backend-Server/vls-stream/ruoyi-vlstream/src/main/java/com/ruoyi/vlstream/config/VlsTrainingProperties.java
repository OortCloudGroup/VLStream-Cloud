/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Remote YOLO training command defaults.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.training")
public class VlsTrainingProperties {

    private String condaProfile = "/data/work/anaconda3/etc/profile.d/conda.sh";

    private String defaultCondaEnv = "yolo8";

    private String defaultWorkDir = "/data/work/ultralytics_yolov8-main/datasets";

    private String logDir = "logs";

    private String synsetFileName = "synset.txt";
}
