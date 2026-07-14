/*
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
import com.ruoyi.workflow.domain.bo.WfFormSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfFormSynthesisVo;
import com.ruoyi.workflow.service.IWfFormSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 表单综合分类
 *
 * @author 雷超群
 * @date 2024-12-25
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/formSynthesis")
public class WfFormSynthesisController extends BaseController {

    private final IWfFormSynthesisService iWfFormSynthesisService;

    /**
     * 查询表单分类列表
     */
    @SaCheckPermission("workflow:formSynthesis:list")
    @GetMapping("/list")
    public R<List<WfFormSynthesisVo>> list(WfFormSynthesisBo bo) {
        List<WfFormSynthesisVo> list = iWfFormSynthesisService.queryList(bo).orElseGet(ArrayList::new);
        return R.ok(list);
    }

    /**
     * 导出表单分类列表
     */
    @SaCheckPermission("workflow:formSynthesis:export")
    @Log(title = "表单分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WfFormSynthesisBo bo, HttpServletResponse response) {
        List<WfFormSynthesisVo> list = iWfFormSynthesisService.queryList(bo).orElseGet(ArrayList::new);
        ExcelUtil.exportExcel(list, "表单分类", WfFormSynthesisVo.class, response);
    }

    /**
     * 获取表单分类详细信息
     *
     * @param categoryId 主键
     */
    @SaCheckPermission("workflow:formSynthesis:getInfo")
    @GetMapping("/{categoryId}")
    public R<WfFormSynthesisVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable String categoryId) {
        return R.ok(iWfFormSynthesisService.queryById(categoryId));
    }

    /**
     * 新增表单分类
     */
    @SaCheckPermission("workflow:formSynthesis:add")
    @Log(title = "表单分类", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WfFormSynthesisBo bo) {
        return toAjax(iWfFormSynthesisService.insertByBo(bo));
    }

    /**
     * 修改表单分类
     */
    @SaCheckPermission("workflow:formSynthesis:edit")
    @Log(title = "表单分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WfFormSynthesisBo bo) {
        return toAjax(iWfFormSynthesisService.updateByBo(bo));
    }

    /**
     * 删除表单分类
     *
     * @param categoryIds 主键串
     */
    @SaCheckPermission("workflow:formSynthesis:remove")
    @Log(title = "表单分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public R<Void> remove(@PathVariable String[] categoryIds) {
        Boolean b = iWfFormSynthesisService.deleteWithValidByIds(Arrays.asList(categoryIds), true);
        return toAjax(b);
    }
}
