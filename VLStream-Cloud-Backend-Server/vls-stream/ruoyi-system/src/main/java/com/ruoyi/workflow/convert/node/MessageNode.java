/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.google.gson.Gson;
import com.ruoyi.workflow.convert.listeners.ClearTimeoutJobListener;
import com.ruoyi.workflow.convert.listeners.MessageNotificationListener;
import com.ruoyi.workflow.convert.listeners.MessageTimeoutHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @description: notificationnode
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Component
public class MessageNode extends Node {

    // notificationobject (user ID )
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> users;

    // notification
    private Integer priority;

    // notificationdata
    private String data;

    // notification array, : [0, 1, 2]
    private List<Integer> channelTypes;

    // ( )
    private Integer timeoutMinutes;

    // : 1- notification, 2- node, 3-
    private Integer timeoutAction;

    // notification (timeoutAction=1 )
    private Integer repeatCount;

    // ( ), 15
    private Integer verificationInterval = 15;

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();

        // 1. UserTask
        UserTask userTask = new UserTask();
        userTask.setId(this.getId());
        userTask.setName(this.getNodeName());

        // 2. Set Assignee (notificationobject)
        String assignee = getAssignee();
        if (StringUtils.isNotBlank(assignee)) {
            // before , Query (taskAssignee is etc. value Query )
            userTask.setAssignee("MESSAGENODE_" + assignee);
        } else {
            // if method assignee, can need to in Process
            // Set is empty, after
            throw new RuntimeException("MessageNode: 无法确定通知对象，请配置 users 或确保下一个节点为审批节点且已配置审批人");
        }

        // 3. CREATE listener: notification
        FlowableListener notificationListener = new FlowableListener();
        notificationListener.setEvent(TaskListener.EVENTNAME_CREATE);
        notificationListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        notificationListener.setImplementation(MessageNotificationListener.class.getName());

        // parameter
        if (this.priority != null) {
            FieldExtension priorityField = new FieldExtension();
            priorityField.setFieldName("priority");
            priorityField.setStringValue(String.valueOf(this.priority));
            notificationListener.getFieldExtensions().add(priorityField);
        }

        if (StringUtils.isNotBlank(this.data)) {
            FieldExtension dataField = new FieldExtension();
            dataField.setFieldName("data");
            dataField.setStringValue(this.data);
            notificationListener.getFieldExtensions().add(dataField);
        }

        // parameter
        FieldExtension intervalField = new FieldExtension();
        intervalField.setFieldName("verificationInterval");
        intervalField
                .setStringValue(String.valueOf(this.verificationInterval != null ? this.verificationInterval : 15));
        notificationListener.getFieldExtensions().add(intervalField);

        // channelTypes parameter
        if (this.channelTypes != null && !this.channelTypes.isEmpty()) {
            FieldExtension channelTypesField = new FieldExtension();
            channelTypesField.setFieldName("channelTypes");
            // Gson List to JSON
            channelTypesField.setStringValue(new Gson().toJson(this.channelTypes));
            notificationListener.getFieldExtensions().add(channelTypesField);
        }

        userTask.getTaskListeners().add(notificationListener);

