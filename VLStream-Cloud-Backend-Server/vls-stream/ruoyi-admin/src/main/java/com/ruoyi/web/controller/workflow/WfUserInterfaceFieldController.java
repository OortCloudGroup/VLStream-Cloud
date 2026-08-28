/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.workflow.domain.bo.WfUserInterfaceFieldBo;
import com.ruoyi.workflow.service.IWfUserInterfaceFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * user interfaceconfiguration need to field code
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/field")
public class WfUserInterfaceFieldController extends BaseController {
    private final IWfUserInterfaceFieldService wfUserInterfaceFieldService;


    /**
     * Get user interface fieldconfiguration
     * @param userId user ID
     * @param interfacePath interface
     * @return field , null not configuration field
     */
    @SaCheckPermission("workflow:field:codes")
    @GetMapping("/field-codes")
    public R<String> getFieldCodes(
        @RequestParam String userId,
        @RequestParam String interfacePath) {
        try {
            String fieldCodes = wfUserInterfaceFieldService.getFieldCodes(userId, interfacePath);
            return R.ok(fieldCodes);
        } catch (Exception e) {
            return R.fail();
        }
    }

    /**
     * Update userfieldconfiguration
     * @param bo object
     * @return operation
     */
    @SaCheckPermission("workflow:field:saveCodes")
    @PostMapping("/field-codes")
    public  R<Void> saveFieldCodes(@RequestBody WfUserInterfaceFieldBo bo) {
        try {
            return toAjax( wfUserInterfaceFieldService.saveFieldCodes(
                bo.getUserId(),
                bo.getInterfacePath(),
                JSON.toJSONString(bo.getFieldCodes())));
        } catch (Exception e) {
            return R.fail();
        }
    }

}
