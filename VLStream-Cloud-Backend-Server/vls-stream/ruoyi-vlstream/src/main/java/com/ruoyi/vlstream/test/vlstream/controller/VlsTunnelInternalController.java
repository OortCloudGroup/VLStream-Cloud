/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.service.TunnelAccessService;
import com.ruoyi.vlstream.test.vlstream.service.TunnelApiException;
import com.ruoyi.vlstream.test.vlstream.service.TunnelControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/vlsTunnel/internal")
@Tag(name = "IPC远程管理内部接口", description = "仅供受信rathole控制器和HTTPS网关调用")
public class VlsTunnelInternalController {
    private final TunnelControlService controlService;
    private final TunnelAccessService accessService;

    public VlsTunnelInternalController(TunnelControlService controlService,
                                       TunnelAccessService accessService) {
        this.controlService = controlService;
        this.accessService = accessService;
    }

    @SaIgnore
    @GetMapping("/routes")
    @Operation(summary = "读取期望的rathole服务配置")
    public ResponseEntity<TunnelDtos.ApiResponse<List<TunnelDtos.RouteView>>> routes(
        @RequestHeader(value = "X-Tunnel-Control-Token", required = false) String token) {
        try {
            controlService.verifyControlToken(token);
            return ResponseEntity.ok(TunnelDtos.ApiResponse.success(controlService.desiredRoutes()));
        } catch (TunnelApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(TunnelDtos.ApiResponse.<List<TunnelDtos.RouteView>>error(
                    exception.getStatus(), exception.getMessage()));
        }
    }

    @SaIgnore
    @PostMapping("/routes/{endpointId}/status")
    @Operation(summary = "回报rathole服务配置应用结果")
    public ResponseEntity<TunnelDtos.ApiResponse<Void>> routeStatus(
        @RequestHeader(value = "X-Tunnel-Control-Token", required = false) String token,
        @PathVariable Long endpointId,
        @Valid @RequestBody TunnelDtos.RouteStatusRequest request) {
        try {
            controlService.verifyControlToken(token);
            controlService.reportRouteStatus(endpointId, request);
            return ResponseEntity.ok(TunnelDtos.ApiResponse.<Void>success(null));
        } catch (TunnelApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(TunnelDtos.ApiResponse.<Void>error(exception.getStatus(), exception.getMessage()));
        }
    }

    @SaIgnore
    @PostMapping("/access-sessions/resolve")
    @Operation(summary = "HTTPS网关解析短期访问令牌")
    public ResponseEntity<TunnelDtos.ApiResponse<TunnelDtos.SessionRouteView>> resolveSession(
        @RequestHeader(value = "X-Tunnel-Gateway-Token", required = false) String token,
        @RequestHeader(value = "X-Tunnel-Access-Token", required = false) String accessToken) {
        try {
            accessService.verifyGatewayToken(token);
            return ResponseEntity.ok(TunnelDtos.ApiResponse.success(
                accessService.resolveSession(accessToken)));
        } catch (TunnelApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(TunnelDtos.ApiResponse.<TunnelDtos.SessionRouteView>error(
                    exception.getStatus(), exception.getMessage()));
        }
    }
}