        // 4. COMPLETE listener: (if )
        if (this.timeoutMinutes != null && this.timeoutMinutes > 0) {
            FlowableListener clearListener = new FlowableListener();
            clearListener.setEvent(TaskListener.EVENTNAME_COMPLETE);
            clearListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            clearListener.setImplementation(ClearTimeoutJobListener.class.getName());
            userTask.getTaskListeners().add(clearListener);

            // 5. BoundaryEvent: Process
            BoundaryEvent boundaryEvent = new BoundaryEvent();
            boundaryEvent.setId(userTask.getId() + "_timeout");
            boundaryEvent.setAttachedToRef(userTask);
            boundaryEvent.setCancelActivity(true); // in , after UserTask

            TimerEventDefinition timerDef = new TimerEventDefinition();
            timerDef.setTimeDuration("PT" + this.timeoutMinutes + "M"); //
            boundaryEvent.getEventDefinitions().add(timerDef);

            // 6. Process ServiceTask
            ServiceTask timeoutServiceTask = new ServiceTask();
            timeoutServiceTask.setId(userTask.getId() + "_timeoutHandler");
            timeoutServiceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            timeoutServiceTask.setImplementation(MessageTimeoutHandler.class.getName());

            // parameter
            if (this.timeoutAction != null) {
                FieldExtension actionField = new FieldExtension();
                actionField.setFieldName("timeoutAction");
                actionField.setStringValue(String.valueOf(this.timeoutAction));
                timeoutServiceTask.getFieldExtensions().add(actionField);
            }
            if (this.repeatCount != null) {
                FieldExtension repeatField = new FieldExtension();
                repeatField.setFieldName("repeatCount");
                repeatField.setStringValue(String.valueOf(this.repeatCount));
                timeoutServiceTask.getFieldExtensions().add(repeatField);
            }
            // notificationrelatedparameter notification
            if (this.priority != null) {
                FieldExtension priorityField = new FieldExtension();
                priorityField.setFieldName("priority");
                priorityField.setStringValue(String.valueOf(this.priority));
                timeoutServiceTask.getFieldExtensions().add(priorityField);
            }
            if (StringUtils.isNotBlank(this.data)) {
                FieldExtension dataField = new FieldExtension();
                dataField.setFieldName("data");
                dataField.setStringValue(this.data);
                timeoutServiceTask.getFieldExtensions().add(dataField);
            }
            // channelTypes parameter
            if (this.channelTypes != null && !this.channelTypes.isEmpty()) {
                FieldExtension channelTypesField = new FieldExtension();
                channelTypesField.setFieldName("channelTypes");
                channelTypesField.setStringValue(new Gson().toJson(this.channelTypes));
                timeoutServiceTask.getFieldExtensions().add(channelTypesField);
            }

            // 7. : BoundaryEvent -> ServiceTask
            SequenceFlow timeoutFlow = new SequenceFlow(
                    boundaryEvent.getId(),
                    timeoutServiceTask.getId());
            timeoutFlow.setId(boundaryEvent.getId() + "-to-" + timeoutServiceTask.getId());

            // 【Add 】8. ExclusiveGateway
            ExclusiveGateway gateway = new ExclusiveGateway();
            gateway.setId(userTask.getId() + "_gateway");

            // 【Add 】9. ServiceTask -> Gateway
            SequenceFlow toGateway = new SequenceFlow(
                    timeoutServiceTask.getId(),
                    gateway.getId());
            toGateway.setId(timeoutServiceTask.getId() + "-to-gateway");

            // 【Add 】10. Gateway -> UserTask ( )
            SequenceFlow loopBack = new SequenceFlow(
                    gateway.getId(),
                    userTask.getId());
            loopBack.setId("loopBack-" + userTask.getId());
            loopBack.setName("重复通知");
            loopBack.setConditionExpression("${messageNode_loopBack == true}");

            // 【Add 】11. Gateway -> EndEvent ( )
            EndEvent rejectEndEvent = new EndEvent();
            rejectEndEvent.setId(userTask.getId() + "_rejectEnd");
            rejectEndEvent.setName("自动驳回");

            SequenceFlow rejectFlow = new SequenceFlow(
                    gateway.getId(),
                    rejectEndEvent.getId());
            rejectFlow.setId("reject-" + userTask.getId());
            rejectFlow.setName("自动驳回");
            rejectFlow.setConditionExpression("${messageNode_autoReject == true}");

            // 【Add 】12. Gateway -> node ( : )
            Node child = this.getChildNode();
            SequenceFlow continueFlow = buildSequence(child);
            continueFlow.setSourceRef(gateway.getId());
            continueFlow.setId("continue-" + userTask.getId());
            gateway.setDefaultFlow(continueFlow.getId());

            // 13. all relatedelement
            elements.add(boundaryEvent);
            elements.add(timeoutServiceTask);
            elements.add(timeoutFlow);
            elements.add(gateway);
            elements.add(toGateway);
            elements.add(loopBack);
            elements.add(rejectEndEvent);
            elements.add(rejectFlow);
            elements.add(continueFlow);
        } else {
            // configuration, node
            Node child = this.getChildNode();
            SequenceFlow sequenceFlow = this.buildSequence(child);
            elements.add(sequenceFlow);
        }

        // 14. UserTask element
        elements.add(userTask);

        // 15. Convert sub node
        Node child = this.getChildNode();
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }

        return elements;
    }

    /**
     * Get notificationobject (Assignee)
     * C: configuration users, from childNode Get
     */
    private String getAssignee() {
        // 1. configuration users
        if (ObjectUtil.isNotEmpty(this.users)) {
            return String.join(",", this.users);
        }

        // 2. Get node approver
        Node child = this.getChildNode();
        if (child instanceof ApprovalNode) {
            ApprovalNode approvalNode = (ApprovalNode) child;
            if (ObjectUtil.isNotEmpty(approvalNode.getUsers())) {
                return String.join(",", approvalNode.getUsers());
            }
        }

        return null;
    }
}
