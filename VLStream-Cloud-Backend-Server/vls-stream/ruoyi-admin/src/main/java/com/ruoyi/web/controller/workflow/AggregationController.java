/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.workflow.service.IAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * interface
 */
@RestController
@RequestMapping("/workflow/aggregation")
@RequiredArgsConstructor
public class AggregationController {
    private final IAggregationService aggregationService;

    /**
     * applicationIdGet workflow work order formid and id
     * @param applicationId
     * @return
     */
    @GetMapping("/FormAndAppId")
    @SaCheckPermission("workflow:aggregation:getFormAndAppId")
    public R<Map<String, Object>> getFormAndAppId(String applicationId,String formType) {
       return R.ok(aggregationService.getFormAndAppId(applicationId,formType));
    }

}
