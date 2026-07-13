/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.bo.WorkOrderBo;
import com.ruoyi.workorder.domain.vo.WorkOrderVo;
import com.ruoyi.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 工单管理
 *
 * @author 雷超群
 * @date 2025-01-02
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workorder/workorder")
public class WorkOrderController extends BaseController {

    private final IWorkOrderService iWorkOrderService;

    /**
     * 我的工单列表
     *
     * @param processQuery
     * @param workOrderBo
     * @param pageQuery
     * @return
     */
    @GetMapping("/ownWorkOrderList")
    @SaCheckPermission("workflow:workorder:selectPageOwnWorkOrderList")
    public TableDataInfo<WorkOrderVo> selectPageOwnWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                                 PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return iWorkOrderService.selectPageOwnWorkOrderList(processQuery, workOrderBo, pageQuery, sysUser);
    }

    /**
     * 待办工单列表
     *
     * @param processQuery
     * @param workOrderBo
     * @param pageQuery
     * @return
     */
    @GetMapping("/todoWorkOrderList")
    @SaCheckPermission("workflow:workorder:selectPageTodoWorkOrderList")
    public TableDataInfo<WorkOrderVo> selectPageTodoWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                                  PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return iWorkOrderService.selectPageTodoWorkOrderList(processQuery, workOrderBo, pageQuery, sysUser);
    }

    /**
     * 已办工单列表
     *
     * @param processQuery
     * @param workOrderBo
     * @param pageQuery
     * @return
     */
    @GetMapping("/finishedWorkOrderList")
    @SaCheckPermission("workflow:workorder:selectPageFinishedWorkOrderList")
    public TableDataInfo<WorkOrderVo> selectPageFinishedWorkOrderList(ProcessQuery processQuery,
                                                                      WorkOrderBo workOrderBo,
                                                                      PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return iWorkOrderService.selectPageFinishedWorkOrderList(processQuery, workOrderBo, pageQuery, sysUser);
    }

    /**
     * 可接工单列表
     *
     * @param processQuery 流程业务对象
     * @param pageQuery    分页参数
     */
    @SaCheckPermission("workflow:workorder:claimWorkOrderList")
    @GetMapping(value = "/claimWorkOrderList")
    public TableDataInfo<WorkOrderVo> claimWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                         PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return iWorkOrderService.selectPageClaimWorkOrderList(processQuery, workOrderBo, pageQuery, sysUser);
    }

    /**
     * 查询即时工单列表
     */
    @SaCheckPermission("workorder:workorder:ImmediateList")
    @GetMapping("/ImmediateList")
    public TableDataInfo<WorkOrderVo> ImmediateList(WorkOrderBo bo, PageQuery pageQuery) {
        return iWorkOrderService.queryImmediatePageList(bo, pageQuery);
    }

    /**
     * 查询循环工单列表
     */
    @SaCheckPermission("workorder:workorder:LoopList")
    @GetMapping("/LoopList")
    public TableDataInfo<Object> LoopList(WorkOrderBo bo, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return iWorkOrderService.queryLoopPageList(bo, pageQuery, sysUser);
    }

    /**
     * 导出工单列表
     */
    @SaCheckPermission("workorder:workorder:export")
    @Log(title = "工单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WorkOrderBo bo, HttpServletResponse response) {
        List<WorkOrderVo> list = iWorkOrderService.queryImmediateList(bo);
        ExcelUtil.exportExcel(list, "工单", WorkOrderVo.class, response);
    }

    /**
     * 获取工单详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("workorder:workorder:getInfo")
    @GetMapping("/{id}")
    public R<WorkOrderVo> getInfo(@NotNull(message = "主键不能为空")
                                  @PathVariable String id) {
        return R.ok(iWorkOrderService.queryById(id));
    }

    /**
     * 新增工单
     */
    @SaCheckPermission("workorder:workorder:add")
    @Log(title = "工单", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<WorkOrder> add(@Validated(AddGroup.class) @RequestBody WorkOrderBo bo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return R.ok(iWorkOrderService.insertByBo(bo, sysUser));
    }

    /**
     * 修改工单
     */
    @SaCheckPermission("workorder:workorder:edit")
    @Log(title = "工单", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WorkOrderBo bo) {
        return toAjax(iWorkOrderService.updateByBo(bo));
    }

    /**
     * 删除工单
     *
     * @param ids 主键串
     */
    @SaCheckPermission("workorder:workorder:remove")
    @Log(title = "工单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids) {
        return toAjax(iWorkOrderService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 批量打印工单
     *
     * @param wordOrderIds
     * @param response
     */
    @SaCheckPermission("workorder:workorder:exportPdf")
    @PostMapping("/exportPdf")
    public void exportPdf(@RequestBody List<String> wordOrderIds, HttpServletResponse response) {
        try {
            // 设置响应头（关键修改：使用 inline）
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=工单.pdf"); // 改为 inline
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // 禁用缓存
            // 生成并返回 PDF 流
            SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
            ByteArrayOutputStream pdfStream = iWorkOrderService.generatePdf(wordOrderIds, sysUser);
            pdfStream.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            try {
                response.getWriter().write("预览失败：" + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
