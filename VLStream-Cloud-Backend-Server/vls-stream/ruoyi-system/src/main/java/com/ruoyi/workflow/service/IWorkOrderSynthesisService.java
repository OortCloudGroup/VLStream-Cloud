/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.workflow.domain.WorkOrderSynthesis;
import com.ruoyi.workflow.domain.bo.WorkOrderSynthesisBo;
import com.ruoyi.workflow.domain.vo.WorkOrderSynthesisVo;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * work orderworkflowServiceinterface
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
public interface IWorkOrderSynthesisService extends IService<WorkOrderSynthesis> {

    /**
     * Query work orderworkflow
     */
    WorkOrderSynthesisVo queryById(String synthesisId);

    /**
     * Query work orderworkflow list
     */
    List<WorkOrderSynthesisVo> queryList(WorkOrderSynthesisBo bo);

    /**
     * Add work orderworkflow
     */
    Boolean insertByBo(WorkOrderSynthesisBo bo);

    /**
     * Update work orderworkflow
     */
    Boolean updateByBo(WorkOrderSynthesisBo bo);

    /**
     * Validate Batch delete work orderworkflowinfo
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    /**
     * id Query sub node
     */
    List<String> selectChildById(@Param("parentId") String parentId);

    List<WorkOrderSynthesisVo> queryListAll(String categoryName);

}
