/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.service.TunnelAgentService;
import com.ruoyi.vlstream.test.vlstream.service.TunnelApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/vlsTunnel/agent")
@Tag(name = "IPC远程管理Agent", description = "设备端注册、配置与心跳")
public class VlsTunnelAgentController {
    private final TunnelAgentService agentService;

    public VlsTunnelAgentController(TunnelAgentService agentService) {
        this.agentService = agentService;
    }

    @SaIgnore
    @PostMapping("/register")
    @Operation(summary = "使用一次性激活码注册Agent")
    public ResponseEntity<TunnelDtos.ApiResponse<TunnelDtos.AgentConfigView>> register(
        @Valid @RequestBody TunnelDtos.AgentRegisterRequest request) {
        try {
            return ResponseEntity.ok(TunnelDtos.ApiResponse.success(agentService.register(request)));
        } catch (TunnelApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(TunnelDtos.ApiResponse.<TunnelDtos.AgentConfigView>error(
                    exception.getStatus(), exception.getMessage()));
        }
    }

    @SaIgnore
    @GetMapping("/config")
    @Operation(summary = "获取Agent当前期望配置")
    public ResponseEntity<TunnelDtos.ApiResponse<TunnelDtos.AgentConfigView>> config(
        @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            return ResponseEntity.ok(TunnelDtos.ApiResponse.success(
                agentService.config(bearerToken(authorization))));
        } catch (TunnelApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(TunnelDtos.ApiResponse.<TunnelDtos.AgentConfigView>error(
                    exception.getStatus(), exception.getMessage()));
        }
    }

    @SaIgnore
    @PostMapping("/heartbeat")
    @Operation(summary = "上报Agent、隧道和IPC本地Web状态")
    public ResponseEntity<TunnelDtos.ApiResponse<TunnelDtos.HeartbeatView>> heartbeat(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody TunnelDtos.AgentHeartbeatRequest request) {
        try {
            return ResponseEntity.ok(TunnelDtos.ApiResponse.success(
                agentService.heartbeat(bearerToken(authorization), request)));
        } catch (TunnelApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(TunnelDtos.ApiResponse.<TunnelDtos.HeartbeatView>error(
                    exception.getStatus(), exception.getMessage()));
        }
    }

    private String bearerToken(String authorization) {
        String value = StringUtils.trimToEmpty(authorization);
        if (!StringUtils.startsWithIgnoreCase(value, "Bearer ")) {
            throw new TunnelApiException(401, "Authorization必须使用Bearer Agent Token");
        }
        return StringUtils.trim(value.substring(7));
    }
}
