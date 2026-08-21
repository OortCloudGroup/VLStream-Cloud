package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsNativeDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMessageMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceStreamMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDeviceStream;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class MqttDeviceStateServiceTest {

	@Test
	void autoRegistersDeviceAndFullStreamSnapshot() {
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MqttDevice.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MqttDeviceStream.class);
		MqttDeviceMapper deviceMapper = mock(MqttDeviceMapper.class);
		MqttDeviceStreamMapper streamMapper = mock(MqttDeviceStreamMapper.class);
		MqttDeviceMessageMapper messageMapper = mock(MqttDeviceMessageMapper.class);
		when(messageMapper.insertIgnore(any(), any(), any(), any(), any())).thenReturn(1);
		when(deviceMapper.selectOne(any())).thenReturn(null);
		when(deviceMapper.insert(any())).thenAnswer(invocation -> {
			MqttDevice device = invocation.getArgument(0);
			device.setId(101L);
			return 1;
		});
		when(streamMapper.selectOne(any())).thenReturn(null);
		VlsNativeDeviceProperties properties = new VlsNativeDeviceProperties();
		properties.setDefaultTenantId("000000");
		MqttDeviceStateService service = new MqttDeviceStateService(
			deviceMapper, streamMapper, messageMapper, properties, "single");

		JSONObject reply = service.handle(JSONUtil.parseObj("{\"protocolVersion\":\"2.2\",\"messageId\":\"m1\",\"deviceId\":\"CAM-1\",\"sentAt\":\"2026-08-12T06:00:00Z\",\"msgDir\":\"dev2platform\",\"mainBizType\":\"deviceBiz\",\"subBizType\":\"state\",\"payload\":{\"online\":true,\"deviceName\":\"camera\",\"deviceModel\":\"OORT-6600-2.5\",\"version\":\"2.0.0\",\"telemetry\":{\"cpu\":16,\"mem\":66,\"diskUsed\":11,\"diskTotalMB\":136},\"streams\":[{\"channelId\":\"1\",\"streamType\":\"main\",\"protocol\":\"rtsp\",\"url\":\"rtsp://camera/main\",\"default\":true},{\"channelId\":\"1\",\"streamType\":\"sub\",\"protocol\":\"rtsp\",\"url\":\"rtsp://camera/sub\",\"default\":true}]}}"));

		assertEquals("platform2dev", reply.getStr("msgDir"));
		assertEquals("deviceBiz", reply.getStr("mainBizType"));
		assertEquals(200, reply.getJSONObject("payload").getInt("code"));
		ArgumentCaptor<MqttDevice> deviceCaptor = ArgumentCaptor.forClass(MqttDevice.class);
		verify(deviceMapper).insert(deviceCaptor.capture());
		assertEquals("OORT-6600-2.5", deviceCaptor.getValue().getDeviceModel());
		assertEquals("2.0.0", deviceCaptor.getValue().getRootfsVersion());
		assertEquals("2.0.0", deviceCaptor.getValue().getFirmwareVersion());
		assertEquals(136, JSONUtil.parseObj(deviceCaptor.getValue().getTelemetryJson()).getInt("diskTotalMB"));
		verify(streamMapper, times(2)).insert(any());
		verify(streamMapper).update(isNull(), any());
		assertNull(TenantContextHolder.getTenantId());
	}

	@Test
	void multiTenantUsesExistingDeviceOwnership() {
		initTableInfo();
		MqttDeviceMapper deviceMapper = mock(MqttDeviceMapper.class);
		MqttDeviceStreamMapper streamMapper = mock(MqttDeviceStreamMapper.class);
		MqttDeviceMessageMapper messageMapper = mock(MqttDeviceMessageMapper.class);
		MqttDevice existing = new MqttDevice();
		existing.setId(201L);
		existing.setTenantId("tenant-a");
		existing.setDeviceId("CAM-2");
		existing.setIsDeleted(0);
		when(deviceMapper.selectTenantIdsByDeviceId("CAM-2"))
			.thenReturn(Collections.singletonList("tenant-a"));
		when(messageMapper.insertIgnore(any(), any(), any(), any(), any())).thenReturn(1);
		when(deviceMapper.selectOne(any())).thenReturn(existing);
		MqttDeviceStateService service = newService(
			deviceMapper, streamMapper, messageMapper, "multi");

		JSONObject reply = service.handle(stateMessage("m2", "CAM-2"));

		assertEquals(200, reply.getJSONObject("payload").getInt("code"));
		verify(messageMapper).insertIgnore(eq("tenant-a"), eq("CAM-2"), eq("m2"), any(), any());
		verify(deviceMapper).updateById(existing);
		assertNull(TenantContextHolder.getTenantId());
	}

	@Test
	void multiTenantFallsBackToConfiguredDefaultForUnknownDevice() {
		initTableInfo();
		MqttDeviceMapper deviceMapper = mock(MqttDeviceMapper.class);
		MqttDeviceStreamMapper streamMapper = mock(MqttDeviceStreamMapper.class);
		MqttDeviceMessageMapper messageMapper = mock(MqttDeviceMessageMapper.class);
		when(deviceMapper.selectTenantIdsByDeviceId("CAM-3")).thenReturn(Collections.emptyList());
		when(messageMapper.insertIgnore(any(), any(), any(), any(), any())).thenReturn(1);
		when(deviceMapper.selectOne(any())).thenReturn(null);
		when(deviceMapper.insert(any())).thenAnswer(invocation -> {
			MqttDevice device = invocation.getArgument(0);
			device.setId(301L);
			return 1;
		});
		MqttDeviceStateService service = newService(
			deviceMapper, streamMapper, messageMapper, "multi");

		JSONObject reply = service.handle(stateMessage("m3", "CAM-3"));

		assertEquals(200, reply.getJSONObject("payload").getInt("code"));
		String defaultTenantId = "0e391fd7-1033-4f09-88c0-187582fee462";
		verify(messageMapper).insertIgnore(eq(defaultTenantId), eq("CAM-3"), eq("m3"), any(), any());
		ArgumentCaptor<MqttDevice> deviceCaptor = ArgumentCaptor.forClass(MqttDevice.class);
		verify(deviceMapper).insert(deviceCaptor.capture());
		assertEquals(defaultTenantId, deviceCaptor.getValue().getTenantId());
		assertNull(TenantContextHolder.getTenantId());
	}

	@Test
	void multiTenantRejectsAmbiguousDeviceOwnership() {
		initTableInfo();
		MqttDeviceMapper deviceMapper = mock(MqttDeviceMapper.class);
		when(deviceMapper.selectTenantIdsByDeviceId("CAM-4"))
			.thenReturn(Arrays.asList("tenant-a", "tenant-b"));
		MqttDeviceStateService service = newService(deviceMapper,
			mock(MqttDeviceStreamMapper.class), mock(MqttDeviceMessageMapper.class), "multi");

		assertThrows(ServiceException.class, () -> service.handle(stateMessage("m4", "CAM-4")));
		assertNull(TenantContextHolder.getTenantId());
	}

	private MqttDeviceStateService newService(MqttDeviceMapper deviceMapper,
											 MqttDeviceStreamMapper streamMapper,
											 MqttDeviceMessageMapper messageMapper,
											 String tenantType) {
		VlsNativeDeviceProperties properties = new VlsNativeDeviceProperties();
		properties.setDefaultTenantId("000000");
		properties.setMultiTenantDefaultTenantId("0e391fd7-1033-4f09-88c0-187582fee462");
		return new MqttDeviceStateService(
			deviceMapper, streamMapper, messageMapper, properties, tenantType);
	}

	private void initTableInfo() {
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MqttDevice.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), MqttDeviceStream.class);
	}

	private JSONObject stateMessage(String messageId, String deviceId) {
		return JSONUtil.parseObj("{\"protocolVersion\":\"2.2\",\"messageId\":\"" + messageId
			+ "\",\"deviceId\":\"" + deviceId
			+ "\",\"sentAt\":\"2026-08-12T06:00:00Z\",\"msgDir\":\"dev2platform\","
			+ "\"mainBizType\":\"deviceBiz\",\"subBizType\":\"state\","
			+ "\"payload\":{\"online\":true,\"deviceName\":\"camera\"}}");
	}
}
