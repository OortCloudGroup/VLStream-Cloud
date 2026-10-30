/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ResolvedCameraEvent;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/** Shared application boundary used by HTTP and MQTT event ingestion. */
@Service
@RequiredArgsConstructor
public class VlsEventReportApplicationService {

	private static final String MULTI = "multi";
	private static final String MEDIA_REFERENCE_PREFIX = "vls-media://";

	private final IVlsEventManagementService eventManagementService;
	private final VlsEventManagementMapper eventManagementMapper;
	private final EventReportOutboxMapper outboxMapper;
	private final IFileUploadService fileUploadService;

	@Value("${token.tenant-type:single}")
	private String tenantType;

	public boolean isMultiTenant() {
		return MULTI.equalsIgnoreCase(StringUtils.trimToEmpty(tenantType));
	}

	public EventManagement findDuplicate(String mqttMessageId, String deviceId, String deviceEventId) {
		if (StringUtils.isAnyBlank(mqttMessageId, deviceId, deviceEventId)) {
			return null;
		}
		return eventManagementMapper.selectOne(new LambdaQueryWrapper<EventManagement>()
			.eq(EventManagement::getMqttMessageId, mqttMessageId)
			.or(wrapper -> wrapper.eq(EventManagement::getReportDevice, deviceId)
				.eq(EventManagement::getDeviceEventId, deviceEventId))
			.last("limit 1"));
	}

	@Transactional(rollbackFor = Exception.class)
	public EventManagement persistDeviceEvent(EventManagement event, DeviceInfo device) {
		return persistDeviceEvent(event, device, null, null);
	}

	/** Cloud detection is visible immediately; external delivery waits for its bounded evidence tail. */
	@Transactional(rollbackFor = Exception.class)
	public EventManagement persistCloudAiEvent(EventManagement event, DeviceInfo device) {
		if (!isMultiTenant()) throw new ServiceException("单租户云端事件必须使用主动安全事件入口");
		if (event == null || device == null || StringUtils.isBlank(device.getTenantId())) throw new ServiceException("云端事件缺少设备归属");
		event.setTenantId(device.getTenantId()); event.setIsReport(isMultiTenant() ? 0 : 1);
		if (!eventManagementService.createEvent(event) || event.getId() == null) throw new ServiceException("云端事件保存失败");
		return event;
	}

	/** Caller holds the cloud event row lock and marks final evidence in the same transaction. */
	@Transactional(rollbackFor = Exception.class)
	public void publishCloudAiEvidence(EventManagement event, DeviceInfo device, List<String> images, List<String> videos) {
		if (!isMultiTenant()) return;
		JSONObject payload = buildPayload(event, device, null);
		payload.set("pics", mediaReferences(images)); payload.set("video", mediaReferences(videos));
		enqueue(event, device, null, payload);
	}

	/** The same multi-tenant sink is used before and after optional LLM review. */
	@Transactional(rollbackFor = Exception.class)
	public EventManagement persistActiveSafetyEvent(ActiveSafetyEventReport report, DeviceInfo device) {
		if (report == null || StringUtils.isAnyBlank(report.getSourceMessageId(),
			report.getDeviceEventId(), report.getDeviceId(), report.getMediaId())
			|| device == null || !StringUtils.equals(report.getDeviceId(), device.getDeviceId())) {
			throw new ServiceException("MQTT 事件或设备身份不完整");
		}
		EventManagement event = new EventManagement();
		event.setMqttMessageId(report.getSourceMessageId());
		event.setDeviceEventId(report.getDeviceEventId());
		event.setMediaId(report.getMediaId());
		event.setEventDesc(report.getDescription());
		event.setHandleResult(report.getDescription());
		event.setEventType(report.getEventType());
		event.setReportDevice(report.getDeviceId());
		event.setReportLocation(report.getAddress());
		event.setReportTime(report.getEventTime());
		event.setEventData(report.getMqttPayloadJson());
		List<String> pictures = mediaReferences(report.getImageMediaIds());
		event.setReportImg(pictures.isEmpty() ? null : pictures.get(0));
		return persistDeviceEvent(event, device, report, null);
	}

	/** All ownership lookups are complete before this database-only transaction starts. */
	@Transactional(rollbackFor = Exception.class)
	public List<EventManagement> persistCameraEvents(List<ResolvedCameraEvent> reports) {
		if (!isMultiTenant() || reports == null || reports.isEmpty()) {
			throw new ServiceException("多租户设备事件不能为空");
		}
		String previous = TenantContextHolder.getTenantId();
		try {
			List<EventManagement> result = new ArrayList<>();
			for (ResolvedCameraEvent report : reports) {
				if (report == null || report.getDevice() == null || StringUtils.isBlank(report.getDevice().getTenantId())) {
					throw new ServiceException("设备事件缺少可信归属");
				}
				TenantContextHolder.setTenantId(report.getDevice().getTenantId());
				result.add(persistDeviceEvent(report.getEvent(), report.getDevice(), null, report.getPayload()));
			}
			return result;
		} finally {
			TenantContextHolder.setTenantId(previous);
		}
	}

