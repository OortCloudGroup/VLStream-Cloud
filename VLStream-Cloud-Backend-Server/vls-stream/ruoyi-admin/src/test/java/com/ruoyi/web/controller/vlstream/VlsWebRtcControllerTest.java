/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.service.IVlsWebRtcService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsWebRtcControllerTest {

    @Test
    void exposesFrontendWebRtcRoutes() throws Exception {
        Method getConfig = VlsWebRtcController.class.getDeclaredMethod("getConfig");
        Method getStatus = VlsWebRtcController.class.getDeclaredMethod("getStatus");
        Method refresh = VlsWebRtcController.class.getDeclaredMethod("refresh");
        Method startWebRtcStream = VlsWebRtcController.class.getDeclaredMethod("startWebRtcStream", Map.class);
        Method stopWebRtcStream = VlsWebRtcController.class.getDeclaredMethod("stopWebRtcStream", Map.class);
        Method validateRtspStream = VlsWebRtcController.class.getDeclaredMethod("validateRtspStream", Map.class);
        Method getActiveStreams = VlsWebRtcController.class.getDeclaredMethod("getActiveStreams");
        Method checkWebRtcStream = VlsWebRtcController.class.getDeclaredMethod("checkWebRtcStream", String.class);

        assertArrayEquals(new String[] {"/config"}, getConfig.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/status"}, getStatus.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/refresh"}, refresh.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/start"}, startWebRtcStream.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/stop"}, stopWebRtcStream.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/validate"}, validateRtspStream.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/active"}, getActiveStreams.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/check/{streamId}"}, checkWebRtcStream.getAnnotation(GetMapping.class).value());
    }

    @Test
    void configReturnsBladeEnvelopeForFrontend() {
        IVlsWebRtcService service = mock(IVlsWebRtcService.class);
        VlsWebRtcController controller = new VlsWebRtcController(service);
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("serverUrl", "http://127.0.0.1:8000");
        config.put("available", true);
        config.put("enabled", true);

        when(service.getConfig()).thenReturn(config);

        BladeResult<Map<String, Object>> result = controller.getConfig();

        assertEquals(200, result.getCode());
        assertEquals(config, result.getData());
        verify(service).getConfig();
    }

    @Test
    void startReadsFrontendJsonBody() {
        IVlsWebRtcService service = mock(IVlsWebRtcService.class);
        VlsWebRtcController controller = new VlsWebRtcController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("deviceId", "camera-1");
        body.put("rtspUrl", "rtsp://example/live");
        Map<String, Object> serviceResult = Collections.<String, Object>singletonMap("streamId", "camera-1");

        when(service.startStream("camera-1", "rtsp://example/live", body)).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.startWebRtcStream(body);

        assertEquals(serviceResult, result.getData());
        verify(service).startStream("camera-1", "rtsp://example/live", body);
    }

    @Test
    void stopReadsFrontendJsonBody() {
        IVlsWebRtcService service = mock(IVlsWebRtcService.class);
        VlsWebRtcController controller = new VlsWebRtcController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("deviceId", "camera-1");
        Map<String, Object> serviceResult = Collections.<String, Object>singletonMap("stopped", true);

        when(service.stopStream("camera-1")).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.stopWebRtcStream(body);

        assertEquals(serviceResult, result.getData());
        verify(service).stopStream("camera-1");
    }

    @Test
    void validateReadsRtspUrlFromJsonBody() {
        IVlsWebRtcService service = mock(IVlsWebRtcService.class);
        VlsWebRtcController controller = new VlsWebRtcController(service);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("rtspUrl", "rtsp://example/live");
        Map<String, Object> serviceResult = Collections.<String, Object>singletonMap("valid", true);

        when(service.validateRtspStream("rtsp://example/live")).thenReturn(serviceResult);

        BladeResult<Map<String, Object>> result = controller.validateRtspStream(body);

        assertEquals(serviceResult, result.getData());
        verify(service).validateRtspStream("rtsp://example/live");
    }

    @Test
    void activeAndCheckReturnStreamState() {
        IVlsWebRtcService service = mock(IVlsWebRtcService.class);
        VlsWebRtcController controller = new VlsWebRtcController(service);
        List<Map<String, Object>> active = Collections.singletonList(Collections.<String, Object>singletonMap("streamId", "camera-1"));
        Map<String, Object> streamState = Collections.<String, Object>singletonMap("active", true);

        when(service.getActiveStreams()).thenReturn(active);
        when(service.checkStream("camera-1")).thenReturn(streamState);

        assertEquals(active, controller.getActiveStreams().getData());
        assertEquals(streamState, controller.checkWebRtcStream("camera-1").getData());
        verify(service).getActiveStreams();
        verify(service).checkStream("camera-1");
    }
}
