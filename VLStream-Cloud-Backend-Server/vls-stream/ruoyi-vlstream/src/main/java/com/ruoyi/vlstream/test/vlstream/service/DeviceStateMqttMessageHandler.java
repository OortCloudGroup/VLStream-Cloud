package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Routes native device state snapshots to the persistence service. */
@Component
@ConditionalOnProperty(value = "vlstream.native-device.legacy-enabled", havingValue = "true")
@RequiredArgsConstructor
public class DeviceStateMqttMessageHandler implements VlsMqttMessageHandler {

	private final MqttDeviceStateService deviceStateService;

	@Override
	public boolean supports(String mainBizType, String subBizType) {
		return VlsMqttProtocol.isDeviceBiz(mainBizType)
			&& VlsMqttProtocol.STATE.equals(subBizType);
	}

	@Override
	public JSONObject handle(JSONObject envelope, String rawPayload) {
		return deviceStateService.handle(envelope);
	}
}
