/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.service.IVlsDeviceInfoService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsDeviceInfoControllerTest {

    @Test
    void exposesFrontendDeviceInfoRoutes() throws Exception {
        Method page = VlsDeviceInfoController.class.getDeclaredMethod(
            "page", Long.class, Long.class, Long.class, String.class, String.class, String.class, String.class);
        Method getDevice = VlsDeviceInfoController.class.getDeclaredMethod("getDevice", Long.class);
        Method createDevice = VlsDeviceInfoController.class.getDeclaredMethod("createDevice", DeviceInfo.class);
        Method updateDevice = VlsDeviceInfoController.class.getDeclaredMethod("updateDevice", Long.class, DeviceInfo.class);
        Method deleteDevice = VlsDeviceInfoController.class.getDeclaredMethod("deleteDevice", Long.class);
        Method deleteDevices = VlsDeviceInfoController.class.getDeclaredMethod("deleteDevices", List.class);
        Method statistics = VlsDeviceInfoController.class.getDeclaredMethod("statistics");
        Method tree = VlsDeviceInfoController.class.getDeclaredMethod("tree");
        Method testConnection = VlsDeviceInfoController.class.getDeclaredMethod("testConnection", Long.class);
        Method refresh = VlsDeviceInfoController.class.getDeclaredMethod("refresh", Long.class);
        Method batchRefresh = VlsDeviceInfoController.class.getDeclaredMethod("batchRefresh", Map.class);
        Method typeStatistics = VlsDeviceInfoController.class.getDeclaredMethod("typeStatistics");
        Method tags = VlsDeviceInfoController.class.getDeclaredMethod("tags");
        Method ptzMove = VlsDeviceInfoController.class.getDeclaredMethod("ptzMove", Long.class, Map.class);
        Method ptzStop = VlsDeviceInfoController.class.getDeclaredMethod("ptzStop", Long.class);
        Method ptzZoom = VlsDeviceInfoController.class.getDeclaredMethod("ptzZoom", Long.class, Map.class);
        Method stream = VlsDeviceInfoController.class.getDeclaredMethod("stream", Long.class);
        Method export = VlsDeviceInfoController.class.getDeclaredMethod("export", String.class);
        Method importDevices = VlsDeviceInfoController.class.getDeclaredMethod("importDevices", org.springframework.web.multipart.MultipartFile.class);
        Method dispatchAlgorithms = VlsDeviceInfoController.class.getDeclaredMethod("dispatchAlgorithms", Long.class, String.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, getDevice.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, createDevice.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, updateDevice.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, deleteDevice.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, deleteDevices.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/statistics"}, statistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/tree"}, tree.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/test"}, testConnection.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/refresh"}, refresh.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/batch/refresh"}, batchRefresh.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/type-statistics"}, typeStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/tags"}, tags.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/ptz/move"}, ptzMove.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/ptz/stop"}, ptzStop.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/ptz/zoom"}, ptzZoom.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/stream"}, stream.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/export"}, export.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/import"}, importDevices.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{algorithmId}/algorithms"}, dispatchAlgorithms.getAnnotation(PostMapping.class).value());
    }

    @Test
    void pageReturnsBladePageEnvelopeAndAcceptsPageAlias() {
        IVlsDeviceInfoService service = mock(IVlsDeviceInfoService.class);
        VlsDeviceInfoController controller = new VlsDeviceInfoController(service);
        DeviceInfo device = new DeviceInfo();
        device.setId(7L);
        device.setDeviceName("camera-7");
        BladePage<DeviceInfo> page = BladePage.of(Arrays.asList(device), 1L, 20L, 2L);

        when(service.getDevicePage(2L, 20L, "cam", null, "1", "PTZ")).thenReturn(page);

        BladeResult<BladePage<DeviceInfo>> result = controller.page(null, 2L, 20L, "cam", null, "1", "PTZ");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        assertEquals("camera-7", result.getData().getRecords().get(0).getDeviceName());
        verify(service).getDevicePage(2L, 20L, "cam", null, "1", "PTZ");
    }

    @Test
    void batchRefreshReadsFrontendIdsBody() {
        IVlsDeviceInfoService service = mock(IVlsDeviceInfoService.class);
        VlsDeviceInfoController controller = new VlsDeviceInfoController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("ids", Arrays.asList(1, "2", 3L));
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("success", 3);

        when(service.batchRefreshDevices(Arrays.asList(1L, 2L, 3L))).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.batchRefresh(body);

        assertEquals(200, result.getCode());
        assertEquals(serviceResult, result.getData());
        verify(service).batchRefreshDevices(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void ptzMoveForwardsFrontendDirectionAndSpeedBody() {
        IVlsDeviceInfoService service = mock(IVlsDeviceInfoService.class);
        VlsDeviceInfoController controller = new VlsDeviceInfoController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("direction", "up");
        body.put("speed", 4);
        Map<String, Object> serviceResult = new HashMap<String, Object>();
        serviceResult.put("command", "move");

        when(service.ptzControl(9L, "move", body)).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.ptzMove(9L, body);

        assertEquals(200, result.getCode());
        assertEquals(serviceResult, result.getData());
        verify(service).ptzControl(9L, "move", body);
    }

    @Test
    void dispatchUsesAlgorithmPathAndDeviceIdsParam() {
        IVlsDeviceInfoService service = mock(IVlsDeviceInfoService.class);
        VlsDeviceInfoController controller = new VlsDeviceInfoController(service);
        Map<String, Object> dispatchResult = new HashMap<String, Object>();
        dispatchResult.put("algorithmId", 5L);
        dispatchResult.put("deviceIds", Arrays.asList(1L, 2L));

        when(service.dispatchAlgorithms(5L, "1,2")).thenReturn(dispatchResult);

        BladeResult<Map<String, Object>> result = controller.dispatchAlgorithms(5L, "1,2");

        assertEquals(200, result.getCode());
        assertEquals(dispatchResult, result.getData());
        verify(service).dispatchAlgorithms(5L, "1,2");
    }
}
