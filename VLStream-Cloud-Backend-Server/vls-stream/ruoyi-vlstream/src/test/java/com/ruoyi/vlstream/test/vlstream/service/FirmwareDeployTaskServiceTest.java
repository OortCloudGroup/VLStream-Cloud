package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.vlstream.test.vlstream.config.VlsFirmwareProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.FirmwareDeployTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployTaskView;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.FirmwareDeployTask;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class FirmwareDeployTaskServiceTest {

	private FirmwareDeployTaskMapper mapper;
	private FirmwareDeployTaskService service;
	private FirmwareDeployTask task;

	@BeforeEach
	void setUp() {
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
			FirmwareDeployTask.class);
		mapper = mock(FirmwareDeployTaskMapper.class);
		VlsFirmwareProperties properties = new VlsFirmwareProperties();
		properties.setOtaTaskInactivityTimeoutMinutes(30);
		service = new FirmwareDeployTaskService(mapper, properties);
		task = new FirmwareDeployTask();
		task.setDeviceRowId(10L);
		task.setRequestId("request-1");
		task.setMqttMessageId("message-1");
		task.setDeviceId("CAM-1");
		task.setDeviceModel("OORT-6600-2.5");
		task.setTarget("rootfs");
		task.setTargetVersion("2.0.0");
		task.setSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		task.setDeployStatus("INSTALLING");
		when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
	}

	@Test
	void rejectsReplyForAnotherOutboundMessage() {
		boolean applied = service.applyHardwareReply("other", "request-1", "CAM-1", "OORT-6600-2.5",
			"rootfs", "2.0.0", task.getSha256(), "SUCCESS", "done", "{}");

		assertFalse(applied);
		verify(mapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void rejectsReplyForAnotherDeviceModel() {
		boolean applied = service.applyHardwareReply("message-1", "request-1", "CAM-1", "OTHER-MODEL",
			"rootfs", "2.0.0", task.getSha256(), "SUCCESS", "done", "{}");

		assertFalse(applied);
		verify(mapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void convertsSuccessfulReplyWithWrongHashToFailure() {
		when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
		ArgumentCaptor<LambdaUpdateWrapper<FirmwareDeployTask>> updateCaptor =
			ArgumentCaptor.forClass(LambdaUpdateWrapper.class);

		boolean applied = service.applyHardwareReply("message-1", "request-1", "CAM-1", "OORT-6600-2.5",
			"rootfs", "2.0.0",
			"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
			"SUCCESS", "done", "{}");

		assertTrue(applied);
		verify(mapper).update(isNull(), updateCaptor.capture());
		assertTrue(updateCaptor.getValue().getParamNameValuePairs().containsValue("FAILED"));
	}

	@Test
	void cancellingActiveTaskReleasesItsPlatformLock() {
		when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		FirmwareDeployTaskView view = service.cancelActiveTask(10L, "request-1");

		assertEquals("CANCELLED", view.getDeployStatus());
		assertTrue(view.getFailureReason().contains("未向设备发送取消指令"));
	}

	@Test
	void ignoresLateReplyAfterTaskWasCancelled() {
		task.setDeployStatus("CANCELLED");

		boolean applied = service.applyHardwareReply("message-1", "request-1", "CAM-1", "OORT-6600-2.5",
			"rootfs", "2.0.0", task.getSha256(), "SUCCESS", "done", "{}");

		assertTrue(applied);
		verify(mapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void expiresDownloadAndExecutionTasks() {
		when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		assertEquals(2, service.expireStaleTasks());
		verify(mapper, org.mockito.Mockito.times(2)).update(isNull(), any(LambdaUpdateWrapper.class));
	}
}
