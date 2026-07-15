package com.ruoyi.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the former location-service work-order callback inside the Java process.
 */
@Service
public class LocationTaskWorkflowCallbackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationTaskWorkflowCallbackService.class);
    private static final String INTERNAL_CALLBACK = "internal://location-task/audit-workorder-callback";
    private static final String LEGACY_CALLBACK_PATH = "/v1/audit_workorder_callback";
    private static final String APPROVED_MESSAGE = "通过";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;

    /**
     * Build the callback handler from the application's primary datasource and Flowable services.
     */
    public LocationTaskWorkflowCallbackService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                                               TaskService taskService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.taskService = taskService;
    }

    /**
     * Recognize both new internal callback identifiers and legacy deployed Go callback URLs.
     */
    public boolean supports(String callbackUrl) {
        return callbackUrl != null
            && (INTERNAL_CALLBACK.equals(callbackUrl) || callbackUrl.contains(LEGACY_CALLBACK_PATH));
    }

    /**
     * Apply an approved work-order task result to its event without making an HTTP request.
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleApprovedTask(DelegateTask delegateTask, HistoricProcessInstance processInstance) {
        if (delegateTask == null || processInstance == null || processInstance.getProcessVariables() == null) {
            LOGGER.warn("事件工单回调缺少流程上下文，已跳过");
            return;
        }

        Map<String, Object> variables = processInstance.getProcessVariables();
        String eventNumber = stringValue(variables.get("no"));
        String workOrderId = stringValue(variables.get("workorderId"));
        if (eventNumber.isEmpty() || workOrderId.isEmpty()) {
            LOGGER.warn("事件工单回调缺少事件编号或工单ID，processInstanceId={}", processInstance.getId());
            return;
        }

        Comment approvalComment = latestComment(delegateTask.getId());
        if (approvalComment == null || !APPROVED_MESSAGE.equals(approvalComment.getFullMessage())) {
            return;
        }

        List<Map<String, Object>> events = jdbcTemplate.queryForList(
            "SELECT id,`no`,JSON_UNQUOTE(JSON_EXTRACT(data,'$.work_order_data.workorderId')) "
                + "AS event_workorder_id FROM oort_task_event "
                + "WHERE CONCAT(id,'-',`no`) = ? AND work_order_status = 1 AND deleted_at = 0 LIMIT 1",
            eventNumber);
        if (events.isEmpty()) {
            LOGGER.warn("事件工单回调未找到事件，eventNumber={}", eventNumber);
            return;
        }
        Map<String, Object> event = events.get(0);
        if (!workOrderId.equals(stringValue(event.get("event_workorder_id")))) {
            LOGGER.warn("事件工单回调ID不匹配，eventNumber={}", eventNumber);
            return;
        }

        Map<String, Object> callback = callbackData(
            eventNumber, workOrderId, delegateTask, approvalComment);
        String callbackJson = toJson(callback);
        jdbcTemplate.update(
            "UPDATE oort_task_event SET data = IF(status = 1,"
                + "JSON_SET(data,'$.work_order_call_back',CAST(? AS JSON)),"
                + "JSON_SET(data,'$.finish_at',DATE_FORMAT(NOW(),'%Y-%m-%d %H:%i:%s'),"
                + "'$.work_order_call_back',CAST(? AS JSON))),status = 1,updated_at = NOW() "
                + "WHERE id = ? AND `no` = ?",
            callbackJson, callbackJson, event.get("id"), event.get("no"));
    }

    /**
     * Return the newest Flowable comment created before the task completion listener runs.
     */
    private Comment latestComment(String taskId) {
        List<Comment> comments = new ArrayList<Comment>(taskService.getTaskComments(taskId));
        if (comments.isEmpty()) {
            return null;
        }
        Collections.sort(comments, new Comparator<Comment>() {
            /**
             * Sort comments from newest to oldest, treating a missing timestamp as the epoch.
             */
            @Override
            public int compare(Comment left, Comment right) {
                return commentTime(right).compareTo(commentTime(left));
            }
        });
        return comments.get(0);
    }

    /**
     * Normalize a potentially missing Flowable comment timestamp for ordering.
     */
    private static Date commentTime(Comment comment) {
        return comment == null || comment.getTime() == null ? new Date(0L) : comment.getTime();
    }

    /**
     * Build the JSON object previously generated by the Go callback endpoint.
     */
    private Map<String, Object> callbackData(String eventNumber, String workOrderId,
                                             DelegateTask delegateTask, Comment comment) {
        LinkedHashMap<String, Object> callback = new LinkedHashMap<String, Object>();
        callback.put("no", eventNumber);
        callback.put("assigneeId", stringValue(delegateTask.getAssignee()));
        callback.put("assigneeName", assigneeName(delegateTask.getAssignee()));
        callback.put("fullMessage", comment.getFullMessage());
        callback.put("workorderId", workOrderId);
        callback.put("time", commentTime(comment).getTime());
        return callback;
    }

    /**
     * Resolve the canonical display name of the task assignee from sys_user.
     */
    private String assigneeName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "";
        }
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT COALESCE(NULLIF(nick_name,''),user_name) AS display_name "
                + "FROM sys_user WHERE user_id = ? AND COALESCE(del_flag,'0') = '0' LIMIT 1",
            userId);
        return users.isEmpty() ? "" : stringValue(users.get(0).get("display_name"));
    }

    /**
     * Serialize callback data for storage in the event JSON document.
     */
    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("事件工单回调数据序列化失败", exception);
        }
    }

    /**
     * Convert nullable database or Flowable values to strings.
     */
    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
