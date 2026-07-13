/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.ContainerInstance;
import com.ruoyi.vlstream.service.IVlsContainerInstanceService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsContainerInstanceControllerTest {

    @Test
    void exposesFrontendContainerInstanceRoutes() throws Exception {
        Method page = VlsContainerInstanceController.class.getDeclaredMethod("getContainerInstancePage", Long.class, Long.class, String.class, String.class, Long.class, String.class, String.class, String.class);
        Method detail = VlsContainerInstanceController.class.getDeclaredMethod("getContainerInstanceById", Long.class);
        Method create = VlsContainerInstanceController.class.getDeclaredMethod("createContainerInstance", ContainerInstance.class);
        Method update = VlsContainerInstanceController.class.getDeclaredMethod("updateContainerInstance", ContainerInstance.class);
        Method delete = VlsContainerInstanceController.class.getDeclaredMethod("deleteContainerInstance", Long.class);
        Method batchDelete = VlsContainerInstanceController.class.getDeclaredMethod("batchDeleteContainerInstances", Map.class);
        Method start = VlsContainerInstanceController.class.getDeclaredMethod("startContainerInstance", Long.class);
        Method stop = VlsContainerInstanceController.class.getDeclaredMethod("stopContainerInstance", Long.class);
        Method restart = VlsContainerInstanceController.class.getDeclaredMethod("restartContainerInstance", Long.class);
        Method statistics = VlsContainerInstanceController.class.getDeclaredMethod("getContainerInstanceStatistics");
        Method running = VlsContainerInstanceController.class.getDeclaredMethod("getRunningContainerInstances");
        Method error = VlsContainerInstanceController.class.getDeclaredMethod("getErrorContainerInstances");
        Method unhealthy = VlsContainerInstanceController.class.getDeclaredMethod("getUnhealthyContainerInstances");
        Method checkName = VlsContainerInstanceController.class.getDeclaredMethod("checkContainerInstanceName", String.class, Long.class);
        Method byAlgorithm = VlsContainerInstanceController.class.getDeclaredMethod("getContainerInstancesByAlgorithm", Long.class);
        Method monitoring = VlsContainerInstanceController.class.getDeclaredMethod("updateContainerInstanceMonitoring", Long.class, Map.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, detail.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {""}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {""}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batchDelete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/start"}, start.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/stop"}, stop.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/restart"}, restart.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/statistics"}, statistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/running"}, running.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/error"}, error.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/unhealthy"}, unhealthy.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/check-name"}, checkName.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/algorithm/{algorithmId}"}, byAlgorithm.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/monitoring"}, monitoring.getAnnotation(PutMapping.class).value());
    }

    @Test
    void pageAndDetailReturnBladeEnvelopeWithFrontendAliases() {
        IVlsContainerInstanceService service = mock(IVlsContainerInstanceService.class);
        VlsContainerInstanceController controller = new VlsContainerInstanceController(service);
        ContainerInstance instance = instance(7L, "edge-1", "running");
        BladePage<ContainerInstance> page = BladePage.of(Collections.singletonList(instance), 1L, 20L, 1L);

        when(service.getContainerInstancePage(1L, 20L, "edge", "running", 9L, "healthy", "2026-01-01", "2026-01-31")).thenReturn(page);
        when(service.getContainerInstanceById(7L)).thenReturn(instance);

        BladeResult<BladePage<ContainerInstance>> pageResult = controller.getContainerInstancePage(1L, 20L, "edge", "running", 9L, "healthy", "2026-01-01", "2026-01-31");
        BladeResult<ContainerInstance> detailResult = controller.getContainerInstanceById(7L);

        assertEquals(200, pageResult.getCode());
        assertSame(page, pageResult.getData());
        assertSame(instance, detailResult.getData());
    }

    @Test
    void createAndUpdateForwardFrontendAliases() {
        IVlsContainerInstanceService service = mock(IVlsContainerInstanceService.class);
        VlsContainerInstanceController controller = new VlsContainerInstanceController(service);
        ContainerInstance create = instance(null, "edge-1", "stopped");
        create.setImage("vlstream/edge:v1");
        create.setPortMappings("{\"port\":\"8080\"}");
        create.setEnvVariables("{\"MODE\":\"edge\"}");
        ContainerInstance updated = instance(7L, "edge-2", "running");

        when(service.createContainerInstance(create)).thenReturn(create);
        when(service.updateContainerInstance(updated)).thenReturn(updated);

        assertSame(create, controller.createContainerInstance(create).getData());
        assertSame(updated, controller.updateContainerInstance(updated).getData());
        assertEquals("edge-1", create.getInstanceName());
        assertEquals("vlstream/edge", create.getImageName());
        assertEquals("v1", create.getImageTag());
        assertEquals("{\"port\":\"8080\"}", create.getPortConfig());
        assertEquals("{\"MODE\":\"edge\"}", create.getEnvConfig());
        verify(service).createContainerInstance(create);
        verify(service).updateContainerInstance(updated);
    }

    @Test
    void actionRoutesForwardIdsAndBatchBody() {
        IVlsContainerInstanceService service = mock(IVlsContainerInstanceService.class);
        VlsContainerInstanceController controller = new VlsContainerInstanceController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("ids", Arrays.asList("7", 8L));

        when(service.deleteContainerInstance(7L)).thenReturn(true);
        when(service.batchDeleteContainerInstances(Arrays.asList(7L, 8L))).thenReturn(true);
        when(service.startContainerInstance(7L)).thenReturn(true);
        when(service.stopContainerInstance(7L)).thenReturn(true);
        when(service.restartContainerInstance(7L)).thenReturn(true);

        assertEquals(Boolean.TRUE, controller.deleteContainerInstance(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteContainerInstances(body).getData());
        assertEquals(Boolean.TRUE, controller.startContainerInstance(7L).getData());
        assertEquals(Boolean.TRUE, controller.stopContainerInstance(7L).getData());
        assertEquals(Boolean.TRUE, controller.restartContainerInstance(7L).getData());
        verify(service).batchDeleteContainerInstances(Arrays.asList(7L, 8L));
    }

    @Test
    void listAndMonitoringRoutesForwardParameters() {
        IVlsContainerInstanceService service = mock(IVlsContainerInstanceService.class);
        VlsContainerInstanceController controller = new VlsContainerInstanceController(service);
        List<ContainerInstance> instances = Collections.singletonList(instance(7L, "edge-1", "running"));
        Map<String, Object> statistics = new HashMap<String, Object>();
        statistics.put("totalInstances", 1L);
        Map<String, Object> monitoring = new HashMap<String, Object>();
        monitoring.put("cpuUsage", "12.50");
        monitoring.put("memoryUsage", 33.25);
        monitoring.put("gpuUsage", BigDecimal.valueOf(44.75));

        when(service.getContainerInstanceStatistics()).thenReturn(statistics);
        when(service.getRunningContainerInstances()).thenReturn(instances);
        when(service.getErrorContainerInstances()).thenReturn(instances);
        when(service.getUnhealthyContainerInstances()).thenReturn(instances);
        when(service.checkContainerInstanceName("edge-1", 7L)).thenReturn(true);
        when(service.getContainerInstancesByAlgorithm(9L)).thenReturn(instances);
        when(service.updateContainerInstanceMonitoring(eq(7L), eq(BigDecimal.valueOf(12.50)), eq(BigDecimal.valueOf(33.25)), eq(BigDecimal.valueOf(44.75)))).thenReturn(true);

        assertSame(statistics, controller.getContainerInstanceStatistics().getData());
        assertSame(instances, controller.getRunningContainerInstances().getData());
        assertSame(instances, controller.getErrorContainerInstances().getData());
        assertSame(instances, controller.getUnhealthyContainerInstances().getData());
        assertEquals(Boolean.TRUE, controller.checkContainerInstanceName("edge-1", 7L).getData());
        assertSame(instances, controller.getContainerInstancesByAlgorithm(9L).getData());
        assertEquals(Boolean.TRUE, controller.updateContainerInstanceMonitoring(7L, monitoring).getData());

        ArgumentCaptor<BigDecimal> cpuCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(service).updateContainerInstanceMonitoring(eq(7L), cpuCaptor.capture(), eq(BigDecimal.valueOf(33.25)), eq(BigDecimal.valueOf(44.75)));
        assertEquals(0, BigDecimal.valueOf(12.50).compareTo(cpuCaptor.getValue()));
    }

    private ContainerInstance instance(Long id, String name, String status) {
        ContainerInstance instance = new ContainerInstance();
        instance.setId(id);
        instance.setName(name);
        instance.setStatus(status);
        instance.setCpuLimit("2 cores");
        instance.setMemoryLimit("4GB");
        instance.setGpuLimit("1GB");
        return instance;
    }
}
