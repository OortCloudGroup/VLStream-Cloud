/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 用户按接口配置要显示的字段 code 列表
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/field")
public class WfUserInterfaceFieldController extends BaseController {
    private final IWfUserInterfaceFieldService wfUserInterfaceFieldService;


    /**
     * 获取用户对某接口的字段配置
     * @param userId 用户ID
     * @param interfacePath 接口路径
     * @return 字段代码列表，null表示未配置应使用默认字段列表
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
     * 保存或修改用户字段配置
     * @param bo 保存请求对象
     * @return 操作结果
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
