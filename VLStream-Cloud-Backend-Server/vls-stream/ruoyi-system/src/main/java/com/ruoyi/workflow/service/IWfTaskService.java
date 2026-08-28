/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.WfSavePdfBo;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
public interface IWfTaskService {
    // Push
    void sendMessage(boolean isPushMessage, String userIds);

    void unifiedMessageSend(boolean isPushMessage, Map<String, Object> params);

    /**
     * approvaltask
     *
     * @param task parameter
     */
    void complete(WfTaskBo task);
    /**
     * approvaltask
     *
     * @param task parameter
     */
    //  void complete(WfTaskBo task);

    /**
     * task
     *
     * @param taskBo
     * @param user
     */
    void taskReject(WfTaskBo taskBo,SysUser user);


    /**
     *
     * task
     * @param bo parameter
     */
    void taskReturn(WfTaskBo bo,SysUser sysUser);

    /**
     * Get all node
     *
     * @param bo
     * @return
     */
    List<FlowElement> findReturnTaskList(WfTaskBo bo);

    /**
     * Delete task
     *
     * @param bo parameter
     */
    void deleteTask(WfTaskBo bo);

    /**
     * / task
     *
     * @param bo parameter
     */
    void claim(WfTaskBo bo);

    /**
     * / task
     *
     * @param bo parameter
     */
    void unClaim(WfTaskBo bo);

    /**
     * task
     *
     * @param bo parameter
     */
    //  void delegateTask(WfTaskBo bo);

    /**
     * task
     *
     * @param bo parameter
     */
    void delegateTask(WfTaskBo bo);


    /**
     * task
     *
     * @param bo parameter
     */
    void transferTask(WfTaskBo bo);

    /**
     *
     *
     * @param bo
     * @return
     */
    void stopProcess(WfTaskBo bo);

    /**
     * workflow
     *
     * @param bo
     * @return
     */
    void revokeProcess(WfTaskBo bo);

    /**
     * Get workflow
     *
     * @param processId
     * @return
     */
    InputStream diagram(String processId);

    /**
     * Get workflow variable
     *
     * @param taskId taskID
     * @return workflow variable
     */
    Map<String, Object> getProcessVariables(String taskId);

//    /**
// * task
// * @param processInstance workflow instance
// * @param variables workflowparameter
//     */
//    void startFirstTask(ProcessInstance processInstance, Map<String, Object> variables);

    /**
     * task
     *
     * @param processInstance workflow instance
     * @param variables workflowparameter
     */
    void startFirstTask(ProcessInstance processInstance, Map<String, Object> variables,SysUser sysUser);

    void savePDF(WfSavePdfBo bo);

    List<String> getApproverIds(String procInstId);

    /**
     * Get workflow
     *
     * @param historicProcessInstance workflow instance
     * @param sysUser current user
     * @return workflow
     */
    String getProcessName(HistoricProcessInstance historicProcessInstance, SysUser sysUser);

    /**
     * Build
     *
     * @param task current task
     * @param receive_userId id
     * @param notifyAllSteps whether Push
     */
    void buildAndSendUnifiedMessage(Task task, String receive_userId, boolean notifyAllSteps,SysUser sysUser);

    /**
     *
     */
    void addSignTask(WfTaskBo bo);
    /**
     *
     */
    void subSignTask(WfTaskBo bo);
}