	private EventManagement persistDeviceEvent(EventManagement event, DeviceInfo device,
											  ActiveSafetyEventReport report, JSONObject cameraPayload) {
		if (event == null || device == null || StringUtils.isBlank(device.getTenantId())) {
			throw new ServiceException("事件和设备租户信息不能为空");
		}
		event.setTenantId(device.getTenantId());
		event.setIsReport(isMultiTenant() ? 0 : 1);
		if (!eventManagementService.createEvent(event) || event.getId() == null) {
			throw new ServiceException("事件写入数据库失败");
		}
		if (isMultiTenant()) {
			enqueue(event, device, report, cameraPayload);
		}
		return event;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean reportMultipartEvent(EventManagement event, MultipartFile multipartFile, DeviceInfo device) {
		if (event == null || multipartFile == null || multipartFile.isEmpty()) {
			throw new ServiceException("事件信息和事件文件不能为空");
		}
		File localFile = fileUploadService.multipartFileToFile(multipartFile);
		if (localFile == null) {
			throw new ServiceException("事件文件转换失败");
		}
		FileResponseDto uploaded = fileUploadService.uploadFile("", "", localFile);
		if (uploaded == null || StringUtils.isBlank(uploaded.getUrl())) {
			throw new ServiceException("事件文件上传失败");
		}
		event.setReportImg(uploaded.getUrl());
		JSONObject payload = null;
		if (isMultiTenant()) {
			payload = buildPayload(event, device, null);
			if (StringUtils.startsWithIgnoreCase(multipartFile.getContentType(), "video/")) {
				payload.set("pics", Collections.emptyList());
				payload.set("video", Collections.singletonList(uploaded.getUrl()));
				event.setReportImg(null);
			}
		}
		persistDeviceEvent(event, device, null, payload);
		return true;
	}

	private void enqueue(EventManagement event, DeviceInfo device, ActiveSafetyEventReport report, JSONObject cameraPayload) {
		Date now = new Date();
		EventReportOutbox outbox = new EventReportOutbox();
		outbox.setTenantId(event.getTenantId());
		outbox.setEventId(event.getId());
		outbox.setIdempotencyKey("vls-event:" + event.getTenantId() + ":" + event.getId());
		JSONObject payload = cameraPayload == null ? buildPayload(event, device, report)
			: new JSONObject(cameraPayload.toString());
		payload.set("device_id", event.getReportDevice());
		payload.set("device_tenant_id", event.getTenantId());
		payload.set("source_event_id", event.getId());
		payload.set("source_message_id", event.getMqttMessageId());
		payload.set("device_event_id", event.getDeviceEventId());
		outbox.setPayloadJson(payload.toString());
		outbox.setStatus("PENDING");
		outbox.setRetryCount(0);
		outbox.setNextRetryTime(now);
		outbox.setCreateTime(now);
		outbox.setUpdateTime(now);
		if (outboxMapper.insert(outbox) != 1) {
			throw new ServiceException("事件上报任务写入失败");
		}
	}

	private JSONObject buildPayload(EventManagement event, DeviceInfo device, ActiveSafetyEventReport report) {
		String description = StringUtils.defaultIfBlank(event.getHandleResult(), event.getEventDesc());
		JSONObject point = new JSONObject();
		point.set("address", StringUtils.defaultIfBlank(event.getReportLocation(), device.getAddress()));
		point.set("coord_system_type", null);
		point.set("coord_system_type_change", null);
		point.set("lat", report == null ? device.getLatitude() : report.getLatitude());
		point.set("lat_change", null);
		point.set("lng", report == null ? device.getLongitude() : report.getLongitude());
		point.set("lng_change", null);

		JSONObject payload = new JSONObject();
		payload.set("describe", description);
		payload.set("device_id", event.getReportDevice());
		payload.set("device_name", StringUtils.defaultIfBlank(report == null ? device.getDeviceName()
			: report.getDeviceName(), event.getReportDevice()));
		payload.set("device_tag", StringUtils.defaultString(report == null ? device.getTag() : report.getDeviceTag()));
		payload.set("device_tenant_id", event.getTenantId());
		payload.set("name", StringUtils.defaultIfBlank(event.getEventType(), description));
		// Without item the location service treats the entire description as its event type.
		payload.set("item", event.getEventType());
		payload.set("pics", report == null ? (StringUtils.isBlank(event.getReportImg())
			? Collections.emptyList() : Collections.singletonList(event.getReportImg()))
			: mediaReferences(report.getImageMediaIds()));
		payload.set("point", point);
		payload.set("video", report == null ? Collections.emptyList() : mediaReferences(report.getVideoMediaIds()));
		payload.set("source_event_id", event.getId());
		payload.set("source_message_id", event.getMqttMessageId());
		payload.set("device_event_id", event.getDeviceEventId());
		return payload;
	}

	private List<String> mediaReferences(List<String> mediaIds) {
		return mediaIds == null ? Collections.emptyList() : mediaIds.stream()
			.filter(StringUtils::isNotBlank).map(VlsEventReportApplicationService::mediaReference)
			.collect(Collectors.toList());
	}

	public static String mediaReference(String mediaId) {
		return MEDIA_REFERENCE_PREFIX + mediaId;
	}
}
