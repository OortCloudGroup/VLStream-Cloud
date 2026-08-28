/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.workflow.domain.WfAttachment;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.domain.vo.*;
import org.flowable.engine.history.HistoricProcessInstance;

import java.util.List;

/**
 * @author KonBAI
 * @createTime 2022/3/24 18:57
 */
public interface IWfProcessService {

    /**
     * Query workflow list
     *
     * @param pageQuery parameter
     * @return
     */
    TableDataInfo<WfDefinitionVo> selectPageStartProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * Query workflow list
     */
    List<WfDefinitionVo> selectStartProcessList(ProcessQuery processQuery);

    /**
     * Query workflow list
     *
     * @param pageQuery parameter
     */
    TableDataInfo<Object> selectPageOwnProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * Query all workflow
     *
     * @param processQuery
     * @param pageQuery
     * @param token
     * @return
     */
    TableDataInfo<WfTaskVo> selectPageAllProcessList(ProcessQuery processQuery, PageQuery pageQuery, String token);

    /**
     * Query workflow list
     */
    List<WfTaskVo> selectOwnProcessList(ProcessQuery processQuery, boolean allFlag, String token);

    /**
     * Query task list
     *
     * @param pageQuery parameter
     */
    TableDataInfo<WfTaskVo> selectPageTodoProcessList(ProcessQuery processQuery, PageQuery pageQuery, SysUser sysUser);

    /**
     * Query task list
     */
    List<WfTaskVo> selectTodoProcessList(ProcessQuery processQuery, String token);

    /**
     * Query task list
     *
     * @param pageQuery parameter
     */
    TableDataInfo<WfTaskVo> selectPageClaimProcessList(ProcessQuery processQuery, PageQuery pageQuery, SysUser sysUser);

    /**
     * Query task list
     */
    List<WfTaskVo> selectClaimProcessList(ProcessQuery processQuery, SysUser sysUser);

    /**
     * Query already task list
     *
     * @param pageQuery parameter
     */
    TableDataInfo<WfTaskVo> selectPageFinishedProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            SysUser sysUser);

    /**
     * Query already task list
     */
    List<WfTaskVo> selectFinishedProcessList(ProcessQuery processQuery, String token);

    /**
     * Query workflow forminfo
     *
     * @param definitionId workflow definition ID
     * @param deployId ID
     * @param procInsId
     */
    Object selectFormContent(String definitionId, String deployId, String procInsId);

    /**
     * workflow instance
     */
    String startProcessByDefId(ProcessStartBo processStartBo, SysUser sysUser);

    /**
     * workflow instanceidGet taskid
     *
     * @param procDefId workflow definition ID
     */
    String getTaskId(String procDefId, SysUser sysUser);

    // /**
    // * DefinitionKey workflow
    // * @param procDefKey workflow definitionKey
    // * @param variables parameter
    // */
    // void startProcessByDefKey(String procDefKey, Map<String, Object> variables);

    /**
     * Delete workflow instance
     */
    void deleteProcessByIds(String[] instanceIds);

    /**
     * xml
     *
     * @param processDefId workflow definition ID
     */
    String queryBpmnXmlById(String processDefId);

    /**
     * Json
     *
     * @param processDefId workflow definition ID
     * @param sysUser
     */
    String queryBpmnJsonById(String processDefId, SysUser sysUser);

    /**
     * Query workflowtask info
     *
     * @param procInsId workflow instance ID
     * @param taskIds taskID
     * @param sysUser
     * @param includeApproverIds whether need to all approverID
     */
    WfDetailVo queryProcessDetail(String procInsId, String taskIds, SysUser sysUser, Boolean includeApproverIds);

    List<WfUserTaskInfoVo> getAllUserInfo(String processDefKey);

    List<WfAttachment> getPDF(String procInstId);

    WfLastHisTaskInfoVo getLatestHisTaskInfo(String procInstId);

    WfDefAndDepVo getDefIdAndDepIdByProcKey(String processKey, String token);

    List<WfProcNodeVo> historyProcNodeList(HistoricProcessInstance historicProcIns, SysUser sysUser);
}
