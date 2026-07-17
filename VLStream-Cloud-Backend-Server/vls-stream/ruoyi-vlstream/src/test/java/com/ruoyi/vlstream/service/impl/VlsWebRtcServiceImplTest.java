/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class VlsWebRtcServiceImplTest {

//    @Test
//    void configContainsFrontendFields() {
//        VlsWebRtcServiceImpl service = configuredService();
//
//        Map<String, Object> config = service.getConfig();
//
//        assertEquals("http://127.0.0.1:8000", config.get("serverUrl"));
//        assertEquals("http://127.0.0.1:8000", config.get("webrtcServerUrl"));
//        assertEquals(Boolean.TRUE, config.get("available"));
//        assertEquals(Boolean.TRUE, config.get("enabled"));
//    }
//
//    @Test
//    void startStopAndCheckStreamState() {
//        VlsWebRtcServiceImpl service = configuredService();
//        Map<String, Object> body = new HashMap<String, Object>();
//        body.put("source", "frontend");
//
//        Map<String, Object> started = service.startStream("camera-1", "rtsp://example/live", body);
//
//        assertEquals("camera-1", started.get("streamId"));
//        assertEquals("rtsp://example/live", started.get("rtspUrl"));
//        assertEquals(Boolean.TRUE, started.get("active"));
//        assertTrue(String.valueOf(started.get("webrtcUrl")).contains("rtsp%3A%2F%2Fexample%2Flive"));
//        assertEquals(1, service.getActiveStreams().size());
//        assertEquals(Boolean.TRUE, service.checkStream("camera-1").get("active"));
//
//        Map<String, Object> stopped = service.stopStream("camera-1");
//
//        assertEquals(Boolean.TRUE, stopped.get("stopped"));
//        assertEquals(0, service.getActiveStreams().size());
//        assertEquals(Boolean.FALSE, service.checkStream("camera-1").get("active"));
//    }
//
//    @Test
//    void validateRtspSchemeWithoutCallingExternalStreamer() {
//        VlsWebRtcServiceImpl service = configuredService();
//
//        assertEquals(Boolean.TRUE, service.validateRtspStream("rtsp://example/live").get("valid"));
//        assertEquals(Boolean.FALSE, service.validateRtspStream("http://example/live").get("valid"));
//    }
//
//    private VlsWebRtcServiceImpl configuredService() {
//        VlsWebRtcServiceImpl service = new VlsWebRtcServiceImpl();
//        ReflectionTestUtils.setField(service, "serverUrl", "http://127.0.0.1:8000");
//        ReflectionTestUtils.setField(service, "enabled", true);
//        ReflectionTestUtils.setField(service, "available", true);
//        return service;
//    }
}
