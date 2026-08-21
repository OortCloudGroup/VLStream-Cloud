package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsTrainingContainerProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsContainerInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class GpuTrainingSchedulerServiceTest {

	@AfterEach
	void clearTenant() {
		TenantContextHolder.clear();
	}

	@Test
	void schedulerQueriesExplicitlyIgnoreTenantInterceptor() throws Exception {
		assertTenantInterceptorIgnored("selectActiveTrainingForScheduler");
		assertTenantInterceptorIgnored("selectNextQueuedTrainingForScheduler");
	}

	@Test
	void skipsSshProbeWhenThereIsNoActiveOrQueuedTraining() throws Exception {
		VlsContainerInstanceMapper mapper = mock(VlsContainerInstanceMapper.class);
		SSHService sshService = mock(SSHService.class);
		when(mapper.selectActiveTrainingForScheduler()).thenReturn(Collections.emptyList());
		when(mapper.selectNextQueuedTrainingForScheduler()).thenReturn(null);

		GpuTrainingSchedulerService scheduler = new GpuTrainingSchedulerService();
		setField(scheduler, "containerInstanceMapper", mapper);
		setField(scheduler, "sshService", sshService);

		invokeSchedule(scheduler);

		verify(mapper).selectNextQueuedTrainingForScheduler();
		verify(sshService, never()).executeCommand(anyString(), anyInt(), anyString(), anyString(), anyString());
	}

	@Test
	void reconcilesActiveTrainingInsideItsTenantAndRestoresPreviousTenant() throws Exception {
		VlsContainerInstanceMapper mapper = mock(VlsContainerInstanceMapper.class);
		IVlsContainerInstanceService containerService = mock(IVlsContainerInstanceService.class);
		SSHService sshService = mock(SSHService.class);
		VlsSshProperties sshProperties = new VlsSshProperties();
		VlsTrainingContainerProperties trainingProperties = new VlsTrainingContainerProperties();

		ContainerInstance active = new ContainerInstance();
		active.setId(10L);
		active.setTenantId("tenant-a");
		active.setInstanceName("vls-training-10");
		active.setInstanceStatus("running");
		when(mapper.selectActiveTrainingForScheduler()).thenReturn(Collections.singletonList(active));
		when(sshService.executeCommand(anyString(), anyInt(), anyString(), anyString(), anyString()))
			.thenAnswer(invocation -> {
				String command = invocation.getArgument(4);
				return command.contains("docker inspect")
					? sshResult("running|0|container-10") : sshResult("15");
			});

		AtomicReference<String> updateTenant = new AtomicReference<String>();
		when(containerService.updateById(any(ContainerInstance.class))).thenAnswer(invocation -> {
			updateTenant.set(TenantContextHolder.getTenantId());
			return true;
		});
		AtomicReference<String> monitoringTenant = new AtomicReference<String>();
		when(containerService.updateMonitoringData(any(), any(), any(), any())).thenAnswer(invocation -> {
			monitoringTenant.set(TenantContextHolder.getTenantId());
			return true;
		});

		GpuTrainingSchedulerService scheduler = new GpuTrainingSchedulerService();
		setField(scheduler, "containerInstanceMapper", mapper);
		setField(scheduler, "containerInstanceService", containerService);
		setField(scheduler, "sshService", sshService);
		setField(scheduler, "sshProperties", sshProperties);
		setField(scheduler, "properties", trainingProperties);
		TenantContextHolder.setTenantId("outer-tenant");

		invokeSchedule(scheduler);

		assertEquals("tenant-a", updateTenant.get());
		assertEquals("tenant-a", monitoringTenant.get());
		assertEquals("outer-tenant", TenantContextHolder.getTenantId());
		verify(mapper, never()).selectNextQueuedTrainingForScheduler();
	}

	private SSHService.SSHExecutionResult sshResult(String output) {
		SSHService.SSHExecutionResult result = new SSHService.SSHExecutionResult();
		result.setSuccess(true);
		result.setOutput(output);
		result.setErrorMsg("");
		return result;
	}

	private void assertTenantInterceptorIgnored(String methodName) throws Exception {
		InterceptorIgnore annotation = VlsContainerInstanceMapper.class.getMethod(methodName)
			.getAnnotation(InterceptorIgnore.class);
		assertNotNull(annotation);
		assertEquals("true", annotation.tenantLine());
	}

	private void invokeSchedule(GpuTrainingSchedulerService scheduler) throws Exception {
		Method method = GpuTrainingSchedulerService.class.getDeclaredMethod("scheduleSafely");
		method.setAccessible(true);
		method.invoke(scheduler);
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
