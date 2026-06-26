package org.springblade.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT configuration
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.mqtt")
public class VlsMqttProperties {

	/**
	 * MQTT service address
	 */
	private String host;

	/**
	 * MQTT service port
	 */
	private Integer port;

	/**
	 * MQTT username
	 */
	private String username;

	/**
	 * MQTT password
	 */
	private String password;

	/**
	 * Common topic prefix
	 */
	private String topicPrefix = "oortcloud";

	/**
	 * Camera display settings message topic
	 */
	private String vlsCameraDisplaySettingTopic = "oortcloud/vlsCameraDisplaySetting";

	/**
	 * Camera OSD settings message topic
	 */
	private String vlsCameraOsdSettingTopic = "oortcloud/vlsCameraOsdSetting";

	/**
	 * Audio exception detection settings message topic
	 */
	private String vlsAudioAnomalyDetectionSettingTopic = "oortcloud/vlsAudioAnomalyDetectionSetting";

	/**
	 * Audio arming schedule setting message topic
	 */
	private String vlsAudioDefenseTimeSettingTopic = "oortcloud/vlsAudioDefenseTimeSetting";

	/**
	 * Audio linkage mode settings message topic
	 */
	private String vlsAudioLinkageModeSettingTopic = "oortcloud/vlsAudioLinkageModeSetting";

	/**
	 * Time strategy message topic
	 */
	private String vlsTimeStrategyTopic = "oortcloud/vlsTimeStrategy";

	/**
	 * Recording event strategy message topic
	 */
	private String vlsRecordEventStrategyTopic = "oortcloud/vlsRecordEventStrategy";

	/**
	 * MQTT client ID prefix
	 */
	private String clientIdPrefix = "vls-dispatch";

	/**
	 * MQTT message Quality of Service (QoS)
	 */
	private Integer qos = 1;

	/**
	 * MQTT keep-alive duration (seconds)
	 */
	private Integer keepAliveSeconds = 60;

	/**
	 * MQTT connection timeout (seconds)
	 */
	private Integer connectionTimeoutSeconds = 10;

}
