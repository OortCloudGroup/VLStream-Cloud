package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/** Validates VLS MQTT envelopes and delegates them to registered business handlers. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsMqttInboundDispatcher {

	private final List<VlsMqttMessageHandler> messageHandlers;

	public JSONObject dispatch(String topic, String rawPayload) {
		try {
			JSONObject envelope = JSONUtil.parseObj(rawPayload);
			if (!VlsMqttProtocol.VERSION.equals(envelope.getStr("protocolVersion"))
				|| !VlsMqttProtocol.DEVICE_TO_PLATFORM.equals(envelope.getStr("msgDir"))) {
				return null;
			}
			String deviceId = envelope.getStr("deviceId");
			if (!StringUtils.equals(topic, VlsMqttProtocol.deviceBusTopic(deviceId))) {
				log.warn("Ignoring VLS message on another device bus: topic={}, deviceId={}",
					topic, deviceId);
				return null;
			}
			String mainBizType = envelope.getStr("mainBizType");
			String subBizType = envelope.getStr("subBizType");
			for (VlsMqttMessageHandler handler : messageHandlers) {
				if (handler.supports(mainBizType, subBizType)) {
					return handler.handle(envelope, rawPayload);
				}
			}
			log.debug("No VLS MQTT handler registered: mainBizType={}, subBizType={}",
				mainBizType, subBizType);
			return null;
		} catch (Exception ex) {
			log.error("Failed to dispatch VLS MQTT message: topic={}", topic, ex);
			return null;
		}
	}
}
