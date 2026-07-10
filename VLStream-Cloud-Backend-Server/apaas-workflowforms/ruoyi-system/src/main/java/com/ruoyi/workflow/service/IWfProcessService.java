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
     * 查询可发起流程列表
     *
     * @param pageQuery 分页参数
     * @return
     */
    TableDataInfo<WfDefinitionVo> selectPageStartProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * 查询可发起流程列表
     */
    List<WfDefinitionVo> selectStartProcessList(ProcessQuery processQuery);

    /**
     * 查询我的流程列表
     * 
     * @param pageQuery 分页参数
     */
    TableDataInfo<Object> selectPageOwnProcessList(ProcessQuery processQuery, PageQuery pageQuery);

    /**
     * 查询所有流程
     *
     * @param processQuery
     * @param pageQuery
     * @param token
     * @return
     */
    TableDataInfo<WfTaskVo> selectPageAllProcessList(ProcessQuery processQuery, PageQuery pageQuery, String token);

    /**
     * 查询我的流程列表
     */
    List<WfTaskVo> selectOwnProcessList(ProcessQuery processQuery, boolean allFlag, String token);

    /**
     * 查询代办任务列表
     * 
     * @param pageQuery 分页参数
     */
    TableDataInfo<WfTaskVo> selectPageTodoProcessList(ProcessQuery processQuery, PageQuery pageQuery, SysUser sysUser);

    /**
     * 查询代办任务列表
     */
    List<WfTaskVo> selectTodoProcessList(ProcessQuery processQuery, String token);

    /**
     * 查询待签任务列表
     * 
     * @param pageQuery 分页参数
     */
    TableDataInfo<WfTaskVo> selectPageClaimProcessList(ProcessQuery processQuery, PageQuery pageQuery, SysUser sysUser);

    /**
     * 查询待签任务列表
     */
    List<WfTaskVo> selectClaimProcessList(ProcessQuery processQuery, SysUser sysUser);

    /**
     * 查询已办任务列表
     * 
     * @param pageQuery 分页参数
     */
    TableDataInfo<WfTaskVo> selectPageFinishedProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            SysUser sysUser);

    /**
     * 查询已办任务列表
     */
    List<WfTaskVo> selectFinishedProcessList(ProcessQuery processQuery, String token);

    /**
     * 查询流程部署关联表单信息
     *
     * @param definitionId 流程定义ID
     * @param deployId     部署ID
     * @param procInsId
     */
    Object selectFormContent(String definitionId, String deployId, String procInsId);

    /**
     * 启动流程实例
     */
    String startProcessByDefId(ProcessStartBo processStartBo, SysUser sysUser);

    /**
     * 根据流程实例id获取任务id
     *
     * @param procDefId 流程定义ID
     */
    String getTaskId(String procDefId, SysUser sysUser);

    // /**
    // * 通过DefinitionKey启动流程
    // * @param procDefKey 流程定义Key
    // * @param variables 扩展参数
    // */
    // void startProcessByDefKey(String procDefKey, Map<String, Object> variables);

    /**
     * 删除流程实例
     */
    void deleteProcessByIds(String[] instanceIds);

    /**
     * 读取xml文件
     * 
     * @param processDefId 流程定义ID
     */
    String queryBpmnXmlById(String processDefId);

    /**
     * 读取Json文件
     *
     * @param processDefId 流程定义ID
     * @param sysUser
     */
    String queryBpmnJsonById(String processDefId, SysUser sysUser);

    /**
     * 查询流程任务详情信息
     *
     * @param procInsId          流程实例ID
     * @param taskIds            任务ID
     * @param sysUser
     * @param includeApproverIds 是否需要返回所有审批人ID列表
     */
    WfDetailVo queryProcessDetail(String procInsId, String taskIds, SysUser sysUser, Boolean includeApproverIds);

    List<WfUserTaskInfoVo> getAllUserInfo(String processDefKey);

    List<WfAttachment> getPDF(String procInstId);

    WfLastHisTaskInfoVo getLatestHisTaskInfo(String procInstId);

    WfDefAndDepVo getDefIdAndDepIdByProcKey(String processKey, String token);

    List<WfProcNodeVo> historyProcNodeList(HistoricProcessInstance historicProcIns, SysUser sysUser);
}
