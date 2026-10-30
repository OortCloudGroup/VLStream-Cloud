/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ruoyi.vlstream.test.vlstream.config.VlsDeviceMediaProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.common.helper.TenantContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Consumes VLS 2.2 faceEvent/struct messages after MQTT routing validation.
 */
@Service
public class DeviceEventMqttHandler {

	@Resource
	private DeviceMediaUploadService mediaUploadService;

	@Resource
	private VlsDeviceMediaProperties deviceMediaProperties;

	@Resource
	private ActiveSafetyEventReportService activeSafetyEventReportService;

	@Resource
	private WvpVlStreamDeviceResolver wvpDeviceResolver;

	@Resource
	private VlsEventReportApplicationService eventReportApplicationService;

	@Resource
	private LlmReviewTaskService llmReviewTaskService;

	@Autowired(required = false)
	private DeviceEventFlowDiagnostics diagnostics;

	@Transactional(rollbackFor = Exception.class)
	public JSONObject handle(JSONObject envelope) {
		String sourceMessageId = envelope.getStr("messageId");
		String deviceId = envelope.getStr("deviceId");
		String subBizType = envelope.getStr("subBizType");
		JSONObject payload = envelope.getJSONObject("payload");
		String eventId = payload == null ? null
			: StringUtils.defaultIfBlank(payload.getStr("eventId"), sourceMessageId);
		String mediaId = null;
		String previousTenant = TenantContextHolder.getTenantId();
		DeviceEventFlowDiagnostics.Counter rejectionStage = DeviceEventFlowDiagnostics.Counter.EVENT_REJECT_ENVELOPE;
		try {
			if (StringUtils.isAnyBlank(sourceMessageId, deviceId, eventId) || payload == null) {
				throw new IllegalArgumentException("事件 messageId、deviceId、eventId 和 payload 均不能为空");
			}
			boolean multiTenant = eventReportApplicationService.isMultiTenant();
			boolean platformFaceEvent = multiTenant && VlsMqttProtocol.FACE_EVENT.equals(subBizType);
			rejectionStage = DeviceEventFlowDiagnostics.Counter.EVENT_REJECT_DEVICE;
			DeviceInfo device = multiTenant ? wvpDeviceResolver.resolveEventDevice(deviceId)
				: wvpDeviceResolver.resolve(deviceId);
			TenantContextHolder.setTenantId(device.getTenantId());

			rejectionStage = DeviceEventFlowDiagnostics.Counter.EVENT_REJECT_PERSIST;
			ActiveSafetyEventReportResult duplicate = activeSafetyEventReportService.findDuplicate(
				sourceMessageId, deviceId, eventId);
			if (duplicate != null) {
				committed(DeviceEventFlowDiagnostics.Counter.EVENT_DUPLICATE);
				return buildReply(envelope, eventId, duplicate.getMediaId(), true,
					"事件已处理，重复消息已忽略");
			}

			rejectionStage = DeviceEventFlowDiagnostics.Counter.EVENT_REJECT_MEDIA;
			JSONArray mediaList = payload.getJSONArray("media");
			if (mediaList == null || mediaList.isEmpty()) {
				JSONObject legacy = firstMedia(payload);
				if (legacy == null) throw new IllegalArgumentException("事件必须包含已上传的 media 媒体信息");
				mediaList = new JSONArray();
				mediaList.add(legacy);
			}
			if (mediaList.size() > 2) throw new IllegalArgumentException("每次事件最多上报一张抓拍图片和一个 MP4");
			List<String> imageIds = new ArrayList<String>();
			List<String> videoIds = new ArrayList<String>();
			Set<String> uniqueIds = new HashSet<String>();
			boolean externalMedia = false;
			DeviceMediaUpload upload = null;
			for (int index = 0; index < mediaList.size(); index++) {
				JSONObject media = mediaList.getJSONObject(index);
				String currentId = media == null ? null : media.getStr("mediaId");
				String objectKey = media == null ? null : media.getStr("objectKey");
				String sha256 = media == null ? null : media.getStr("sha256");
				if (StringUtils.isAnyBlank(currentId, objectKey, sha256) || !uniqueIds.add(currentId)) {
					throw new IllegalArgumentException("mediaId、objectKey、sha256 均不能为空且媒体不得重复");
				}
				if (mediaId == null) mediaId = currentId;
				boolean external = Boolean.TRUE.equals(deviceMediaProperties.getAllowExternalEventMedia())
					&& mediaUploadService.getByMediaId(currentId) == null;
				if (external) { externalMedia = true; continue; }
				DeviceMediaUpload validated = mediaUploadService.validateAndBind(
					currentId, deviceId, objectKey, sha256, sourceMessageId);
				if (DeviceMediaUploadService.isVideoType(validated.getContentType())) {
					videoIds.add(currentId);
					if (upload == null) upload = validated;
				} else {
					imageIds.add(currentId);
					upload = validated;
				}
			}
			if (imageIds.size() > 1 || videoIds.size() > 1) {
				throw new IllegalArgumentException("每次事件最多上报一张抓拍图片和一个 MP4");
			}
			String eventType = StringUtils.defaultIfBlank(payload.getStr("eventType"),
				defaultEventType(subBizType, payload));
			String description = StringUtils.defaultIfBlank(payload.getStr("eventDesc"),
				defaultDescription(subBizType, payload));
			Date eventTime = parseEventTime(envelope, payload);
			rejectionStage = DeviceEventFlowDiagnostics.Counter.EVENT_REJECT_REVIEW;
			if (!platformFaceEvent && llmReviewTaskService != null
				&& llmReviewTaskService.enqueueIfRequired(envelope, device, upload,
					eventId, eventType, description, eventTime)) {
				committed(DeviceEventFlowDiagnostics.Counter.EVENT_REVIEW_QUEUED);
				return buildReply(envelope, eventId, mediaId, true, "事件已接收，等待大模型复核");
			}
			rejectionStage = DeviceEventFlowDiagnostics.Counter.EVENT_REJECT_PERSIST;
			ActiveSafetyEventReportResult result = activeSafetyEventReportService.report(
					ActiveSafetyEventReport.builder()
						.sourceMessageId(sourceMessageId)
						.deviceEventId(eventId)
						.deviceId(deviceId)
						.deviceName(StringUtils.defaultIfBlank(device.getDeviceName(), deviceId))
						.deviceTag(device.getTag())
						.eventType(eventType)
						.description(description)
						.eventTime(eventTime)
						.mediaId(mediaId)
						.imageMediaIds(imageIds)
						.videoMediaIds(videoIds)
						.externalMedia(externalMedia)
						.mqttPayloadJson(payload.toString())
						.address(device.getAddress())
						.longitude(device.getLongitude())
						.latitude(device.getLatitude())
						.build());
			if (result == null) {
				throw new IllegalStateException("事件写入数据库失败");
			}
			committed(DeviceEventFlowDiagnostics.Counter.EVENT_BUSINESS_COMMITTED);
			return buildReply(envelope, eventId, mediaId, true, multiTenant
				? "事件已接收，等待外部业务处理" : "事件已接收并入库");
		} catch (Exception ex) {
			if (diagnostics != null) diagnostics.record(rejectionStage);
			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			}
			return buildReply(envelope, eventId, mediaId, false,
				StringUtils.defaultIfBlank(ex.getMessage(), "事件处理失败"));
		} finally {
			TenantContextHolder.setTenantId(previousTenant);
		}
	}

	private void committed(DeviceEventFlowDiagnostics.Counter counter) {
		if (diagnostics != null) diagnostics.afterCommit(counter);
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
			: StringUtils.defaultIfBlank(payload.getStr("eventType"),
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
