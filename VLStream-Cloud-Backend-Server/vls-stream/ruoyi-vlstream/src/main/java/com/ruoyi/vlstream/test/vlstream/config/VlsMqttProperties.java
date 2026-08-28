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
 * MQTTconfiguration
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.mqtt")
public class VlsMqttProperties {

	/**
	 * MQTTservice
	 */
	private String host;

	/**
	 * MQTTservice
	 */
	private Integer port;

	/**
	 * MQTTuser
	 */
	private String username;

	/**
	 * MQTT
	 */
	private String password;

	/**
	 * main before
	 */
	private String topicPrefix = "oortcloud";

    /**
	 * Set main
	 */
	private String vlsCameraDisplaySettingTopic = "oortcloud/vlsCameraDisplaySetting";

	/**
	 * OSDSet main
	 */
	private String vlsCameraOsdSettingTopic = "oortcloud/vlsCameraOsdSetting";

	/**
	 * Set main
	 */
	private String vlsAudioAnomalyDetectionSettingTopic = "oortcloud/vlsAudioAnomalyDetectionSetting";

	/**
	 * Set main
	 */
	private String vlsAudioDefenseTimeSettingTopic = "oortcloud/vlsAudioDefenseTimeSetting";

	/**
	 * Set main
	 */
	private String vlsAudioLinkageModeSettingTopic = "oortcloud/vlsAudioLinkageModeSetting";

	/**
	 * main
	 */
	private String vlsTimeStrategyTopic = "oortcloud/vlsTimeStrategy";

	/**
	 * recordingevent main
	 */
	private String vlsRecordEventStrategyTopic = "oortcloud/vlsRecordEventStrategy";

	/**
	 * MQTT ID before
	 */
	private String clientIdPrefix = "vls-dispatch";

	/**
	 * MQTT etc.
	 */
	private Integer qos = 1;

	/**
	 * MQTT ( )
	 */
	private Integer keepAliveSeconds = 60;

	/**
	 * MQTT ( )
	 */
	private Integer connectionTimeoutSeconds = 10;

}
