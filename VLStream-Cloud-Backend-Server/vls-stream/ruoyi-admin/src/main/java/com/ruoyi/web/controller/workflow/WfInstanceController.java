/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow;


import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.workflow.service.IWfInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * workflow instance
 *
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/instance")
public class WfInstanceController {

    private final IWfInstanceService instanceService;

    /**
     * workflow instance
     *
     * @param state 1: ,2:
     * @param instanceId workflow instance ID
     */
    @SaCheckPermission("workflow:instance:updateState")
    @PostMapping(value = "/updateState")
    public R updateState(@RequestParam Integer state, @RequestParam String instanceId) {
        instanceService.updateState(state, instanceId);
        return R.ok();
    }

//    /**
// * finishworkflow instance
//     *
// * @param bo workflowtask object
//     */
//    @SaCheckPermission("workflow:instance:stopProcessInstance")
//    @PostMapping(value = "/stopProcessInstance")
//    public R stopProcessInstance(@RequestBody WfTaskBo bo) {
//        instanceService.stopProcessInstance(bo);
//        return R.ok();
//    }

    /**
     * Delete workflow instance
     *
     * @param instanceId workflow instance ID
     * @param deleteReason Delete
     */
    @SaCheckPermission("workflow:instance:delete")
    @Deprecated
    @DeleteMapping(value = "/delete")
    public R delete(@RequestParam String instanceId, String deleteReason) {
        instanceService.delete(instanceId, deleteReason,RedisUtils.getCacheObject(AuthorizationInterceptor.getToken()));
        return R.ok();
    }

    /**
     * Query workflow instance info
     *
     * @param procInsId workflow instance ID
     * @param deployId workflow ID
     */
    @SaCheckPermission("workflow:instance:detail")
    @GetMapping("/detail")
    public R detail(String procInsId, String deployId) {
        return R.ok(instanceService.queryDetailProcess(procInsId, deployId));
    }
}
