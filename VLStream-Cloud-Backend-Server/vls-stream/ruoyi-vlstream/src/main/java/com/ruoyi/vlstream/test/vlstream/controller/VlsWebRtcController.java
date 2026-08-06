/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.service.VlsWebRtcGatewayService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Browser-facing WebRTC runtime configuration and health endpoints.
 */
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class VlsWebRtcController {

	private final VlsWebRtcGatewayService webRtcGatewayService;

	/**
	 * Returns the public media URL and cached availability used by the frontend.
	 */
	@GetMapping("/config")
	public R<Map<String, Object>> getConfig() {
		return R.data(webRtcGatewayService.getPublicConfig());
	}

	/**
	 * Forces a real media gateway health probe without faking service availability.
	 */
	@GetMapping("/status")
	public R<Map<String, Object>> getStatus() {
		return R.data(webRtcGatewayService.getStatus());
	}
}
