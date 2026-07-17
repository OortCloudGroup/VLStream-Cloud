/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.mqtt")
public class VlsMqttProperties {

	/**
	 * MQTT服务地址
	 */
	private String host;

	/**
	 * MQTT服务端口
	 */
	private Integer port;

	/**
	 * MQTT用户名
	 */
	private String username;

	/**
	 * MQTT密码
	 */
	private String password;

	/**
	 * 通用主题前缀
	 */
	private String topicPrefix = "oortcloud";

    private String dispatchAlgorithmsTopic = "oortcloud/dispatchAlgorithms";


    /**
	 * 摄像机显示设置消息主题
	 */
	private String vlsCameraDisplaySettingTopic = "oortcloud/vlsCameraDisplaySetting";

	/**
	 * 摄像机OSD设置消息主题
	 */
	private String vlsCameraOsdSettingTopic = "oortcloud/vlsCameraOsdSetting";

	/**
	 * 音频异常侦测设置消息主题
	 */
	private String vlsAudioAnomalyDetectionSettingTopic = "oortcloud/vlsAudioAnomalyDetectionSetting";

	/**
	 * 音频布防时间设置消息主题
	 */
	private String vlsAudioDefenseTimeSettingTopic = "oortcloud/vlsAudioDefenseTimeSetting";

	/**
	 * 音频联动方式设置消息主题
	 */
	private String vlsAudioLinkageModeSettingTopic = "oortcloud/vlsAudioLinkageModeSetting";

	/**
	 * 时间策略消息主题
	 */
	private String vlsTimeStrategyTopic = "oortcloud/vlsTimeStrategy";

	/**
	 * 录像事件策略消息主题
	 */
	private String vlsRecordEventStrategyTopic = "oortcloud/vlsRecordEventStrategy";

	/**
	 * MQTT客户端ID前缀
	 */
	private String clientIdPrefix = "vls-dispatch";

	/**
	 * MQTT消息质量等级
	 */
	private Integer qos = 1;

	/**
	 * MQTT心跳保持时长（秒）
	 */
	private Integer keepAliveSeconds = 60;

	/**
	 * MQTT连接超时（秒）
	 */
	private Integer connectionTimeoutSeconds = 10;

}
