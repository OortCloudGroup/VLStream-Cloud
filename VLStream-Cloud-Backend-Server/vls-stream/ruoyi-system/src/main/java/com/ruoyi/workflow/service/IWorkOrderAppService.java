/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.domain.bo.WorkOrderAppBo;
import com.ruoyi.workflow.domain.vo.WorkOrderAppVo;

import java.util.Collection;
import java.util.List;

/**
 * work order Serviceinterface
 *
 * @author
 * @date 2025-01-04
 */
public interface IWorkOrderAppService extends IService<WorkOrderApp> {

    /**
     * Query work order
     */
    WorkOrderAppVo queryById(String appId);

    /**
     * Query work order list
     */
    List<WorkOrderAppVo> queryPageList(WorkOrderAppBo bo, PageQuery pageQuery);

    /**
     * Query work order list
     */
    List<WorkOrderAppVo> queryList(WorkOrderAppBo bo);

    /**
     * Add work order
     */
    Boolean insertByBo(WorkOrderAppBo bo);

    /**
     * Update work order
     */
    Boolean updateByBo(WorkOrderAppBo bo);

    /**
     * Validate Batch delete work order info
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
