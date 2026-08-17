package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.vlstream.test.vlstream.config.VlsNativeDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMessageMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceStreamMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDeviceStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Persists the full device/state snapshot reported by native VLStream hardware. */
@Service
@RequiredArgsConstructor
public class MqttDeviceStateService {

	private final MqttDeviceMapper deviceMapper;
	private final MqttDeviceStreamMapper streamMapper;
	private final MqttDeviceMessageMapper messageMapper;
	private final VlsNativeDeviceProperties properties;

	@Transactional(rollbackFor = Exception.class)
	public synchronized JSONObject handle(JSONObject message) {
		String deviceId = StringUtils.trimToEmpty(message.getStr("deviceId"));
		String messageId = StringUtils.trimToEmpty(message.getStr("messageId"));
		if (StringUtils.isAnyBlank(deviceId, messageId)) {
			return reply(message, 400, "deviceId和messageId不能为空");
		}
		JSONObject payload = message.getJSONObject("payload");
		if (payload == null) {
			return reply(message, 400, "payload不能为空");
		}

		String tenantId = StringUtils.defaultIfBlank(properties.getDefaultTenantId(), "000000");
		Date receivedAt = new Date();
		Date reportedAt = parseDate(message.getStr("sentAt"), receivedAt);
		if (messageMapper.insertIgnore(tenantId, deviceId, messageId, reportedAt, receivedAt) == 0) {
			return reply(message, 200, "重复消息已确认");
		}
		MqttDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<MqttDevice>()
			.eq(MqttDevice::getTenantId, tenantId)
			.eq(MqttDevice::getDeviceId, deviceId)
			.eq(MqttDevice::getIsDeleted, 0));
		if (device != null && device.getLastReportedAt() != null
			&& reportedAt.before(device.getLastReportedAt())) {
			return reply(message, 200, "过期状态快照已忽略");
		}
		if (device == null) {
			device = new MqttDevice();
			device.setTenantId(tenantId);
			device.setDeviceId(deviceId);
			device.setStatus(1);
			device.setIsDeleted(0);
			device.setCreateTime(receivedAt);
		}
		device.setDeviceName(payload.getStr("deviceName"));
		device.setDeviceSerial(payload.getStr("deviceSerial"));
		String deviceModel = StringUtils.trim(payload.getStr("deviceModel"));
		if (StringUtils.isNotBlank(deviceModel)) {
			device.setDeviceModel(deviceModel);
		}
		JSONObject firmwareVersions = payload.getJSONObject("firmwareVersions");
		String applicationVersion = firmwareVersions == null
			? null : StringUtils.trim(firmwareVersions.getStr("application"));
		String rootfsVersion = firmwareVersions == null
			? null : StringUtils.trim(firmwareVersions.getStr("rootfs"));
		if (StringUtils.isBlank(applicationVersion)) {
			applicationVersion = StringUtils.trim(payload.getStr("version"));
		}
		if (StringUtils.isNotBlank(applicationVersion)) {
			device.setApplicationVersion(applicationVersion);
			device.setFirmwareVersion(applicationVersion);
		}
		if (StringUtils.isNotBlank(rootfsVersion)) {
			device.setRootfsVersion(rootfsVersion);
		}
		device.setFaceVersion(payload.getStr("deviceFaceVer"));
		device.setIpAddr(payload.getStr("ipAddr"));
		device.setMac(payload.getStr("mac"));
		device.setOnline(payload.getBool("online", Boolean.FALSE));
		device.setOnlineReason(payload.getStr("reason"));
		device.setHeartbeatIndex(payload.getLong("heartbeatIndex"));
		device.setLastMessageId(messageId);
		device.setLastReportedAt(reportedAt);
		device.setLastHeartbeatTime(receivedAt);
		device.setTelemetryJson(jsonString(payload.get("telemetry")));
		device.setServiceStatusJson(jsonString(payload.get("serviceStatus")));
		device.setUpdateTime(receivedAt);
		if (device.getId() == null) {
			deviceMapper.insert(device);
		} else {
			deviceMapper.updateById(device);
		}

