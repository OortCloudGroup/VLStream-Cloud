/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Publishes VLS dispatch and device-setting messages through MQTT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsMqttPublishService {

    private final VlsMqttProperties properties;
    private final ObjectMapper objectMapper;

    public void publishOrThrow(String topic, Object payload) {
        if (!StringUtils.hasText(topic) || !topic.startsWith("oortcloud/")) {
            throw new IllegalArgumentException("MQTT topic only allows oortcloud/#: " + topic);
        }
        if (!StringUtils.hasText(properties.getHost())) {
            throw new IllegalStateException("MQTT host is not configured");
        }

        MqttClient mqttClient = null;
        try {
            mqttClient = createMqttClient();
            mqttClient.connect(connectOptions());
            byte[] payloadBytes = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(payloadBytes);
            message.setQos(properties.getQos() == null ? 1 : properties.getQos());
            mqttClient.publish(topic, message);
            log.info("MQTT message published: topic={}, payloadBytes={}", topic, payloadBytes.length);
        } catch (Exception ex) {
            throw new IllegalStateException("MQTT publish failed: " + ex.getMessage(), ex);
        } finally {
            close(mqttClient);
        }
    }

    private MqttClient createMqttClient() throws MqttException {
        String clientIdPrefix = StringUtils.hasText(properties.getClientIdPrefix())
            ? properties.getClientIdPrefix()
            : "vls-dispatch";
        String brokerUrl = "tcp://" + properties.getHost() + ":" + properties.getPort();
        return new MqttClient(brokerUrl, clientIdPrefix + "-" + UUID.randomUUID());
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(defaultInt(properties.getConnectionTimeoutSeconds(), 10));
        options.setKeepAliveInterval(defaultInt(properties.getKeepAliveSeconds(), 60));
        if (StringUtils.hasText(properties.getUsername())) {
            options.setUserName(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            options.setPassword(properties.getPassword().toCharArray());
        }
        return options;
    }

    private void close(MqttClient client) {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException ex) {
            log.warn("Failed to disconnect MQTT client", ex);
        }
        try {
            client.close();
        } catch (MqttException ex) {
            log.warn("Failed to close MQTT client", ex);
        }
    }

    private static int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
