/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/** Validates VLS MQTT envelopes and delegates them to registered business handlers. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsMqttInboundDispatcher {

	private final List<VlsMqttMessageHandler> messageHandlers;

	@Autowired(required = false)
	private DeviceEventFlowDiagnostics diagnostics;

	public JSONObject dispatch(String topic, String rawPayload) {
		try {
			JSONObject envelope = JSONUtil.parseObj(rawPayload);
			if (!VlsMqttProtocol.VERSION.equals(envelope.getStr("protocolVersion"))
				|| !VlsMqttProtocol.DEVICE_TO_PLATFORM.equals(envelope.getStr("msgDir"))) {
				record(DeviceEventFlowDiagnostics.Counter.MQTT_IGNORED_ENVELOPE);
				return null;
			}
			record(DeviceEventFlowDiagnostics.Counter.MQTT_DEVICE_ENVELOPE);
			String deviceId = envelope.getStr("deviceId");
			if (!StringUtils.equals(topic, VlsMqttProtocol.deviceBusTopic(deviceId))) {
				record(DeviceEventFlowDiagnostics.Counter.MQTT_TOPIC_REJECTED);
				log.warn("Ignoring VLS message on another device bus: topic={}, deviceId={}",
					topic, deviceId);
				return null;
			}
			String mainBizType = envelope.getStr("mainBizType");
			String subBizType = envelope.getStr("subBizType");
			if (VlsMqttProtocol.isDeviceBiz(mainBizType) && VlsMqttProtocol.STATE.equals(subBizType)) {
				record(DeviceEventFlowDiagnostics.Counter.MQTT_DEVICE_STATE);
			} else if (VlsMqttProtocol.AI_BIZ.equals(mainBizType)) {
				if (VlsMqttProtocol.FACE_EVENT.equals(subBizType)) record(DeviceEventFlowDiagnostics.Counter.MQTT_AI_FACE);
				if (VlsMqttProtocol.STRUCT_EVENT.equals(subBizType)) record(DeviceEventFlowDiagnostics.Counter.MQTT_AI_STRUCT);
			}
			for (VlsMqttMessageHandler handler : messageHandlers) {
				if (handler.supports(mainBizType, subBizType)) {
					return handler.handle(envelope, rawPayload);
				}
			}
			log.debug("No VLS MQTT handler registered: mainBizType={}, subBizType={}",
				mainBizType, subBizType);
			record(DeviceEventFlowDiagnostics.Counter.MQTT_UNSUPPORTED);
			return null;
		} catch (Exception ex) {
			record(DeviceEventFlowDiagnostics.Counter.MQTT_DISPATCH_ERROR);
			log.error("Failed to dispatch VLS MQTT message: topic={}", topic, ex);
			return null;
		}
	}

	private void record(DeviceEventFlowDiagnostics.Counter counter) {
		if (diagnostics != null) diagnostics.record(counter);
	}
}