		synchronizeStreams(device, payload.getJSONArray("streams"), receivedAt);
		return reply(message, 200, "状态已接收");
	}

	private void synchronizeStreams(MqttDevice device, JSONArray streams, Date receivedAt) {
		Set<String> snapshotKeys = new HashSet<>();
		boolean defaultAssigned = false;
		streamMapper.update(null, new LambdaUpdateWrapper<MqttDeviceStream>()
			.eq(MqttDeviceStream::getDeviceRowId, device.getId())
			.set(MqttDeviceStream::getAvailable, false)
			.set(MqttDeviceStream::getUpdateTime, receivedAt));
		if (streams != null) {
			for (Object item : streams) {
				JSONObject streamJson = item instanceof JSONObject ? (JSONObject) item : JSONUtil.parseObj(item);
				String channelId = StringUtils.trimToEmpty(streamJson.getStr("channelId"));
				String streamType = StringUtils.defaultIfBlank(streamJson.getStr("streamType"), "main");
				String protocol = StringUtils.lowerCase(StringUtils.trimToEmpty(streamJson.getStr("protocol")));
				String sourceUrl = StringUtils.trimToEmpty(streamJson.getStr("url"));
				if (StringUtils.isAnyBlank(channelId, sourceUrl)
					|| !(StringUtils.equals(protocol, "rtsp") || StringUtils.equals(protocol, "rtmp"))) {
					continue;
				}
				String key = channelId + "\n" + streamType;
				if (!snapshotKeys.add(key)) {
					continue;
				}
				MqttDeviceStream stream = streamMapper.selectOne(new LambdaQueryWrapper<MqttDeviceStream>()
					.eq(MqttDeviceStream::getDeviceRowId, device.getId())
					.eq(MqttDeviceStream::getChannelId, channelId)
					.eq(MqttDeviceStream::getStreamType, streamType));
				if (stream == null) {
					stream = new MqttDeviceStream();
					stream.setTenantId(device.getTenantId());
					stream.setDeviceRowId(device.getId());
					stream.setChannelId(channelId);
					stream.setStreamType(streamType);
					stream.setStatus(1);
					stream.setIsDeleted(0);
					stream.setCreateTime(receivedAt);
				}
				stream.setStreamName(streamJson.getStr("name"));
				stream.setProtocol(protocol);
				stream.setSourceUrl(sourceUrl);
				boolean requestedDefault = streamJson.getBool("default", Boolean.FALSE);
				stream.setIsDefault(requestedDefault && !defaultAssigned);
				defaultAssigned = defaultAssigned || requestedDefault;
				stream.setAvailable(streamJson.getBool("available", Boolean.TRUE));
				stream.setLastReportTime(receivedAt);
				stream.setUpdateTime(receivedAt);
				if (stream.getId() == null) {
					streamMapper.insert(stream);
				} else {
					streamMapper.updateById(stream);
				}
			}
		}
	}

	private JSONObject reply(JSONObject source, int code, String message) {
		JSONObject payload = new JSONObject();
		payload.set("sourceMsgId", source.getStr("messageId"));
		payload.set("code", code);
		payload.set("msg", message);
		payload.set("errCode", code == 200 ? 0 : 1001);
		payload.set("errDetail", code == 200 ? "" : message);
		payload.set("bizData", new JSONObject());
		JSONObject reply = new JSONObject();
		reply.set("protocolVersion", VlsMqttProtocol.VERSION);
		reply.set("messageId", UUID.randomUUID().toString());
		reply.set("msgDir", VlsMqttProtocol.PLATFORM_TO_DEVICE);
		reply.set("deviceId", source.getStr("deviceId"));
		reply.set("sentAt", Instant.now().toString());
		String sourceMainBizType = source.getStr("mainBizType");
		reply.set("mainBizType", VlsMqttProtocol.isDeviceBiz(sourceMainBizType)
			? sourceMainBizType : VlsMqttProtocol.DEVICE_BIZ);
		reply.set("subBizType", VlsMqttProtocol.STATE);
		reply.set("payload", payload);
		return reply;
	}

	private Date parseDate(String value, Date fallback) {
		try {
			return Date.from(Instant.parse(value));
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private String jsonString(Object value) {
		return value == null ? null : JSONUtil.toJsonStr(value);
	}
}
