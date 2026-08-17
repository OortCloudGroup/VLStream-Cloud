package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Correlates firmwareDeploy progress replies with durable OTA tasks. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirmwareDeployReplyMqttMessageHandler implements VlsMqttMessageHandler {

	private final FirmwareDeployTaskService taskService;

	@Override
	public boolean supports(String mainBizType, String subBizType) {
		return VlsMqttProtocol.isDeviceBiz(mainBizType)
			&& VlsMqttProtocol.FIRMWARE_DEPLOY.equals(subBizType);
	}

	@Override
	public JSONObject handle(JSONObject envelope, String rawPayload) {
		JSONObject payload = envelope.getJSONObject("payload");
		JSONObject bizData = payload == null ? null : payload.getJSONObject("bizData");
		String sourceMsgId = payload == null ? null : payload.getStr("sourceMsgId");
		String requestId = bizData == null ? null : bizData.getStr("requestId");
		String status = bizData == null ? null : bizData.getStr("status");
		String deviceModel = bizData == null ? null : bizData.getStr("deviceModel");
		String target = bizData == null ? null : bizData.getStr("target");
		String version = bizData == null ? null : bizData.getStr("version");
		Integer code = payload == null ? null : payload.getInt("code");
		if (StringUtils.isBlank(status)) {
			status = statusFromCode(code);
		}
		String message = payload == null ? null : payload.getStr("msg");
		String errDetail = payload == null ? null : payload.getStr("errDetail");
		if (StringUtils.isNotBlank(errDetail) && !StringUtils.equals(message, errDetail)) {
			message = StringUtils.defaultString(message) + ": " + errDetail;
		}
		String deviceId = envelope.getStr("deviceId");
		if (StringUtils.isAnyBlank(sourceMsgId, requestId, deviceId, status,
			deviceModel, target, version)) {
			log.warn("Ignoring incomplete firmware deploy reply: deviceId={}", deviceId);
			return null;
		}
		if (!taskService.applyHardwareReply(sourceMsgId, requestId, deviceId,
			deviceModel, target, version, bizData.getStr("fileSha256"),
			status, message, rawPayload)) {
			log.warn("Ignoring unmatched firmware deploy reply: sourceMsgId={}, requestId={}, deviceId={}",
				sourceMsgId, requestId, deviceId);
		}
		return null;
	}

	private String statusFromCode(Integer code) {
		if (code == null) return null;
		if (code == 102) return "ACCEPTED";
		if (code == 200) return "SUCCESS";
		if (code >= 400) return "FAILED";
		return null;
	}
}
