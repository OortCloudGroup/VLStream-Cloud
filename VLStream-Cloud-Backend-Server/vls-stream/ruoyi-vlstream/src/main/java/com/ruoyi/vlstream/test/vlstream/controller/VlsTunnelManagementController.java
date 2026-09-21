/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TunnelDtos;
import com.ruoyi.vlstream.test.vlstream.service.TunnelAccessService;
import com.ruoyi.vlstream.test.vlstream.service.TunnelManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vlsTunnel")
@Tag(name = "IPC远程管理", description = "远程管理接入、状态与临时访问会话")
public class VlsTunnelManagementController {
    private final TunnelManagementService managementService;
    private final TunnelAccessService accessService;

    public VlsTunnelManagementController(TunnelManagementService managementService,
                                         TunnelAccessService accessService) {
        this.managementService = managementService;
        this.accessService = accessService;
    }

    @SaCheckPermission("vls:tunnel:manage")
    @Log(title = "签发IPC远程管理激活码", businessType = BusinessType.INSERT,
        isSaveResponseData = false)
    @PostMapping("/devices/{deviceId}/enrollments")
    @Operation(summary = "为当前租户设备签发一次性Agent激活码")
    public R<TunnelDtos.EnrollmentView> issueEnrollment(@PathVariable String deviceId) {
        return R.data(managementService.issueEnrollment(deviceId));
    }

    @SaCheckPermission("vls:tunnel:access")
    @GetMapping("/devices/{deviceId}")
    @Operation(summary = "查询设备远程管理状态")
    public R<TunnelDtos.EndpointView> status(@PathVariable String deviceId) {
        return R.data(managementService.getEndpoint(deviceId));
    }

    @SaCheckPermission("vls:tunnel:manage")
    @Log(title = "变更IPC远程管理状态", businessType = BusinessType.UPDATE)
    @PostMapping("/devices/{deviceId}/desired-state")
    @Operation(summary = "启用、停用或吊销设备远程管理")
    public R<TunnelDtos.EndpointView> changeState(@PathVariable String deviceId,
                                                   @RequestParam String state) {
        return R.data(managementService.changeDesiredState(deviceId, state));
    }

    @SaCheckPermission("vls:tunnel:access")
    @Log(title = "打开IPC远程管理后台", businessType = BusinessType.OTHER,
        isSaveResponseData = false)
    @PostMapping("/devices/{deviceId}/access-sessions")
    @Operation(summary = "创建短期远程管理访问会话")
    public R<TunnelDtos.AccessSessionView> createAccessSession(@PathVariable String deviceId) {
        return R.data(accessService.createSession(deviceId));
    }
}
