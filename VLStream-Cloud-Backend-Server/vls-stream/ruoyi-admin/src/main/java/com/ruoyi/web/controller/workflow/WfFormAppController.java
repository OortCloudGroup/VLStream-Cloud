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
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.WfFormApp;
import com.ruoyi.workflow.domain.bo.WfFormAppBo;
import com.ruoyi.workflow.domain.vo.WfFormAppVo;
import com.ruoyi.workflow.service.IWfFormAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * form
 *
 * @author
 * @date 2025-04-26
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/formApp")
public class WfFormAppController extends BaseController {

    private final IWfFormAppService iWfFormAppService;

    /**
     * Query form list
     */
    @SaCheckPermission("system:formApp:list")
    @GetMapping("/list")
    public TableDataInfo<WfFormAppVo> list(WfFormAppBo bo, PageQuery pageQuery) {
        return iWfFormAppService.queryPageList(bo, pageQuery);
    }

    /**
     * Export form
     */
    @SaCheckPermission("system:formApp:export")
    @Log(title = "表单应用分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WfFormAppBo bo, HttpServletResponse response) {
        Optional<List<WfFormAppVo>> list = iWfFormAppService.queryList(bo);
        ExcelUtil.exportExcel(list.orElseGet(ArrayList::new), "表单应用分类", WfFormAppVo.class, response);
    }

    /**
     * Get form info
     *
     * @param categoryId primary key
     */
    @SaCheckPermission("system:formApp:query")
    @GetMapping("/{categoryId}")
    public R<WfFormAppVo> getInfo(@NotNull(message = "主键不能为空")
                                  @PathVariable String categoryId) {
        return R.ok(iWfFormAppService.queryById(categoryId));
    }

    /**
     * Add form
     */
    @SaCheckPermission("system:formApp:add")
    @Log(title = "表单应用分类", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<WfFormApp> add(@RequestBody WfFormAppBo bo) {
        return R.ok(iWfFormAppService.insertByBo(bo));
    }

    /**
     * Update form
     */
    @SaCheckPermission("system:formApp:edit")
    @Log(title = "表单应用分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WfFormAppBo bo) {
        return toAjax(iWfFormAppService.updateByBo(bo));
    }

    /**
     * Delete form
     * @param categoryIds primary key
     */
    @SaCheckPermission("system:formApp:remove")
    @Log(title = "表单应用分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] categoryIds) {
        return toAjax(iWfFormAppService.deleteWithValidByIds(Arrays.asList(categoryIds), true));
    }
}
