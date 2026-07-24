/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * Long-lived MQTT connection used for model dispatch and device acknowledgements.
 */
@Slf4j
@Service
public class ModelDispatchMqttService {

	@Resource
	private VlsMqttProperties mqttProperties;

	@Resource
	private VlsModelDispatchProperties dispatchProperties;

	@Resource
	private ModelDispatchTaskService taskService;

	private final Object clientMonitor = new Object();
	private MqttClient client;

	@PostConstruct
	public void initialize() {
		try {
			ensureConnected();
		} catch (Exception ex) {
			log.warn("MQTT acknowledgement subscriber is not connected yet: {}", ex.getMessage());
		}
	}

	public void publish(String topic, Object payload) {
		if (!isAllowedTopic(topic, false)) {
			throw new IllegalArgumentException("Invalid MQTT model dispatch topic: " + topic);
		}
		try {
			MqttClient connectedClient = ensureConnected();
			MqttMessage message = new MqttMessage(
				JSONUtil.toJsonStr(payload).getBytes(StandardCharsets.UTF_8));
			message.setQos(qos());
			message.setRetained(false);
			connectedClient.publish(topic, message);
		} catch (Exception ex) {
			throw new IllegalStateException("MQTT model dispatch failed: " + ex.getMessage(), ex);
		}
	}

	private MqttClient ensureConnected() throws Exception {
		synchronized (clientMonitor) {
			if (client != null && client.isConnected()) {
				return client;
			}
			closeClient();
			String brokerUrl = "tcp://" + mqttProperties.getHost() + ":" + mqttProperties.getPort();
			String clientId = StringUtils.defaultIfBlank(
				dispatchProperties.getMqttClientId(), "vls-model-dispatch-backend");
			client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
			client.setCallback(callback());
			client.connect(connectOptions());
			subscribeReplies(client);
			return client;
		}
	}

	private MqttCallbackExtended callback() {
		return new MqttCallbackExtended() {
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				if (reconnect && client != null) {
					try {
						subscribeReplies(client);
					} catch (Exception ex) {
						log.error("Failed to restore MQTT model reply subscription", ex);
					}
				}
			}

			@Override
			public void connectionLost(Throwable cause) {
				log.warn("MQTT model dispatch connection lost: {}",
					cause == null ? "unknown" : cause.getMessage());
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				handleReply(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// QoS delivery completion is not a hardware deployment acknowledgement.
			}
		};
	}

	private void handleReply(String topic, String payload) {
		try {
			JSONObject json = JSONUtil.parseObj(payload);
			String requestId = json.getStr("requestId");
			String deviceId = json.getStr("deviceId");
			String status = json.getStr("status");
			String message = json.getStr("message");
			if (StringUtils.isAnyBlank(requestId, deviceId, status)) {
				log.warn("Ignoring incomplete model dispatch reply: topic={}", topic);
				return;
			}
			if (!taskService.applyHardwareReply(requestId, deviceId, status, message, payload)) {
				log.warn("Ignoring unmatched model dispatch reply: requestId={}, deviceId={}",
					requestId, deviceId);
			}
		} catch (Exception ex) {
			log.error("Failed to process model dispatch reply: topic={}", topic, ex);
		}
	}

	private void subscribeReplies(MqttClient mqttClient) throws Exception {
		String topic = dispatchProperties.getReplyTopic();
		if (!isAllowedTopic(topic, true)) {
			throw new IllegalStateException("Invalid MQTT model reply topic: " + topic);
		}
		mqttClient.subscribe(topic, qos());
		log.info("Subscribed to MQTT model dispatch replies: {}", topic);
	}

	private boolean isAllowedTopic(String topic, boolean allowWildcard) {
		if (StringUtils.isBlank(topic)) {
			return false;
		}
		String prefix = StringUtils.defaultIfBlank(mqttProperties.getTopicPrefix(), "oortcloud") + "/";
		if (!topic.startsWith(prefix)) {
			return false;
		}
		return allowWildcard || (!topic.contains("#") && !topic.contains("+"));
	}

	private int qos() {
		Integer configured = mqttProperties.getQos();
		return configured == null ? 1 : Math.max(0, Math.min(configured, 2));
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(defaultInt(mqttProperties.getConnectionTimeoutSeconds(), 10));
		options.setKeepAliveInterval(defaultInt(mqttProperties.getKeepAliveSeconds(), 60));
		if (StringUtils.isNotBlank(mqttProperties.getUsername())) {
			options.setUserName(mqttProperties.getUsername());
		}
		if (StringUtils.isNotBlank(mqttProperties.getPassword())) {
			options.setPassword(mqttProperties.getPassword().toCharArray());
		}
		return options;
	}

	private int defaultInt(Integer value, int fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	@PreDestroy
	public void destroy() {
		synchronized (clientMonitor) {
			closeClient();
		}
	}

	private void closeClient() {
		if (client == null) {
			return;
		}
		try {
			if (client.isConnected()) {
				client.disconnect();
			}
			client.close();
		} catch (Exception ex) {
			log.debug("Failed to close MQTT model dispatch client", ex);
		} finally {
			client = null;
		}
	}
}
