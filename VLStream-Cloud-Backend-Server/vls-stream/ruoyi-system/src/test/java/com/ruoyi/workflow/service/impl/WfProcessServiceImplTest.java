/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.workflow.domain.vo.WfProcNodeVo;
import com.ruoyi.workflow.service.IWfCopyService;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
public class WfProcessServiceImplTest {

    /**
     * 并行多实例节点没有唯一办理人，顶层 assigneeName 应为空，人员明细由 assigneeInfoList 承载。
     */
    @Test
    public void historyProcNodeListShouldClearAssigneeNameForParallelMultiInstanceTask() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.selectUserNameById("user-1")).thenReturn("张三");
        when(userService.selectUserNameById("user-2")).thenReturn("李四");

        IWfCopyService wfCopyService = mock(IWfCopyService.class);
        when(wfCopyService.selectCopyUserIdByTaskId(anyString())).thenReturn(Collections.emptyList());

        WfProcessServiceImpl service = new WfProcessServiceImpl(
            null, null, userService, null, null, null, null, null, wfCopyService, null,
            null, null, null, null, null, null, null, null, null, null
        );

        HistoryService historyService = mock(HistoryService.class);
        HistoricActivityInstanceQuery activityQuery = mock(HistoricActivityInstanceQuery.class, Answers.RETURNS_SELF);
        List<HistoricActivityInstance> activityInstances = Arrays.asList(
            mockHistoricUserTask("task-1", "exec-1", "user-1"),
            mockHistoricUserTask("task-2", "exec-2", "user-2")
        );
        when(activityQuery.list()).thenReturn(activityInstances);
        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
        when(historyService.getHistoricIdentityLinksForTask(anyString())).thenReturn(Collections.emptyList());

        TaskService taskService = mock(TaskService.class);
        when(taskService.getProcessInstanceComments("proc-1")).thenReturn(Collections.emptyList());

        RepositoryService repositoryService = mock(RepositoryService.class);
        when(repositoryService.getBpmnModel("proc-def-1")).thenReturn(parallelMultiInstanceModel());

        setFlowServiceField(service, "historyService", historyService);
        setFlowServiceField(service, "taskService", taskService);
        setFlowServiceField(service, "repositoryService", repositoryService);

        HistoricProcessInstance historicProcIns = mock(HistoricProcessInstance.class);
        when(historicProcIns.getId()).thenReturn("proc-1");
        when(historicProcIns.getProcessDefinitionId()).thenReturn("proc-def-1");

        List<WfProcNodeVo> nodeList = service.historyProcNodeList(historicProcIns, new SysUser());

        assertEquals(1, nodeList.size());
        WfProcNodeVo node = nodeList.get(0);
        assertNull(node.getAssigneeName());
        assertEquals(2, node.getAssigneeInfoList().size());
        assertEquals("张三", node.getAssigneeInfoList().get(0).getAssigneeName());
        assertEquals("李四", node.getAssigneeInfoList().get(1).getAssigneeName());
    }

    /**
     * 构造一个已流转到同一用户任务的历史活动实例。
     */
    private HistoricActivityInstance mockHistoricUserTask(String taskId, String executionId, String assignee) {
        HistoricActivityInstance activityInstance = mock(HistoricActivityInstance.class);
        when(activityInstance.getProcessDefinitionId()).thenReturn("proc-def-1");
        when(activityInstance.getActivityId()).thenReturn("approve_1");
        when(activityInstance.getActivityName()).thenReturn("并行审批");
        when(activityInstance.getActivityType()).thenReturn(BpmnXMLConstants.ELEMENT_TASK_USER);
        when(activityInstance.getStartTime()).thenReturn(new Date(1000L));
        when(activityInstance.getEndTime()).thenReturn(null);
        when(activityInstance.getTransactionOrder()).thenReturn(1);
        when(activityInstance.getExecutionId()).thenReturn(executionId);
        when(activityInstance.getAssignee()).thenReturn(assignee);
        when(activityInstance.getTaskId()).thenReturn(taskId);
        return activityInstance;
    }

    /**
     * 构造 sequential=false 的用户任务模型，用于表示或签或并行会签节点。
     */
    private BpmnModel parallelMultiInstanceModel() {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId("Process_Test");

        UserTask userTask = new UserTask();
        userTask.setId("approve_1");
        MultiInstanceLoopCharacteristics loopCharacteristics = new MultiInstanceLoopCharacteristics();
        loopCharacteristics.setSequential(false);
        userTask.setLoopCharacteristics(loopCharacteristics);

        process.addFlowElement(userTask);
        bpmnModel.addProcess(process);
        return bpmnModel;
    }

    /**
     * 注入 FlowServiceFactory 中通过 @Resource 注入的 Flowable 服务依赖。
     */
    private void setFlowServiceField(WfProcessServiceImpl service, String fieldName, Object value) throws Exception {
        Field field = FlowServiceFactory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }
}
