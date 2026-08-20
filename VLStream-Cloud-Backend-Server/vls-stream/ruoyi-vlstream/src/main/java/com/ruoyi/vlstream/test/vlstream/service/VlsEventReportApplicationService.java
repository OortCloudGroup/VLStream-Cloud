/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
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
		if (event == null || device == null || StringUtils.isBlank(device.getTenantId())) {
			throw new ServiceException("事件和设备租户信息不能为空");
		}
		event.setTenantId(device.getTenantId());
		event.setIsReport(isMultiTenant() ? 0 : 1);
		if (!eventManagementService.createEvent(event) || event.getId() == null) {
			throw new ServiceException("事件写入数据库失败");
		}
		if (isMultiTenant()) {
			enqueue(event, device);
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
		persistDeviceEvent(event, device);
		return true;
	}

	private void enqueue(EventManagement event, DeviceInfo device) {
		Date now = new Date();
		EventReportOutbox outbox = new EventReportOutbox();
		outbox.setTenantId(event.getTenantId());
		outbox.setEventId(event.getId());
		outbox.setIdempotencyKey("vls-event:" + event.getTenantId() + ":" + event.getId());
		outbox.setPayloadJson(buildPayload(event, device).toString());
		outbox.setStatus("PENDING");
		outbox.setRetryCount(0);
		outbox.setNextRetryTime(now);
		outbox.setCreateTime(now);
		outbox.setUpdateTime(now);
		if (outboxMapper.insert(outbox) != 1) {
			throw new ServiceException("事件上报任务写入失败");
		}
	}

	private JSONObject buildPayload(EventManagement event, DeviceInfo device) {
		String description = StringUtils.defaultIfBlank(event.getHandleResult(), event.getEventDesc());
		JSONObject point = new JSONObject();
		point.set("address", StringUtils.defaultString(device.getAddress()));
		point.set("coord_system_type", null);
		point.set("coord_system_type_change", null);
		point.set("lat", device.getLatitude());
		point.set("lat_change", null);
		point.set("lng", device.getLongitude());
		point.set("lng_change", null);

		JSONObject payload = new JSONObject();
		payload.set("describe", description);
		payload.set("device_id", event.getReportDevice());
		payload.set("device_name", StringUtils.defaultIfBlank(device.getDeviceName(), event.getReportDevice()));
		payload.set("device_tag", StringUtils.defaultString(device.getTag()));
		payload.set("device_tenant_id", event.getTenantId());
		payload.set("name", StringUtils.defaultIfBlank(event.getEventType(), description));
		payload.set("pics", StringUtils.isBlank(event.getReportImg())
			? Collections.emptyList() : Collections.singletonList(event.getReportImg()));
		payload.set("point", point);
		payload.set("video", Collections.emptyList());
		payload.set("source_event_id", event.getId());
		payload.set("source_message_id", event.getMqttMessageId());
		payload.set("device_event_id", event.getDeviceEventId());
		return payload;
	}

	public static String mediaReference(String mediaId) {
		return MEDIA_REFERENCE_PREFIX + mediaId;
	}
}
