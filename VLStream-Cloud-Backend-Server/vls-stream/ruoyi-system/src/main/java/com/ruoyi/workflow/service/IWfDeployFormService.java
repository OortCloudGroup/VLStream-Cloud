/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.workflow.domain.WfDeployForm;
import com.ruoyi.workflow.domain.vo.WfFormVo;
import org.flowable.bpmn.model.BpmnModel;

/**
 * workflow instance formServiceinterface
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
public interface IWfDeployFormService {

    /**
     * Add workflow instance form
     *
     * @param wfDeployForm workflow instance form
     * @return
     */
    int insertWfDeployForm(WfDeployForm wfDeployForm);

    /**
     * workflow instance form
     * @param deployId ID
     * @param bpmnModel bpmnModelobject
     * @return
     */
    boolean saveInternalDeployForm(String deployId, BpmnModel bpmnModel);

    /**
     * Query workflow form
     *
     * @param deployId
     * @return
     */
    @Deprecated
    WfFormVo selectDeployFormByDeployId(String deployId);
}
