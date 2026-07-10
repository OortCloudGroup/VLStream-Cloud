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
 * @description：消息通知节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Component
public class MessageNode extends Node {

    // 通知对象（用户ID列表）
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> users;

    // 通知优先级
    private Integer priority;

    // 通知数据
    private String data;

    // 通知渠道类型数组，支持多个渠道 如：[0, 1, 2]
    private List<Integer> channelTypes;

    // 超时时间（分钟）
    private Integer timeoutMinutes;

    // 超时动作: 1-重复通知, 2-自动转下个节点, 3-自动驳回
    private Integer timeoutAction;

    // 重复通知次数（timeoutAction=1时有效）
    private Integer repeatCount;

    // 验证间隔（秒），默认15秒
    private Integer verificationInterval = 15;

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();

        // 1. 创建 UserTask
        UserTask userTask = new UserTask();
        userTask.setId(this.getId());
        userTask.setName(this.getNodeName());

        // 2. 设置 Assignee (通知对象)
        String assignee = getAssignee();
        if (StringUtils.isNotBlank(assignee)) {
            // 添加特殊前缀，待办查询时自动过滤（taskAssignee 是等值查询）
            userTask.setAssignee("MESSAGENODE_" + assignee);
        } else {
            // 如果无法确定 assignee，可能需要在运行时处理或抛出异常
            // 这里暂时设置为空，依赖后续逻辑或抛出异常
            throw new RuntimeException("MessageNode: 无法确定通知对象，请配置 users 或确保下一个节点为审批节点且已配置审批人");
        }

        // 3. 添加 CREATE 监听器：发送通知
        FlowableListener notificationListener = new FlowableListener();
        notificationListener.setEvent(TaskListener.EVENTNAME_CREATE);
        notificationListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        notificationListener.setImplementation(MessageNotificationListener.class.getName());

        // 注入参数
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

        // 注入验证间隔参数
        FieldExtension intervalField = new FieldExtension();
        intervalField.setFieldName("verificationInterval");
        intervalField
                .setStringValue(String.valueOf(this.verificationInterval != null ? this.verificationInterval : 15));
        notificationListener.getFieldExtensions().add(intervalField);

        // 注入 channelTypes 参数
        if (this.channelTypes != null && !this.channelTypes.isEmpty()) {
            FieldExtension channelTypesField = new FieldExtension();
            channelTypesField.setFieldName("channelTypes");
            // 使用Gson将List序列化为JSON字符串
            channelTypesField.setStringValue(new Gson().toJson(this.channelTypes));
            notificationListener.getFieldExtensions().add(channelTypesField);
        }

        userTask.getTaskListeners().add(notificationListener);

        // 4. 添加 COMPLETE 监听器：清理定时器（如果有）
        if (this.timeoutMinutes != null && this.timeoutMinutes > 0) {
            FlowableListener clearListener = new FlowableListener();
            clearListener.setEvent(TaskListener.EVENTNAME_COMPLETE);
            clearListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            clearListener.setImplementation(ClearTimeoutJobListener.class.getName());
            userTask.getTaskListeners().add(clearListener);

            // 5. 添加 BoundaryEvent：超时处理
            BoundaryEvent boundaryEvent = new BoundaryEvent();
            boundaryEvent.setId(userTask.getId() + "_timeout");
            boundaryEvent.setAttachedToRef(userTask);
            boundaryEvent.setCancelActivity(true); // 中断型，超时后取消 UserTask

            TimerEventDefinition timerDef = new TimerEventDefinition();
            timerDef.setTimeDuration("PT" + this.timeoutMinutes + "M"); // 分钟
            boundaryEvent.getEventDefinitions().add(timerDef);

            // 6. 创建超时处理 ServiceTask
            ServiceTask timeoutServiceTask = new ServiceTask();
            timeoutServiceTask.setId(userTask.getId() + "_timeoutHandler");
            timeoutServiceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            timeoutServiceTask.setImplementation(MessageTimeoutHandler.class.getName());

            // 注入超时参数
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
            // 注入通知相关参数用于重复通知
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
            // 注入 channelTypes 参数
            if (this.channelTypes != null && !this.channelTypes.isEmpty()) {
                FieldExtension channelTypesField = new FieldExtension();
                channelTypesField.setFieldName("channelTypes");
                channelTypesField.setStringValue(new Gson().toJson(this.channelTypes));
                timeoutServiceTask.getFieldExtensions().add(channelTypesField);
            }

            // 7. 创建连线：BoundaryEvent -> ServiceTask
            SequenceFlow timeoutFlow = new SequenceFlow(
                    boundaryEvent.getId(),
                    timeoutServiceTask.getId());
            timeoutFlow.setId(boundaryEvent.getId() + "-to-" + timeoutServiceTask.getId());

            // 【新增】8. 创建 ExclusiveGateway
            ExclusiveGateway gateway = new ExclusiveGateway();
            gateway.setId(userTask.getId() + "_gateway");

            // 【新增】9. ServiceTask -> Gateway
            SequenceFlow toGateway = new SequenceFlow(
                    timeoutServiceTask.getId(),
                    gateway.getId());
            toGateway.setId(timeoutServiceTask.getId() + "-to-gateway");

            // 【新增】10. Gateway -> UserTask (回退分支)
            SequenceFlow loopBack = new SequenceFlow(
                    gateway.getId(),
                    userTask.getId());
            loopBack.setId("loopBack-" + userTask.getId());
            loopBack.setName("重复通知");
            loopBack.setConditionExpression("${messageNode_loopBack == true}");

            // 【新增】11. Gateway -> EndEvent (驳回分支)
            EndEvent rejectEndEvent = new EndEvent();
            rejectEndEvent.setId(userTask.getId() + "_rejectEnd");
            rejectEndEvent.setName("自动驳回");

            SequenceFlow rejectFlow = new SequenceFlow(
                    gateway.getId(),
                    rejectEndEvent.getId());
            rejectFlow.setId("reject-" + userTask.getId());
            rejectFlow.setName("自动驳回");
            rejectFlow.setConditionExpression("${messageNode_autoReject == true}");

            // 【新增】12. Gateway -> 下一个节点 (默认分支:自动通过)
            Node child = this.getChildNode();
            SequenceFlow continueFlow = buildSequence(child);
            continueFlow.setSourceRef(gateway.getId());
            continueFlow.setId("continue-" + userTask.getId());
            gateway.setDefaultFlow(continueFlow.getId());

            // 13. 添加所有超时相关元素到列表
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
            // 没有超时配置，直接连接到下一个节点
            Node child = this.getChildNode();
            SequenceFlow sequenceFlow = this.buildSequence(child);
            elements.add(sequenceFlow);
        }

        // 14. 添加 UserTask 到元素列表
        elements.add(userTask);

        // 15. 递归转换子节点
        Node child = this.getChildNode();
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }

        return elements;
    }

    /**
     * 获取通知对象 (Assignee)
     * 方案C：优先使用配置的 users，否则从 childNode 获取
     */
    private String getAssignee() {
        // 1. 优先使用配置的 users
        if (ObjectUtil.isNotEmpty(this.users)) {
            return String.join(",", this.users);
        }

        // 2. 否则尝试获取下一个节点的审批人
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
