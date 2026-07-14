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
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.bo.WfAppBo;
import com.ruoyi.workflow.domain.vo.WfAppVo;
import com.ruoyi.workflow.service.IWfAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 应用通用流程
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wf/app")
public class WfAppController extends BaseController {

    private final IWfAppService iWfAppService;

    /**
     * 查询应用通用流程列表
     */
    @SaCheckPermission("wf:app:list")
    @GetMapping("/list")
    public R<List<WfAppVo>> list(WfAppBo bo, PageQuery pageQuery) {
        return R.ok(iWfAppService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出应用通用流程列表
     */
    @SaCheckPermission("wf:app:export")
    @Log(title = "应用通用流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WfAppBo bo, HttpServletResponse response) {
        List<WfAppVo> list = iWfAppService.queryList(bo);
        ExcelUtil.exportExcel(list, "应用通用流程", WfAppVo.class, response);
    }

    /**
     * 获取应用通用流程详细信息
     *
     * @param appId 主键
     */
    @SaCheckPermission("wf:app:getInfo")
    @GetMapping("/{appId}")
    public R<WfAppVo> getInfo(@NotNull(message = "主键不能为空")
                              @PathVariable String appId) {
        return R.ok(iWfAppService.queryById(appId));
    }

    /**
     * 新增应用通用流程
     */
    @SaCheckPermission("wf:app:add")
    @RepeatSubmit()
    @PostMapping()
    public R<WfApp> add(@RequestBody WfAppBo bo) {
        return R.ok(iWfAppService.insertByBo(bo));
    }

    /**
     * 修改应用通用流程
     */
    @SaCheckPermission("wf:app:edit")
    @Log(title = "应用通用流程", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WfAppBo bo) {
        return toAjax(iWfAppService.updateByBo(bo));
    }

    /**
     * 删除应用通用流程
     *
     * @param appIds 主键串
     */
    @SaCheckPermission("wf:app:remove")
    @Log(title = "应用通用流程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String [] appIds) {
        return toAjax(iWfAppService.deleteWithValidByIds(Arrays.asList(appIds), true));
    }
}
