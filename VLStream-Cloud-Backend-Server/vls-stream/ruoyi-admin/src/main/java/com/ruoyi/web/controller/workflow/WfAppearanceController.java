/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.workflow.domain.vo.WfAppearanceAllCountVo;
import com.ruoyi.workflow.service.IWfAppearanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表板接口
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/appearance")
public class WfAppearanceController extends BaseController {

    private final IWfAppearanceService appearanceService;

    /**
     * 统计所有工单数量
     */
    @GetMapping("/getAllCount")
    @SaCheckPermission("workflow:appearance:getAllCount")
    public R<WfAppearanceAllCountVo> getAllCount(@RequestHeader("Authorization")String token) {
        return R.ok(appearanceService.getAllCount(token));
    }
}
