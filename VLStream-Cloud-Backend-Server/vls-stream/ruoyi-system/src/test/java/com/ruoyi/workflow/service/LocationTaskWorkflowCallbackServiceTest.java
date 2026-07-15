package com.ruoyi.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class LocationTaskWorkflowCallbackServiceTest {

    /**
     * Verify that new internal identifiers and already deployed legacy URLs are both handled in-process.
     */
    @Test
    void recognizesInternalAndLegacyCallbacks() {
        LocationTaskWorkflowCallbackService service = service(
            mock(JdbcTemplate.class), mock(TaskService.class));

        assertTrue(service.supports("internal://location-task/audit-workorder-callback"));
        assertTrue(service.supports("http://apaas-location-service/v1/audit_workorder_callback"));
        assertFalse(service.supports("https://example.invalid/callback"));
    }

    /**
     * Verify that an approved task writes the callback and completion state to its matching event.
     */
    @Test
    void approvedTaskUpdatesMatchingEvent() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TaskService taskService = mock(TaskService.class);
        DelegateTask delegateTask = mock(DelegateTask.class);
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        Comment comment = mock(Comment.class);

        when(delegateTask.getId()).thenReturn("task-1");
        when(delegateTask.getAssignee()).thenReturn("user-1");
        when(comment.getFullMessage()).thenReturn("通过");
        when(comment.getTime()).thenReturn(new Date(1234L));
        when(taskService.getTaskComments("task-1")).thenReturn(Collections.singletonList(comment));
        Map<String, Object> variables = new LinkedHashMap<String, Object>();
        variables.put("no", "event-20260714-7");
        variables.put("workorderId", "work-order-type");
        when(processInstance.getProcessVariables()).thenReturn(variables);
        when(processInstance.getId()).thenReturn("process-1");

        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("id", "event-20260714");
        event.put("no", 7);
        event.put("event_workorder_id", "work-order-type");
        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("display_name", "测试用户");
        List<Map<String, Object>> events = Collections.singletonList(event);
        List<Map<String, Object>> users = Collections.singletonList(user);
        doReturn(events).doReturn(users).when(jdbcTemplate)
            .queryForList(anyString(), anyString());

        service(jdbcTemplate, taskService).handleApprovedTask(delegateTask, processInstance);

        verify(jdbcTemplate).queryForList(contains("CONCAT(id,'-',`no`)"), eq("event-20260714-7"));
        verify(jdbcTemplate).update(anyString(), anyString(), anyString(), any(), any());
    }

    /**
     * Build a callback service with a real JSON mapper and mocked infrastructure.
     */
    private static LocationTaskWorkflowCallbackService service(JdbcTemplate jdbcTemplate,
                                                               TaskService taskService) {
        return new LocationTaskWorkflowCallbackService(jdbcTemplate, new ObjectMapper(), taskService);
    }
}
