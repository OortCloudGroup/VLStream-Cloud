package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Applies hardware model deployment acknowledgements to durable dispatch tasks. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelDeployReplyMqttMessageHandler implements VlsMqttMessageHandler {

	private final ModelDispatchTaskService taskService;

	@Override
	public boolean supports(String mainBizType, String subBizType) {
		return VlsMqttProtocol.AI_BIZ.equals(mainBizType)
			&& VlsMqttProtocol.MODEL_DEPLOY.equals(subBizType);
	}

	@Override
	public JSONObject handle(JSONObject envelope, String rawPayload) {
		JSONObject reply = envelope.getJSONObject("payload");
		JSONObject bizData = reply == null ? null : reply.getJSONObject("bizData");
		String sourceMsgId = reply == null ? null : reply.getStr("sourceMsgId");
		String requestId = bizData == null ? null : bizData.getStr("requestId");
		String deviceId = envelope.getStr("deviceId");
		String status = bizData == null ? null : bizData.getStr("status");
		String fileSha256 = bizData == null ? null : bizData.getStr("fileSha256");
		String message = reply == null ? null : reply.getStr("msg");
		String errDetail = reply == null ? null : reply.getStr("errDetail");
		if (StringUtils.isNotBlank(errDetail) && !StringUtils.equals(message, errDetail)) {
			message = StringUtils.defaultString(message) + ": " + errDetail;
		}
		if (StringUtils.isAnyBlank(sourceMsgId, requestId, deviceId, status)) {
			log.warn("Ignoring incomplete model dispatch reply: deviceId={}", deviceId);
			return null;
		}
		if (!taskService.applyHardwareReply(
			sourceMsgId, requestId, deviceId, status, fileSha256, message, rawPayload)) {
			log.warn("Ignoring unmatched model dispatch reply: sourceMsgId={}, requestId={}, deviceId={}",
				sourceMsgId, requestId, deviceId);
		}
		return null;
	}
}
