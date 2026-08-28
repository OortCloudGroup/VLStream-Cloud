/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * work orderServiceinterface
 *
 * @author
 * @date 2025-01-02
 */
public interface IWorkOrderService extends IService<WorkOrder> {

    /**
     * Query work order
     */
    WorkOrderVo queryById(String id);

    /**
     * Query work order list
     */
    TableDataInfo<WorkOrderVo> queryPageList(WorkOrderBo bo, PageQuery pageQuery);

    /**
     * work order
     *
     * @param processQuery
     * @param workOrderBo
     * @param pageQuery parameter
     * @param sysUser
     * @return
     */
    TableDataInfo<WorkOrderVo> selectPageOwnWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                          PageQuery pageQuery,
                                                          SysUser sysUser);

    /**
     * Query work order
     *
     * @param workOrderBo
     * @param pageQuery parameter
     */
    TableDataInfo<WorkOrderVo> selectPageTodoWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                           PageQuery pageQuery,
                                                           SysUser sysUser);

    /**
     * Query already task list
     *
     * @param pageQuery parameter
     */
    TableDataInfo<WorkOrderVo> selectPageFinishedWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
                                                          PageQuery pageQuery,
                                                          SysUser sysUser);

    /**
     * Query work order list
     */
    List<WorkOrderVo> queryList(WorkOrderBo bo);

    /**
     * Add work order
     */
    WorkOrder insertByBo(WorkOrderBo bo, SysUser sysUser);

    /**
     * Update work order
     */
    Boolean updateByBo(WorkOrderBo bo);

    /**
     * new work order
     *
     * @param task task ( non- null / empty to task)
     * @param assignId
     * @return whether new successfully
     * @throws IllegalArgumentException parameterValidate failed
     */
    boolean updateWorkOrderToPending(Task task, String status, String assignId);

    /**
     * work orderfinish new
     *
     * @param processInstanceId
     * @param assignId
     * @return whether new successfully
     * @throws IllegalArgumentException parameterValidate failed
     */
    boolean updateWorkOrderToPending(String  processInstanceId, String status, String assignId);

    ByteArrayOutputStream generatePdf(List<String > wordOrderIds,SysUser sysUser)throws IOException;

    /**
     * Query work order list
     */
    TableDataInfo<WorkOrderVo> queryImmediatePageList(WorkOrderBo bo, PageQuery pageQuery);

    /**
     * Query work order list
     */
    List<WorkOrderVo> queryImmediateList(WorkOrderBo bo);

    /**
     * Query loopwork order list
     */
    TableDataInfo<Object> queryLoopPageList(WorkOrderBo bo, PageQuery pageQuery,SysUser sysUser);

    /**
     * Query loopwork order list
     */
    List<WorkOrderVo> queryLoopList(WorkOrderBo bo);

    /**
     * Validate Batch delete work orderinfo
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    TableDataInfo<WorkOrderVo> selectPageClaimWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo, PageQuery pageQuery, SysUser sysUser);

}
