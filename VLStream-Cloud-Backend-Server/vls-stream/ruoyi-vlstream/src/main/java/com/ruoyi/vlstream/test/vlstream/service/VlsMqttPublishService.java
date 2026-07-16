/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONUtil;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * MQTT消息发送服务
 */
@Slf4j
@Service
public class VlsMqttPublishService {

	@Resource
	private VlsMqttProperties vlsMqttProperties;

	public boolean publish(String topic, Object payload) {
		if (StringUtils.isBlank(topic) || !StringUtils.startsWith(topic, "oortcloud/")) {
			log.error("MQTT消息发送失败，topic仅允许oortcloud/#: topic={}", topic);
			return false;
		}

		MqttClient mqttClient = null;
		try {
			mqttClient = createMqttClient();
			mqttClient.connect(buildMqttConnectOptions());
			MqttMessage mqttMessage = new MqttMessage(JSONUtil.toJsonStr(payload).getBytes(StandardCharsets.UTF_8));
			mqttMessage.setQos(vlsMqttProperties.getQos());
			mqttClient.publish(topic, mqttMessage);
			return true;
		} catch (Exception publishException) {
			log.error("MQTT消息发送异常: topic={}", topic, publishException);
			return false;
		} finally {
			closeMqttClient(mqttClient);
		}
	}

	private MqttClient createMqttClient() throws MqttException {
		String clientIdPrefix = StringUtils.defaultIfBlank(vlsMqttProperties.getClientIdPrefix(), "vls-dispatch");
		String brokerUrl = "tcp://" + vlsMqttProperties.getHost() + ":" + vlsMqttProperties.getPort();
		String clientId = clientIdPrefix + "-" + UUID.randomUUID();
		return new MqttClient(brokerUrl, clientId);
	}

	private MqttConnectOptions buildMqttConnectOptions() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setAutomaticReconnect(true);
		connectOptions.setCleanSession(true);
		connectOptions.setConnectionTimeout(vlsMqttProperties.getConnectionTimeoutSeconds());
		connectOptions.setKeepAliveInterval(vlsMqttProperties.getKeepAliveSeconds());
		if (StringUtils.isNotBlank(vlsMqttProperties.getUsername())) {
			connectOptions.setUserName(vlsMqttProperties.getUsername());
		}
		if (StringUtils.isNotBlank(vlsMqttProperties.getPassword())) {
			connectOptions.setPassword(vlsMqttProperties.getPassword().toCharArray());
		}
		return connectOptions;
	}

	private void closeMqttClient(MqttClient mqttClient) {
		if (mqttClient == null) {
			return;
		}
		try {
			if (mqttClient.isConnected()) {
				mqttClient.disconnect();
			}
		} catch (MqttException disconnectException) {
			log.warn("关闭MQTT连接失败", disconnectException);
		}
		try {
			mqttClient.close();
		} catch (MqttException closeException) {
			log.warn("关闭MQTT客户端失败", closeException);
		}
	}

}
