package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Routes face and structured recognition events. */
@Component
@RequiredArgsConstructor
public class DeviceEventMqttMessageHandler implements VlsMqttMessageHandler {

	private final DeviceEventMqttHandler deviceEventHandler;

	@Override
	public boolean supports(String mainBizType, String subBizType) {
		return VlsMqttProtocol.AI_BIZ.equals(mainBizType)
			&& (VlsMqttProtocol.FACE_EVENT.equals(subBizType)
			|| VlsMqttProtocol.STRUCT_EVENT.equals(subBizType));
	}

	@Override
	public JSONObject handle(JSONObject envelope, String rawPayload) {
		return deviceEventHandler.handle(envelope);
	}
}
