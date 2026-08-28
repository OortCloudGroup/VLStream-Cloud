/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.bo.WorkOrderSynthesisBo;
import com.ruoyi.workflow.domain.vo.WorkOrderSynthesisVo;
import com.ruoyi.workflow.service.IWorkOrderSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * work orderworkflow
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workorder/synthesis")
public class WorkOrderSynthesisController extends BaseController {

    private final IWorkOrderSynthesisService iWorkOrderSynthesisService;

    /**
     * Query work orderworkflow list
     */
    @SaCheckPermission("workorder:synthesis:list")
    @GetMapping("/list")
    public R<List<WorkOrderSynthesisVo>> list(WorkOrderSynthesisBo bo) {
        List<WorkOrderSynthesisVo> list = iWorkOrderSynthesisService.queryList(bo);
        return R.ok(list);
    }

    /**
     * Query full work orderworkflow
     */
    @SaCheckPermission("workorder:synthesis:queryListAll")
    @GetMapping("/listAll")
    R<List<WorkOrderSynthesisVo>> queryListAll(String categoryName) {
        return R.ok(iWorkOrderSynthesisService.queryListAll(categoryName));
    }

    /**
     * Export work orderworkflow
     */
    @SaCheckPermission("workorder:synthesis:export")
    @Log(title = "综合工单流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WorkOrderSynthesisBo bo, HttpServletResponse response) {
        List<WorkOrderSynthesisVo> list = iWorkOrderSynthesisService.queryList(bo);
        ExcelUtil.exportExcel(list, "综合工单流程", WorkOrderSynthesisVo.class, response);
    }

    /**
     * Get work orderworkflow info
     *
     * @param synthesisId primary key
     */
    @SaCheckPermission("workorder:synthesis:getInfo")
    @GetMapping("/{synthesisId}")
    public R<WorkOrderSynthesisVo> getInfo(@NotNull(message = "主键不能为空")
                                           @PathVariable String synthesisId) {
        return R.ok(iWorkOrderSynthesisService.queryById(synthesisId));
    }

    /**
     * Add work orderworkflow
     */
    @SaCheckPermission("workorder:synthesis:add")
    @Log(title = "综合工单流程", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WorkOrderSynthesisBo bo) {
        return toAjax(iWorkOrderSynthesisService.insertByBo(bo));
    }

    /**
     * Update work orderworkflow
     */
    @SaCheckPermission("workorder:synthesis:edit")
    @Log(title = "综合工单流程", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WorkOrderSynthesisBo bo) {
        return toAjax(iWorkOrderSynthesisService.updateByBo(bo));
    }

    /**
     * Delete work orderworkflow
     *
     * @param synthesisIds primary key
     */
    @SaCheckPermission("workorder:synthesis:remove")
    @Log(title = "综合工单流程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{synthesisIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] synthesisIds) {
        return toAjax(iWorkOrderSynthesisService.deleteWithValidByIds(Arrays.asList(synthesisIds), true));
    }
}
