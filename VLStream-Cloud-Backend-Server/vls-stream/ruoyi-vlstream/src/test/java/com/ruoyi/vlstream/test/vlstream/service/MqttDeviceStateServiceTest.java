package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
		MqttDeviceStateService service = new MqttDeviceStateService(deviceMapper, streamMapper, messageMapper, properties);

		JSONObject reply = service.handle(JSONUtil.parseObj("{\"protocolVersion\":\"2.2\",\"messageId\":\"m1\",\"deviceId\":\"CAM-1\",\"sentAt\":\"2026-08-12T06:00:00Z\",\"msgDir\":\"dev2platform\",\"mainBizType\":\"deviceBiz\",\"subBizType\":\"state\",\"payload\":{\"online\":true,\"deviceName\":\"camera\",\"deviceModel\":\"OORT-6600-2.5\",\"firmwareVersions\":{\"application\":\"1.0.1.14\",\"rootfs\":\"2.0.0\"},\"telemetry\":{\"cpu\":16,\"mem\":66,\"diskUsed\":11,\"diskTotalMB\":136},\"streams\":[{\"channelId\":\"1\",\"streamType\":\"main\",\"protocol\":\"rtsp\",\"url\":\"rtsp://camera/main\",\"default\":true},{\"channelId\":\"1\",\"streamType\":\"sub\",\"protocol\":\"rtsp\",\"url\":\"rtsp://camera/sub\",\"default\":true}]}}"));

		assertEquals("platform2dev", reply.getStr("msgDir"));
		assertEquals("deviceBiz", reply.getStr("mainBizType"));
		assertEquals(200, reply.getJSONObject("payload").getInt("code"));
		ArgumentCaptor<MqttDevice> deviceCaptor = ArgumentCaptor.forClass(MqttDevice.class);
		verify(deviceMapper).insert(deviceCaptor.capture());
		assertEquals("OORT-6600-2.5", deviceCaptor.getValue().getDeviceModel());
		assertEquals("1.0.1.14", deviceCaptor.getValue().getApplicationVersion());
		assertEquals("2.0.0", deviceCaptor.getValue().getRootfsVersion());
		assertEquals(136, JSONUtil.parseObj(deviceCaptor.getValue().getTelemetryJson()).getInt("diskTotalMB"));
		verify(streamMapper, times(2)).insert(any());
		verify(streamMapper).update(isNull(), any());
	}
}
