/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.flowable.common.constant.ProcessConstants;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.UserTask;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ApprovalNotificationListener notification recipient tests.
 */
@Tag("dev")
public class ApprovalNotificationListenerTest {

    /**
     * Parallel multi-instance tasks after the first one should not send another notice.
     */
    @Test
    public void skipsParallelMultiInstanceNotificationAfterFirstLoop() {
        ApprovalNotificationListener listener = new ApprovalNotificationListener();
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getVariableLocal("loopCounter")).thenReturn(1);

        UserTask userTask = buildMultiInstanceUserTask(false, Arrays.asList("user-1", "user-2"));

        assertTrue(listener.shouldSkipParallelMultiInstanceNotification(delegateTask, userTask));
    }

    /**
     * The first parallel multi-instance task should send one notice to all generated assignees.
     */
    @Test
    public void resolvesParallelMultiInstanceRecipientsFromUserTaskCandidates() {
        ApprovalNotificationListener listener = new ApprovalNotificationListener();
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getAssignee()).thenReturn("user-1");
        when(delegateTask.getVariableLocal("loopCounter")).thenReturn(0);

        UserTask userTask = buildMultiInstanceUserTask(false, Arrays.asList("user-1", "user-2", "user-3"));

        List<String> assigneeIds = listener.resolveNotificationAssigneeIds(delegateTask, userTask);

        assertEquals(Arrays.asList("user-1", "user-2", "user-3"), assigneeIds);
        assertFalse(listener.shouldSkipParallelMultiInstanceNotification(delegateTask, userTask));
    }

    /**
     * Sequential multi-instance tasks should keep the current per-assignee notice behavior.
     */
    @Test
    public void keepsSequentialMultiInstanceNotificationOnCurrentAssignee() {
        ApprovalNotificationListener listener = new ApprovalNotificationListener();
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getAssignee()).thenReturn("user-1");
        when(delegateTask.getVariableLocal("loopCounter")).thenReturn(1);

        UserTask userTask = buildMultiInstanceUserTask(true, Arrays.asList("user-1", "user-2"));

        List<String> assigneeIds = listener.resolveNotificationAssigneeIds(delegateTask, userTask);

        assertEquals(Arrays.asList("user-1"), assigneeIds);
        assertFalse(listener.shouldSkipParallelMultiInstanceNotification(delegateTask, userTask));
    }

    /**
     * If Flowable does not expose loopCounter on the task, only the first recipient should send.
     */
    @Test
    public void skipsParallelMultiInstanceWhenLoopCounterUnavailableAndAssigneeIsNotFirstCandidate() {
        ApprovalNotificationListener listener = new ApprovalNotificationListener();
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getAssignee()).thenReturn("user-2");

        UserTask userTask = buildMultiInstanceUserTask(false, Arrays.asList("user-1", "user-2"));

        assertTrue(listener.shouldSkipParallelMultiInstanceNotification(delegateTask, userTask));
    }

    /**
     * Creates a user task with multi-instance settings for listener decisions.
     */
    private UserTask buildMultiInstanceUserTask(boolean sequential, List<String> candidateUsers) {
        UserTask userTask = new UserTask();
        MultiInstanceLoopCharacteristics loopCharacteristics = new MultiInstanceLoopCharacteristics();
        loopCharacteristics.setSequential(sequential);
        userTask.setLoopCharacteristics(loopCharacteristics);
        userTask.setCandidateUsers(candidateUsers);

        ExtensionAttribute dataType = new ExtensionAttribute();
        dataType.setName(ProcessConstants.PROCESS_CUSTOM_DATA_TYPE);
        dataType.setValue("USERS");
        Map<String, List<ExtensionAttribute>> attributes = new HashMap<>();
        attributes.put(ProcessConstants.NAMASPASE, Arrays.asList(dataType));
        userTask.setAttributes(attributes);
        return userTask;
    }
}
