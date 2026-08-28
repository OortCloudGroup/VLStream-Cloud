/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import com.ruoyi.workflow.convert.listeners.MessageNotificationListener;
import com.ruoyi.workflow.convert.listeners.MessageTimeoutHandler;
import org.flowable.bpmn.model.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * MessageNode
 */
public class MessageNodeTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DelegateTask delegateTask;

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private MessageNotificationListener messageNotificationListener;

    @InjectMocks
    private MessageTimeoutHandler messageTimeoutHandler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMessageNodeConvert() {
        // data
        MessageNode messageNode = new MessageNode();
        messageNode.setId("msgNode1");
        messageNode.setNodeName("通知节点");
        messageNode.setUsers(new ArrayList<String>() {
            {
                add("user1");
            }
        });
        messageNode.setPriority(1);
        messageNode.setData("testData");
        messageNode.setTimeoutMinutes(10);
        messageNode.setTimeoutAction(1); // notification
        messageNode.setRepeatCount(3);

        // Set sub node
        ApprovalNode childNode = new ApprovalNode();
        childNode.setId("childNode1");
        messageNode.setChildNode(childNode);

        // Execute Convert
        List<FlowElement> elements = messageNode.convert();

        //
        assertNotNull(elements);
        assertTrue(elements.size() > 0);

        // UserTask
        UserTask userTask = (UserTask) elements.stream()
                .filter(e -> e instanceof UserTask && "msgNode1".equals(e.getId()))
                .findFirst().orElse(null);
        assertNotNull(userTask);
        assertEquals("user1", userTask.getAssignee());

        // Listener
        assertTrue(userTask.getTaskListeners().stream()
                .anyMatch(l -> MessageNotificationListener.class.getName().equals(l.getImplementation())));

        // BoundaryEvent
        BoundaryEvent boundaryEvent = (BoundaryEvent) elements.stream()
                .filter(e -> e instanceof BoundaryEvent)
                .findFirst().orElse(null);
        assertNotNull(boundaryEvent);
        assertEquals("msgNode1", boundaryEvent.getAttachedToRefId());

        // ServiceTask (Handler)
        ServiceTask serviceTask = (ServiceTask) elements.stream()
                .filter(e -> e instanceof ServiceTask && e.getId().contains("timeoutHandler"))
                .findFirst().orElse(null);
        assertNotNull(serviceTask);
        assertEquals(MessageTimeoutHandler.class.getName(), serviceTask.getImplementation());

        // Gateway ( to timeoutAction=1)
        ExclusiveGateway gateway = (ExclusiveGateway) elements.stream()
                .filter(e -> e instanceof ExclusiveGateway)
                .findFirst().orElse(null);
        assertNotNull(gateway);
    }

    @Test
    public void testMessageTimeoutHandler_Repeat() {
        //
        when(delegateExecution.getCurrentActivityId()).thenReturn("serviceTask1");
        when(delegateExecution.getVariable("messageNode_retryCount")).thenReturn(1); // current 1

        // parameter ( Mock FixedValue)
        // , assuming Handler correct parameter
        // FixedValue Mock, main need to Handler
        // if need to , need to Mock FixedValue.getValue(execution)

        // assuming Update Handler ,
        // Mock
    }

    @Test
    public void testGetAssignee_SchemeC() {
        MessageNode messageNode = new MessageNode();
        messageNode.setId("msgNode1");

        // Case 1: Explicit Users
        messageNode.setUsers(new ArrayList<String>() {
            {
                add("user1");
            }
        });
        // convert user1
        // getAssignee is , convert
        List<FlowElement> elements = messageNode.convert();
        UserTask userTask = (UserTask) elements.get(0);
        assertEquals("user1", userTask.getAssignee());

        // Case 2: Fallback to Child Node
        messageNode.setUsers(null);
        ApprovalNode child = new ApprovalNode();
        child.setUsers(new ArrayList<String>() {
            {
                add("childUser");
            }
        });
        messageNode.setChildNode(child);

        elements = messageNode.convert();
        userTask = (UserTask) elements.get(0);
        assertEquals("childUser", userTask.getAssignee());
    }
}
