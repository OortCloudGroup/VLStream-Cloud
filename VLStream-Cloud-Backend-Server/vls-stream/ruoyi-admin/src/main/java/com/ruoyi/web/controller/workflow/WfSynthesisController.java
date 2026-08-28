/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
import com.ruoyi.workflow.domain.bo.WfSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;
import com.ruoyi.workflow.service.IWfSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * workflow
 *
 * @author
 * @date 2025-01-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wf/synthesis")
public class WfSynthesisController extends BaseController {

    private final IWfSynthesisService iWfSynthesisService;

    /**
     * Query workflow list
     */
    @SaCheckPermission("wf:synthesis:list")
    @GetMapping("/list")
    public R<List<WfSynthesisVo>> list(WfSynthesisBo bo) {
        List<WfSynthesisVo> list = iWfSynthesisService.queryList(bo);
        return R.ok(list);
    }

    /**
     * Query full workflow
     */
    @SaCheckPermission("wf:synthesis:queryListAll")
    @GetMapping("/listAll")
    R<List<WfSynthesisVo>> queryListAll(String categoryName) {
        return R.ok(iWfSynthesisService.queryListAll(categoryName));
    }

    /**
     * Export workflow
     */
    @SaCheckPermission("wf:synthesis:export")
    @Log(title = "综合通用流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WfSynthesisBo bo, HttpServletResponse response) {
        List<WfSynthesisVo> list = iWfSynthesisService.queryList(bo);
        ExcelUtil.exportExcel(list, "综合通用流程", WfSynthesisVo.class, response);
    }

    /**
     * Get workflow info
     *
     * @param synthesisId primary key
     */
    @SaCheckPermission("wf:synthesis:getInfo")
    @GetMapping("/{synthesisId}")
    public R<WfSynthesisVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable String synthesisId) {
        return R.ok(iWfSynthesisService.queryById(synthesisId));
    }

    /**
     * Add workflow
     */
    @SaCheckPermission("wf:synthesis:add")
    @Log(title = "综合通用流程", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WfSynthesisBo bo) {
        return toAjax(iWfSynthesisService.insertByBo(bo));
    }

    /**
     * Update workflow
     */
    @SaCheckPermission("wf:synthesis:edit")
    @Log(title = "综合通用流程", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WfSynthesisBo bo) {
        return toAjax(iWfSynthesisService.updateByBo(bo));
    }

    /**
     * Delete workflow
     *
     * @param synthesisIds primary key
     */
    @SaCheckPermission("wf:synthesis:remove")
    @Log(title = "综合通用流程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{synthesisIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable String[] synthesisIds) {
        return toAjax(iWfSynthesisService.deleteWithValidByIds(Arrays.asList(synthesisIds), true));
    }
}
