/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import org.flowable.engine.history.HistoricProcessInstance;

import java.util.Map;

/**
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
public interface IWfInstanceService {

    /**
     * finishworkflow instance
     *
     * @param vo
     */
    void stopProcessInstance(WfTaskBo vo);

    /**
     * workflow instance
     *
     * @param state
     * @param instanceId workflow instance ID
     */
    void updateState(Integer state, String instanceId);

    /**
     * Delete workflow instance ID
     *
     * @param instanceId workflow instance ID
     * @param deleteReason Delete
     */
    void delete(String instanceId, String deleteReason, SysUser sysUser);

    /**
     * instanceIDQuery history instancedata
     *
     * @param processInstanceId
     * @return
     */
    HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId, SysUser sysUser);


    /**
     * Query workflow info
     * @param procInsId workflow instance ID
     * @param deployId workflow ID
     */
    Map<String, Object> queryDetailProcess(String procInsId, String deployId);
}
