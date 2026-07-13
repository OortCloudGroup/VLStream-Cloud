/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
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
 * 综合工单流程
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
     * 查询综合工单流程列表
     */
    @SaCheckPermission("workorder:synthesis:list")
    @GetMapping("/list")
    public R<List<WorkOrderSynthesisVo>> list(WorkOrderSynthesisBo bo) {
        List<WorkOrderSynthesisVo> list = iWorkOrderSynthesisService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 查询全部综合工单流程
     */
    @SaCheckPermission("workorder:synthesis:queryListAll")
    @GetMapping("/listAll")
    R<List<WorkOrderSynthesisVo>> queryListAll(String categoryName) {
        return R.ok(iWorkOrderSynthesisService.queryListAll(categoryName));
    }

    /**
     * 导出综合工单流程列表
     */
    @SaCheckPermission("workorder:synthesis:export")
    @Log(title = "综合工单流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WorkOrderSynthesisBo bo, HttpServletResponse response) {
        List<WorkOrderSynthesisVo> list = iWorkOrderSynthesisService.queryList(bo);
        ExcelUtil.exportExcel(list, "综合工单流程", WorkOrderSynthesisVo.class, response);
    }

    /**
     * 获取综合工单流程详细信息
     *
     * @param synthesisId 主键
     */
    @SaCheckPermission("workorder:synthesis:getInfo")
    @GetMapping("/{synthesisId}")
    public R<WorkOrderSynthesisVo> getInfo(@NotNull(message = "主键不能为空")
                                           @PathVariable String synthesisId) {
        return R.ok(iWorkOrderSynthesisService.queryById(synthesisId));
    }

    /**
     * 新增综合工单流程
     */
    @SaCheckPermission("workorder:synthesis:add")
    @Log(title = "综合工单流程", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WorkOrderSynthesisBo bo) {
        return toAjax(iWorkOrderSynthesisService.insertByBo(bo));
    }

    /**
     * 修改综合工单流程
     */
    @SaCheckPermission("workorder:synthesis:edit")
    @Log(title = "综合工单流程", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WorkOrderSynthesisBo bo) {
        return toAjax(iWorkOrderSynthesisService.updateByBo(bo));
    }

    /**
     * 删除综合工单流程
     *
     * @param synthesisIds 主键串
     */
    @SaCheckPermission("workorder:synthesis:remove")
    @Log(title = "综合工单流程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{synthesisIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] synthesisIds) {
        return toAjax(iWorkOrderSynthesisService.deleteWithValidByIds(Arrays.asList(synthesisIds), true));
    }
}
