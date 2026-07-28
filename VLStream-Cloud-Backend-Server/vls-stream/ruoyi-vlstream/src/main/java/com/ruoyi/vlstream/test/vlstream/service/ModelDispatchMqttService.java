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
				handleIncomingMessage(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// QoS delivery completion is not a hardware deployment acknowledgement.
			}
		};
	}

	void handleIncomingMessage(String topic, String payload) {
		try {
			JSONObject json = JSONUtil.parseObj(payload);
			if (!VlsMqttProtocol.VERSION.equals(json.getStr("protocolVersion"))
				|| !VlsMqttProtocol.DEVICE_TO_PLATFORM.equals(json.getStr("msgDir"))
				|| !VlsMqttProtocol.AI_BIZ.equals(json.getStr("mainBizType"))
				|| !VlsMqttProtocol.MODEL_DEPLOY.equals(json.getStr("subBizType"))) {
				return;
			}
			String deviceId = json.getStr("deviceId");
			if (!StringUtils.equals(topic, VlsMqttProtocol.deviceBusTopic(deviceId))) {
				log.warn("Ignoring modelDeploy reply on another device bus: topic={}, deviceId={}",
					topic, deviceId);
				return;
			}

			JSONObject reply = json.getJSONObject("payload");
			JSONObject bizData = reply == null ? null : reply.getJSONObject("bizData");
			String sourceMsgId = reply == null ? null : reply.getStr("sourceMsgId");
			String requestId = bizData == null ? null : bizData.getStr("requestId");
			String status = bizData == null ? null : bizData.getStr("status");
			String fileSha256 = bizData == null ? null : bizData.getStr("fileSha256");
			String message = reply == null ? null : reply.getStr("msg");
			String errDetail = reply == null ? null : reply.getStr("errDetail");
			if (StringUtils.isNotBlank(errDetail) && !StringUtils.equals(message, errDetail)) {
				message = StringUtils.defaultString(message) + ": " + errDetail;
			}
			if (StringUtils.isAnyBlank(sourceMsgId, requestId, deviceId, status)) {
				log.warn("Ignoring incomplete model dispatch reply: topic={}", topic);
				return;
			}
			if (!taskService.applyHardwareReply(
				sourceMsgId, requestId, deviceId, status, fileSha256, message, payload)) {
				log.warn("Ignoring unmatched model dispatch reply: sourceMsgId={}, requestId={}, deviceId={}",
					sourceMsgId, requestId, deviceId);
			}
		} catch (Exception ex) {
			log.error("Failed to process model dispatch reply: topic={}", topic, ex);
		}
	}

	private void subscribeReplies(MqttClient mqttClient) throws Exception {
		String topic = VlsMqttProtocol.BUS_TOPIC_FILTER;
		mqttClient.subscribe(topic, qos());
		log.info("Subscribed to VLS 2.2 device bus: {}", topic);
	}

	private boolean isAllowedTopic(String topic, boolean allowWildcard) {
		if (StringUtils.isBlank(topic)) {
			return false;
		}
		if (allowWildcard) {
			return VlsMqttProtocol.BUS_TOPIC_FILTER.equals(topic);
		}
		return topic.matches("vlstream/v2\\.2/dev/[^/+#]+/bus");
	}

	private int qos() {
		return 1;
	}

	private MqttConnectOptions connectOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(false);
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
