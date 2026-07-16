/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT settings used by device-side VLS dispatch and camera configuration messages.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.mqtt")
public class VlsMqttProperties {

    private String host;

    private Integer port = 1883;

    private String username;

    private String password;

    private String topicPrefix = "oortcloud";

    private String dispatchAlgorithmsTopic = "oortcloud/dispatchAlgorithms";

    private String vlsCameraDisplaySettingTopic = "oortcloud/vlsCameraDisplaySetting";

    private String vlsCameraOsdSettingTopic = "oortcloud/vlsCameraOsdSetting";

    private String vlsAudioAnomalyDetectionSettingTopic = "oortcloud/vlsAudioAnomalyDetectionSetting";

    private String vlsAudioDefenseTimeSettingTopic = "oortcloud/vlsAudioDefenseTimeSetting";

    private String vlsAudioLinkageModeSettingTopic = "oortcloud/vlsAudioLinkageModeSetting";

    private String vlsTimeStrategyTopic = "oortcloud/vlsTimeStrategy";

    private String vlsRecordEventStrategyTopic = "oortcloud/vlsRecordEventStrategy";

    private String clientIdPrefix = "vls-dispatch";

    private Integer qos = 1;

    private Integer keepAliveSeconds = 60;

    private Integer connectionTimeoutSeconds = 10;
}
