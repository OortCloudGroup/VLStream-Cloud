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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Maintains the shared VLS MQTT device-bus connection and publishes handler replies. */
@Slf4j
@Service
public class VlsMqttBusService {

	@Resource
	private VlsMqttProperties mqttProperties;

	@Resource
	private VlsModelDispatchProperties dispatchProperties;

	@Resource
	private VlsMqttInboundDispatcher inboundDispatcher;

	private final Object clientMonitor = new Object();
	private final ExecutorService inboundExecutor = Executors.newFixedThreadPool(2, runnable -> {
		Thread thread = new Thread(runnable, "vls-mqtt-inbound");
		thread.setDaemon(true);
		return thread;
	});
	private final ScheduledExecutorService connectionExecutor =
		Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "vls-mqtt-connect");
			thread.setDaemon(true);
			return thread;
		});
	private MqttClient client;

	@PostConstruct
	public void initialize() {
		connectionExecutor.scheduleWithFixedDelay(
			this::connectIfNecessary, 0, 10, TimeUnit.SECONDS);
	}

	void connectIfNecessary() {
		try {
			ensureConnected();
		} catch (Exception ex) {
			log.warn("VLS MQTT device bus connection failed, retrying in 10 seconds: {}:{} - {}",
				mqttProperties.getHost(), mqttProperties.getPort(), ex.getMessage());
		}
	}

	public void publish(String topic, Object payload) {
		if (!isAllowedTopic(topic)) {
			throw new IllegalArgumentException("Invalid VLS MQTT device bus topic: " + topic);
		}
		try {
			MqttClient connectedClient = ensureConnected();
			MqttMessage message = new MqttMessage(
				JSONUtil.toJsonStr(payload).getBytes(StandardCharsets.UTF_8));
			message.setQos(qos());
			message.setRetained(false);
			connectedClient.publish(topic, message);
		} catch (Exception ex) {
			throw new IllegalStateException("VLS MQTT publish failed: " + ex.getMessage(), ex);
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
			subscribeDeviceBus(client);
			return client;
		}
	}

	private MqttCallbackExtended callback() {
		return new MqttCallbackExtended() {
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				if (reconnect && client != null) {
					try {
						subscribeDeviceBus(client);
					} catch (Exception ex) {
						log.error("Failed to restore VLS MQTT device bus subscription", ex);
					}
				}
			}

			@Override
			public void connectionLost(Throwable cause) {
				log.warn("VLS MQTT device bus connection lost: {}",
					cause == null ? "unknown" : cause.getMessage());
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
				inboundExecutor.submit(() -> handleIncomingMessage(topic, payload));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// QoS delivery completion is not a hardware business acknowledgement.
			}
		};
	}

	void handleIncomingMessage(String topic, String payload) {
		JSONObject reply = inboundDispatcher.dispatch(topic, payload);
		if (reply != null) {
			publish(topic, reply);
		}
	}

	private void subscribeDeviceBus(MqttClient mqttClient) throws Exception {
		mqttClient.subscribe(VlsMqttProtocol.BUS_TOPIC_FILTER, qos());
		log.info("Subscribed to VLS 2.2 device bus: {}", VlsMqttProtocol.BUS_TOPIC_FILTER);
	}

	private boolean isAllowedTopic(String topic) {
		return StringUtils.isNotBlank(topic)
			&& topic.matches("vlstream/v2\\.2/dev/[^/+#]+/bus");
	}

	private int qos() {
		return 1;
	}

	MqttConnectOptions connectOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		// Reconnection is exclusively owned by connectionExecutor. Enabling Paho's
		// automatic reconnect at the same time can create a second client with the
		// same client ID and make the broker repeatedly take over the old session.
		options.setAutomaticReconnect(false);
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
		connectionExecutor.shutdownNow();
		inboundExecutor.shutdownNow();
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
			log.debug("Failed to close VLS MQTT device bus client", ex);
		} finally {
			client = null;
		}
	}
}
