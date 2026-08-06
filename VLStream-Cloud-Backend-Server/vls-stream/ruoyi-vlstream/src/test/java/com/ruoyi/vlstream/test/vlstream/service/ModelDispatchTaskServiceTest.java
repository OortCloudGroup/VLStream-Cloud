package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsModelDispatchTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class ModelDispatchTaskServiceTest {

	private VlsModelDispatchTaskMapper taskMapper;
	private ModelDispatchTaskService taskService;
	private ModelDispatchTask task;

	@BeforeEach
	void setUp() throws Exception {
		TableInfoHelper.initTableInfo(
			new MapperBuilderAssistant(new MybatisConfiguration(), ""),
			ModelDispatchTask.class);
		taskMapper = mock(VlsModelDispatchTaskMapper.class);
		taskService = new ModelDispatchTaskService();
		setField(taskService, "taskMapper", taskMapper);

		task = new ModelDispatchTask();
		task.setRequestId("request-1");
		task.setMqttMessageId("mqtt-1");
		task.setDeviceId("CAM-1");
		task.setSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		task.setDispatchStatus("DEPLOYING");
		when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
	}

	@Test
	void rejectsReplyWhoseMqttMessageIdDoesNotMatchTheTask() {
		boolean applied = taskService.applyHardwareReply(
			"other-message", "request-1", "CAM-1", "SUCCESS",
			task.getSha256(), "done", "{}");

		assertFalse(applied);
		verify(taskMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void convertsSuccessWithWrongDeviceHashToFailed() {
		when(taskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
		ArgumentCaptor<LambdaUpdateWrapper<ModelDispatchTask>> updateCaptor =
			ArgumentCaptor.forClass(LambdaUpdateWrapper.class);

		boolean applied = taskService.applyHardwareReply(
			"mqtt-1", "request-1", "CAM-1", "SUCCESS",
			"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
			"done", "{}");

		assertTrue(applied);
		verify(taskMapper).update(isNull(), updateCaptor.capture());
		assertTrue(updateCaptor.getValue().getParamNameValuePairs().containsValue("FAILED"));
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
