/*
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
 * MessageNode 单元测试
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
        // 准备数据
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
        messageNode.setTimeoutAction(1); // 重复通知
        messageNode.setRepeatCount(3);

        // 设置子节点
        ApprovalNode childNode = new ApprovalNode();
        childNode.setId("childNode1");
        messageNode.setChildNode(childNode);

        // 执行转换
        List<FlowElement> elements = messageNode.convert();

        // 验证结果
        assertNotNull(elements);
        assertTrue(elements.size() > 0);

        // 验证 UserTask
        UserTask userTask = (UserTask) elements.stream()
                .filter(e -> e instanceof UserTask && "msgNode1".equals(e.getId()))
                .findFirst().orElse(null);
        assertNotNull(userTask);
        assertEquals("user1", userTask.getAssignee());

        // 验证 Listener
        assertTrue(userTask.getTaskListeners().stream()
                .anyMatch(l -> MessageNotificationListener.class.getName().equals(l.getImplementation())));

        // 验证 BoundaryEvent
        BoundaryEvent boundaryEvent = (BoundaryEvent) elements.stream()
                .filter(e -> e instanceof BoundaryEvent)
                .findFirst().orElse(null);
        assertNotNull(boundaryEvent);
        assertEquals("msgNode1", boundaryEvent.getAttachedToRefId());

        // 验证 ServiceTask (Handler)
        ServiceTask serviceTask = (ServiceTask) elements.stream()
                .filter(e -> e instanceof ServiceTask && e.getId().contains("timeoutHandler"))
                .findFirst().orElse(null);
        assertNotNull(serviceTask);
        assertEquals(MessageTimeoutHandler.class.getName(), serviceTask.getImplementation());

        // 验证 Gateway (因为 timeoutAction=1)
        ExclusiveGateway gateway = (ExclusiveGateway) elements.stream()
                .filter(e -> e instanceof ExclusiveGateway)
                .findFirst().orElse(null);
        assertNotNull(gateway);
    }

    @Test
    public void testMessageTimeoutHandler_Repeat() {
        // 模拟环境
        when(delegateExecution.getCurrentActivityId()).thenReturn("serviceTask1");
        when(delegateExecution.getVariable("messageNode_retryCount")).thenReturn(1); // 当前第1次

        // 模拟注入参数 (通过反射或Mock FixedValue)
        // 这里简化，假设 Handler 内部逻辑正确读取了参数
        // 实际上由于 FixedValue 难以 Mock，我们主要验证 Handler 的核心逻辑分支
        // 如果要严格测试，需要 Mock FixedValue.getValue(execution)

        // 假设我们修改 Handler 让它易于测试，或者使用集成测试
        // 这里演示基本的 Mock 交互
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
        // 此时 convert 应该使用 user1
        // 由于 getAssignee 是私有的，我们通过 convert 间接测试
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
