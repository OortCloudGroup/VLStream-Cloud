/*
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
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.bo.ProcessTemplateBo;
import com.ruoyi.workflow.domain.vo.ProcessTemplateVo;
import com.ruoyi.workflow.service.IProcessTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 流程初始化模版
 *
 * @author lcq
 * @date 2025-01-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/template")
public class ProcessTemplateController extends BaseController {

    private final IProcessTemplateService iProcessTemplateService;

    /**
     * 查询流程初始化模版列表
     */
    @SaCheckPermission("workflow:template:list")
    @GetMapping("/list")
    public TableDataInfo<ProcessTemplateVo> list(ProcessTemplateBo bo, PageQuery pageQuery) {
        return iProcessTemplateService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出流程初始化模版列表
     */
    @SaCheckPermission("workflow:template:export")
    @Log(title = "流程初始化模版", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ProcessTemplateBo bo, HttpServletResponse response) {
        List<ProcessTemplateVo> list = iProcessTemplateService.queryList(bo);
        ExcelUtil.exportExcel(list, "流程初始化模版", ProcessTemplateVo.class, response);
    }

    /**
     * 获取流程初始化模版详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("workflow:template:getInfo")
    @GetMapping("/{id}")
    public R<ProcessTemplateVo> getInfo(@NotNull(message = "主键不能为空")
                                        @PathVariable String id) {
        return R.ok(iProcessTemplateService.queryById(id));
    }

    /**
     * 新增流程初始化模版
     */
    @SaCheckPermission("workflow:template:add")
    @Log(title = "流程初始化模版", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ProcessTemplateBo bo) {
        return toAjax(iProcessTemplateService.insertByBo(bo));
    }

    /**
     * 修改流程初始化模版
     */
    @SaCheckPermission("workflow:template:edit")
    @Log(title = "流程初始化模版", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProcessTemplateBo bo) {
        return toAjax(iProcessTemplateService.updateByBo(bo));
    }

    /**
     * 删除流程初始化模版
     *
     * @param ids 主键串
     */
    @SaCheckPermission("workflow:template:remove")
    @Log(title = "流程初始化模版", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids) {
        return toAjax(iProcessTemplateService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
