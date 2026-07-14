/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.bo.WorkOrderBo;
import com.ruoyi.workorder.domain.vo.WorkOrderVo;
import org.flowable.task.api.Task;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * 工单Service接口
 *
 * @author 雷超群
 * @date 2025-01-02
 */
public interface IWorkOrderService extends IService<WorkOrder> {

    /**
     * 查询工单
     */
    WorkOrderVo queryById(String id);

    /**
     * 查询工单列表
     */
    TableDataInfo<WorkOrderVo> queryPageList(WorkOrderBo bo, PageQuery pageQuery);

    /**
     * 我的工单列表
     *
     * @param processQuery
     * @param workOrderBo
     * @param pageQuery    分页参数
     * @param sysUser
     * @return
     */
    TableDataInfo<WorkOrderVo> selectPageOwnWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                          PageQuery pageQuery,
                                                          SysUser sysUser);

    /**
     * 查询代办工单
     *
     * @param workOrderBo
     * @param pageQuery   分页参数
     */
    TableDataInfo<WorkOrderVo> selectPageTodoWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                           PageQuery pageQuery,
                                                           SysUser sysUser);

    /**
     * 查询已办任务列表
     *
     * @param pageQuery 分页参数
     */
    TableDataInfo<WorkOrderVo> selectPageFinishedWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                          PageQuery pageQuery,
                                                          SysUser sysUser);

    /**
     * 查询工单列表
     */
    List<WorkOrderVo> queryList(WorkOrderBo bo);

    /**
     * 新增工单
     */
    WorkOrder insertByBo(WorkOrderBo bo, SysUser sysUser);

    /**
     * 修改工单
     */
    Boolean updateByBo(WorkOrderBo bo);

    /**
     * 更新工单状态
     *
     * @param task     任务列表（需确保非空且至少包含一个任务）
     * @param assignId
     * @return 是否更新成功
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    boolean updateWorkOrderToPending(Task task, String status, String assignId);

    /**
     * 工单结束更新状态
     *
     * @param processInstanceId
     * @param assignId
     * @return 是否更新成功
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    boolean updateWorkOrderToPending(String  processInstanceId, String status, String assignId);

    ByteArrayOutputStream generatePdf(List<String > wordOrderIds,SysUser sysUser)throws IOException;

    /**
     * 查询即时工单分页列表
     */
    TableDataInfo<WorkOrderVo> queryImmediatePageList(WorkOrderBo bo, PageQuery pageQuery);

    /**
     * 查询即时工单列表
     */
    List<WorkOrderVo> queryImmediateList(WorkOrderBo bo);

    /**
     * 查询循环工单分页列表
     */
    TableDataInfo<Object> queryLoopPageList(WorkOrderBo bo, PageQuery pageQuery,SysUser sysUser);

    /**
     * 查询循环工单列表
     */
    List<WorkOrderVo> queryLoopList(WorkOrderBo bo);

    /**
     * 校验并批量删除工单信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    TableDataInfo<WorkOrderVo> selectPageClaimWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo, PageQuery pageQuery, SysUser sysUser);

}
