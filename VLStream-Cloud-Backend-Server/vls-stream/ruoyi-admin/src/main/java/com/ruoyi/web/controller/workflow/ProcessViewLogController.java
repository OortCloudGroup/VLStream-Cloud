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
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.workflow.domain.bo.ProcessViewLogBo;
import com.ruoyi.workflow.domain.vo.ProcessViewLogVo;
import com.ruoyi.workflow.service.IProcessViewLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 流程访问日志
 *
 * @author lcq
 * @date 2025-08-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/viewLog")
public class ProcessViewLogController extends BaseController {

    private final IProcessViewLogService iProcessViewLogService;

    /**
     * 查询流程访问日志列表
     */
    @SaCheckPermission("workflow:viewLog:list")
    @GetMapping("/list")
    public TableDataInfo<ProcessViewLogVo> list(ProcessViewLogBo bo, PageQuery pageQuery) {
        return iProcessViewLogService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询流程访问人员列表
     */
    @SaCheckPermission("workflow:UserViewLog:list")
    @GetMapping("/userList")
    public TableDataInfo<ProcessViewLogVo> userList(ProcessViewLogBo bo, PageQuery pageQuery) {
        return iProcessViewLogService.queryUserPageList(bo, pageQuery);
    }

    /**
     * 导出流程访问日志列表
     */
    @SaCheckPermission("workflow:viewLog:export")
    @PostMapping("/export")
    public void export(ProcessViewLogBo bo, HttpServletResponse response) {
        List<ProcessViewLogVo> list = iProcessViewLogService.queryList(bo);
        ExcelUtil.exportExcel(list, "流程访问日志", ProcessViewLogVo.class, response);
    }

    /**
     * 获取流程访问日志详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("workflow:viewLog:query")
    @GetMapping("/{id}")
    public R<ProcessViewLogVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable String id) {
        return R.ok(iProcessViewLogService.queryById(id));
    }

    /**
     * 新增流程访问日志
     */
    @SaCheckPermission("workflow:viewLog:add")
    @Log(title = "流程访问日志", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ProcessViewLogBo bo) {
        SysUser sysUser=RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return toAjax(iProcessViewLogService.insertByBo(bo,sysUser));
    }

    /**
     * 修改流程访问日志
     */
    @SaCheckPermission("workflow:viewLog:edit")
    @Log(title = "流程访问日志", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProcessViewLogBo bo) {
        return toAjax(iProcessViewLogService.updateByBo(bo));
    }

    /**
     * 删除流程访问日志
     *
     * @param ids 主键串
     */
    @SaCheckPermission("workflow:viewLog:remove")
    @Log(title = "流程访问日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids) {
        return toAjax(iProcessViewLogService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
