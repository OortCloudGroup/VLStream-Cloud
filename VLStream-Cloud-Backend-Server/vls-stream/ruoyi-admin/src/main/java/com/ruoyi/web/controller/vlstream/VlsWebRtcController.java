/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.service.IVlsWebRtcService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * WebRTC routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webrtc")
public class VlsWebRtcController {

    private final IVlsWebRtcService webRtcService;

    @GetMapping("/config")
    public BladeResult<Map<String, Object>> getConfig() {
        return BladeResult.success(webRtcService.getConfig());
    }

    @GetMapping("/status")
    public BladeResult<Map<String, Object>> getStatus() {
        return BladeResult.success(webRtcService.getStatus());
    }

    @PostMapping("/refresh")
    public BladeResult<Map<String, Object>> refresh() {
        return BladeResult.success(webRtcService.refresh());
    }

    @PostMapping("/start")
    public BladeResult<Map<String, Object>> startWebRtcStream(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(webRtcService.startStream(text(body, "deviceId"), text(body, "rtspUrl"), body));
    }

    @PostMapping("/stop")
    public BladeResult<Map<String, Object>> stopWebRtcStream(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(webRtcService.stopStream(text(body, "deviceId")));
    }

    @PostMapping("/validate")
    public BladeResult<Map<String, Object>> validateRtspStream(@RequestBody(required = false) Map<String, Object> body) {
        return BladeResult.success(webRtcService.validateRtspStream(text(body, "rtspUrl")));
    }

    @GetMapping("/active")
    public BladeResult<List<Map<String, Object>>> getActiveStreams() {
        return BladeResult.success(webRtcService.getActiveStreams());
    }

    @GetMapping("/check/{streamId}")
    public BladeResult<Map<String, Object>> checkWebRtcStream(@PathVariable String streamId) {
        return BladeResult.success(webRtcService.checkStream(streamId));
    }

    private String text(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }
}
