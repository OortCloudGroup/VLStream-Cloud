/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Consumes VLS 2.2 faceEvent/struct messages after MQTT routing validation.
 */
@Service
public class DeviceEventMqttHandler {

	@Resource
	private VlsDeviceInfoMapper deviceInfoMapper;

	@Resource
	private DeviceMediaUploadService mediaUploadService;

	@Resource
	private ActiveSafetyEventReportService activeSafetyEventReportService;

	@Transactional(rollbackFor = Exception.class)
	public JSONObject handle(JSONObject envelope) {
		String sourceMessageId = envelope.getStr("messageId");
		String deviceId = envelope.getStr("deviceId");
		String subBizType = envelope.getStr("subBizType");
		JSONObject payload = envelope.getJSONObject("payload");
		String eventId = payload == null ? null
			: StringUtils.defaultIfBlank(payload.getStr("eventId"), sourceMessageId);
		String mediaId = null;
		try {
			if (StringUtils.isAnyBlank(sourceMessageId, deviceId, eventId) || payload == null) {
				throw new IllegalArgumentException("事件 messageId、deviceId、eventId 和 payload 均不能为空");
			}
			ActiveSafetyEventReportResult duplicate = activeSafetyEventReportService.findDuplicate(
				sourceMessageId, deviceId, eventId);
			if (duplicate != null) {
				return buildReply(envelope, eventId, duplicate.getMediaId(), true,
					"事件已处理，重复消息已忽略");
			}
			DeviceInfo device = deviceInfoMapper.selectOne(
				new LambdaQueryWrapper<DeviceInfo>()
					.eq(DeviceInfo::getDeviceId, deviceId)
					.last("limit 1"));
			if (device == null) {
				throw new IllegalArgumentException("平台不存在该设备");
			}

			JSONObject media = firstMedia(payload);
			if (media == null) {
				throw new IllegalArgumentException("事件必须包含已上传的 media 图片信息");
			}
			mediaId = media.getStr("mediaId");
			String objectKey = media.getStr("objectKey");
			String sha256 = media.getStr("sha256");
			if (StringUtils.isAnyBlank(mediaId, objectKey, sha256)) {
				throw new IllegalArgumentException("mediaId、objectKey、sha256 均不能为空");
			}
			DeviceMediaUpload upload = mediaUploadService.validateAndBind(
				mediaId, deviceId, objectKey, sha256, sourceMessageId);
			ActiveSafetyEventReportResult result = activeSafetyEventReportService.report(
				ActiveSafetyEventReport.builder()
					.sourceMessageId(sourceMessageId)
					.deviceEventId(eventId)
					.deviceId(deviceId)
					.deviceName(StringUtils.defaultIfBlank(device.getDeviceName(), deviceId))
					.deviceTag(device.getTag())
					.eventType(StringUtils.defaultIfBlank(payload.getStr("eventType"),
						defaultEventType(subBizType, payload)))
					.description(StringUtils.defaultIfBlank(payload.getStr("eventDesc"),
						defaultDescription(subBizType, payload)))
					.eventTime(parseEventTime(envelope, payload))
					.mediaId(upload.getMediaId())
					.address(device.getAddress())
					.longitude(device.getLongitude())
					.latitude(device.getLatitude())
					.build());
			if (result == null) {
				throw new IllegalStateException("事件写入数据库失败");
			}
			return buildReply(envelope, eventId, mediaId, true, "事件已接收并入库");
		} catch (Exception ex) {
			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			}
			return buildReply(envelope, eventId, mediaId, false,
				StringUtils.defaultIfBlank(ex.getMessage(), "事件处理失败"));
		}
	}

	private JSONObject firstMedia(JSONObject payload) {
		JSONArray mediaList = payload.getJSONArray("media");
		if (mediaList != null && !mediaList.isEmpty()) {
			return mediaList.getJSONObject(0);
		}
		if (StringUtils.isNotBlank(payload.getStr("mediaId"))) {
			JSONObject media = new JSONObject();
			media.put("mediaId", payload.getStr("mediaId"));
			media.put("objectKey", payload.getStr("objectKey"));
			media.put("sha256", payload.getStr("sha256"));
			return media;
		}
		return null;
	}

	private String defaultDescription(String subBizType, JSONObject payload) {
		return VlsMqttProtocol.FACE_EVENT.equals(subBizType)
			? "设备上报人脸识别事件"
			: "设备上报结构化识别事件：" + StringUtils.defaultIfBlank(payload.getStr("eventType"),
				StringUtils.defaultIfBlank(payload.getStr("type"), "unknown"));
	}

	private String defaultEventType(String subBizType, JSONObject payload) {
		if (VlsMqttProtocol.FACE_EVENT.equals(subBizType)) {
			return "faceEvent";
		}
		return "struct:" + StringUtils.defaultIfBlank(payload.getStr("type"), "unknown");
	}

	private Date parseEventTime(JSONObject envelope, JSONObject payload) {
		String value = StringUtils.defaultIfBlank(payload.getStr("eventTime"),
			StringUtils.defaultIfBlank(payload.getStr("datetime"), envelope.getStr("sentAt")));
		JSONObject info = payload.getJSONObject("info");
		if (StringUtils.isBlank(value) && info != null) {
			value = info.getStr("time");
		}
		if (StringUtils.isBlank(value)) {
			return new Date();
		}
		try {
			return Date.from(Instant.parse(value));
		} catch (Exception ignored) {
			try {
				return parseLocalDateTime(value);
			} catch (ParseException ignoredAgain) {
				return new Date();
			}
		}
	}

	private Date parseLocalDateTime(String value) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setLenient(false);
		format.setTimeZone(TimeZone.getDefault());
		return format.parse(value);
	}

	private JSONObject buildReply(JSONObject request, String eventId, String mediaId,
								  boolean success, String message) {
		JSONObject bizData = new JSONObject();
		bizData.put("eventId", eventId);
		bizData.put("mediaId", mediaId);
		bizData.put("status", success ? "SUCCESS" : "FAILED");

		JSONObject payload = new JSONObject();
		payload.put("sourceMsgId", request.getStr("messageId"));
		payload.put("code", success ? 200 : 500);
		payload.put("msg", message);
		payload.put("errCode", success ? 0 : 50001);
		payload.put("errDetail", success ? "" : message);
		payload.put("bizData", bizData);

		JSONObject reply = new JSONObject();
		reply.put("protocolVersion", VlsMqttProtocol.VERSION);
		reply.put("messageId", UUID.randomUUID().toString());
		reply.put("deviceId", request.getStr("deviceId"));
		reply.put("sentAt", Instant.now().toString());
		reply.put("msgDir", VlsMqttProtocol.PLATFORM_TO_DEVICE);
		reply.put("mainBizType", VlsMqttProtocol.AI_BIZ);
		reply.put("subBizType", request.getStr("subBizType"));
		reply.put("payload", payload);
		reply.put("extend", new JSONObject());
		return reply;
	}
}
