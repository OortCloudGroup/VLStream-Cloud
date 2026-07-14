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
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.bo.WorkOrderAppBo;
import com.ruoyi.workflow.domain.vo.WorkOrderAppVo;
import com.ruoyi.workflow.service.IWorkOrderAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 应用工单分类
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/WorkOrder/app")
public class WorkOrderAppController extends BaseController {

    private final IWorkOrderAppService iWorkOrderAppService;

    /**
     * 查询应用工单分类列表
     */
    @SaCheckPermission("WorkOrder:app:list")
    @GetMapping("/list")
    public R<List<WorkOrderAppVo>> list(WorkOrderAppBo bo, PageQuery pageQuery) {
        return R.ok(iWorkOrderAppService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出应用工单分类列表
     */
    @SaCheckPermission("WorkOrder:app:export")
    @Log(title = "应用工单分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WorkOrderAppBo bo, HttpServletResponse response) {
        List<WorkOrderAppVo> list = iWorkOrderAppService.queryList(bo);
        ExcelUtil.exportExcel(list, "应用工单分类", WorkOrderAppVo.class, response);
    }

    /**
     * 获取应用工单分类详细信息
     *
     * @param appId 主键
     */
    @SaCheckPermission("WorkOrder:app:getInfo")
    @GetMapping("/{appId}")
    public R<WorkOrderAppVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable String appId) {
        return R.ok(iWorkOrderAppService.queryById(appId));
    }

    /**
     * 新增应用工单分类
     */
    @SaCheckPermission("WorkOrder:app:add")
    @Log(title = "应用工单分类", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WorkOrderAppBo bo) {
        return toAjax(iWorkOrderAppService.insertByBo(bo));
    }

    /**
     * 修改应用工单分类
     */
    @SaCheckPermission("WorkOrder:app:edit")
    @Log(title = "应用工单分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WorkOrderAppBo bo) {
        return toAjax(iWorkOrderAppService.updateByBo(bo));
    }

    /**
     * 删除应用工单分类
     *
     * @param appIds 主键串
     */
    @SaCheckPermission("WorkOrder:app:remove")
    @Log(title = "应用工单分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] appIds) {
        return toAjax(iWorkOrderAppService.deleteWithValidByIds(Arrays.asList(appIds), true));
    }
}
