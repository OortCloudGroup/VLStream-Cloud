package com.ruoyi.workflow.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class WorkflowTaskBindingTest {
    @Test
    void rejectsMissingOrForgedProcessIdsAndAcceptsTheActualOne() {
        assertThrows(ServiceException.class, () -> WorkflowTaskBinding.requireSameInstance("real", "other"));
        assertThrows(ServiceException.class, () -> WorkflowTaskBinding.requireSameInstance("real", null));
        assertThrows(ServiceException.class, () -> WorkflowTaskBinding.requireSameInstance(null, null));
        assertDoesNotThrow(() -> WorkflowTaskBinding.requireSameInstance("real", "real"));
        WfTaskBo optional = new WfTaskBo();
        WorkflowTaskBinding.bindOptionalInstance("real", optional);
        assertEquals("real", optional.getProcInsId());
        optional.setProcInsId("other");
        assertThrows(ServiceException.class, () -> WorkflowTaskBinding.bindOptionalInstance("real", optional));
    }

    @Test
    void scheduledStartDoesNotRequireAnHttpRequest() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        assertNull(WfProcessServiceImpl.currentRequestApplicationId());
    }

    @Test
    void rejectOperationChecksTheQueriedTasksRealInstanceBeforeAnyWorkflowMutation() throws Exception {
        WfTaskServiceImpl service = new WfTaskServiceImpl(null, null, null, null, null, null, null, null);
        TaskService tasks = mock(TaskService.class);
        RuntimeService runtime = mock(RuntimeService.class);
        TaskQuery query = mock(TaskQuery.class, RETURNS_SELF);
        Task task = mock(Task.class);
        when(tasks.createTaskQuery()).thenReturn(query);
        doReturn(task).when(query).singleResult();
        when(task.getProcessInstanceId()).thenReturn("real");
        setFactoryField(service, "taskService", tasks);
        setFactoryField(service, "runtimeService", runtime);
        SysUser user = new SysUser();
        user.setTenantId("tenant-a");
        WfTaskBo input = new WfTaskBo();
        input.setTaskId("task");
        input.setProcInsId("other");
        assertThrows(ServiceException.class, () -> service.taskReject(input, user));
        verifyNoInteractions(runtime);
        verify(tasks, never()).addComment(anyString(), anyString(), anyString(), anyString());

        input.setProcInsId("real");
        when(task.isSuspended()).thenReturn(true);
        RuntimeException suspended = assertThrows(RuntimeException.class, () -> service.taskReject(input, user));
        assertEquals("任务处于挂起状态", suspended.getMessage());
    }

    @Test
    void detailRejectsATaskBelongingToAnotherInstanceBeforeReadingTaskForm() throws Exception {
        WfProcessServiceImpl service = new WfProcessServiceImpl(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null);
        HistoryService history = mock(HistoryService.class);
        HistoricProcessInstanceQuery processes = mock(HistoricProcessInstanceQuery.class, RETURNS_SELF);
        HistoricTaskInstanceQuery tasks = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        HistoricProcessInstance process = mock(HistoricProcessInstance.class);
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(history.createHistoricProcessInstanceQuery()).thenReturn(processes);
        doReturn(process).when(processes).singleResult();
        when(process.getId()).thenReturn("real");
        when(history.createHistoricTaskInstanceQuery()).thenReturn(tasks);
        when(tasks.list()).thenReturn(Collections.singletonList(task));
        when(task.getProcessInstanceId()).thenReturn("other");
        setFactoryField(service, "historyService", history);
        SysUser user = new SysUser();
        user.setTenantId("tenant-a");
        assertThrows(ServiceException.class, () -> service.queryProcessDetail("real", "task", user, false));
        verify(process, never()).getDeploymentId();
    }

    private static void setFactoryField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = FlowServiceFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
