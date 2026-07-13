/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.google.gson.Gson;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.flowable.common.constant.TaskConstants;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import com.ruoyi.workflow.convert.enums.ApprovalMultiEnum;
import com.ruoyi.workflow.convert.listeners.*;
import com.ruoyi.workflow.convert.model.TimeoutHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.task.service.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description：审批节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Component
public class ApprovalNode extends Node {
    private static final Logger log = LoggerFactory.getLogger(ApprovalNode.class);
    // 构造器注入 sysUserService
    private static final SysUserServiceImpl sysUserServiceImpl = SpringUtil.getBean(SysUserServiceImpl.class);
    // 审批对象类型 1指定人员， 2 指定部门， 3发起人自己， 4指定角色， 5发起人自选， 6 直属上级， 9系统自动拒绝
    private Integer approvalType = 1;
    // 人员
    private List<String> users;
    // 部门
    private List<String> dept;
    // 审批人角色
    private List<String> roles;
    // 当前岗位领导(job)
    private List<String> postLeaders;
    // 当前职位领导(post)
    private List<String> jobLeaders;
    // 主管 1 直属上级 ，2 二级上级， 3 三级上级， 4 四级上级
    private Integer leader = 1;
    // 通知方式-紧急级别 0：pstn电话通知 1：workup视频通话 2：workup息屏弹窗提醒 3：workup顶部消息栏提醒
    // 4:APP红点提示或微信服务号消息 5:短信或邮件提醒
    private Integer priority;
    // 通知方式-说明文字
    private String data;
    // 通知渠道类型数组，支持多个渠道 如：[0, 1, 2]
    private List<Integer> channelTypes;
    // 关闭站内推送方式
    private Boolean noNotifyAllSteps;
    // 多人审批方式
    private ApprovalMultiEnum multi;
    // 多人会签通过百分比
    private BigDecimal multiPercent;
    // 审批人为空类型 1 自动通过 ，2 自动驳回 ，3 转交审批管理员， 4 转交指定人员
    private int emptyApproType;
    // 审批人为空人员列表
    private List<String> emptyApproUser;
    // 审批人员同意是否需求签字
    private Boolean shouldSign = false;
    // 超时处理器列表
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<TimeoutHandler> timeoutHandlers = new ArrayList<>();
    // 如果审批被驳回,是否直接结束
    private Boolean disAgreenEnd = false;
    // 操作权限 字典的值 通过， 委派 ， 转办，退回 ，拒绝 等
    private List<String> operations = new ArrayList<>();
    // 任务监听器
    private List<NodeListener> taskListeners;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 用户节点
        UserTask userTask = new UserTask();
        userTask.setId(this.getId());
        userTask.setName(this.getNodeName());
        if (StringUtils.isNotBlank(this.getFormKey())) {
            userTask.setFormKey("key_" + this.getFormKey());
        }
        // userTask.setAsynchronous(true);
        // userTask.setFormKey(this.getFormKey());
        userTask.setExecutionListeners(this.buidExecutionListener());
        if (!CollectionUtils.isEmpty(this.taskListeners)) {
            List<FlowableListener> listeners = this.taskListeners.stream()
                                                                 .filter(l -> StringUtils.isNotBlank(l.getImplementation())).map(listener -> {
                    FlowableListener eventListener = new FlowableListener();
                    eventListener.setEvent(listener.getEvent());
                    eventListener.setImplementation(listener.getImplementation());
                    eventListener.setImplementationType(listener.getImplementationType());
                    addHttpExtensions(eventListener, listener);
                    return eventListener;
                }).collect(Collectors.toList());
            userTask.getTaskListeners().addAll(listeners);
        }

        // 如果配置了 priority，添加消息推送监听器
        if (this.priority != null) {
            if (StringUtils.isBlank(this.data)) {
                throw new RuntimeException("通知方式-通知说明文字不能为空");
            }
            FlowableListener notificationListener = new FlowableListener();
            notificationListener.setEvent(TaskListener.EVENTNAME_CREATE);
            notificationListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            notificationListener.setImplementation(ApprovalNotificationListener.class.getName());

            // 添加 priority 参数
            FieldExtension priorityField = new FieldExtension();
            priorityField.setFieldName("priority");
            priorityField.setStringValue(String.valueOf(this.priority));
            notificationListener.getFieldExtensions().add(priorityField);

            // 添加 data 参数
            FieldExtension dataField = new FieldExtension();
            dataField.setFieldName("data");
            dataField.setStringValue(this.data);
            notificationListener.getFieldExtensions().add(dataField);

            // 添加 channelTypes 参数
            if (this.channelTypes != null && !this.channelTypes.isEmpty()) {
                FieldExtension channelTypesField = new FieldExtension();
                channelTypesField.setFieldName("channelTypes");
                // 使用Gson将List序列化为JSON字符串
                channelTypesField.setStringValue(new Gson().toJson(this.channelTypes));
                notificationListener.getFieldExtensions().add(channelTypesField);
            }

            userTask.getTaskListeners().add(notificationListener);
        }
        // 移除全局初始化
        // ExtensionAttribute dataType = new ExtensionAttribute();
        // ExtensionAttribute text = new ExtensionAttribute();

        List<ExtensionAttribute> arr = new ArrayList<>();

        // 处理 dataType
        if (ObjectUtil.isNotEmpty(users) || ObjectUtil.isNotEmpty(roles) || ObjectUtil.isNotEmpty(dept)
            || ObjectUtil.isNotEmpty(postLeaders) || ObjectUtil.isNotEmpty(jobLeaders)) {
            ExtensionAttribute dataType = new ExtensionAttribute();
            dataType.setName("flowable:dataType");
            ExtensionAttribute formKey = new ExtensionAttribute();
            formKey.setName("flowable:formKey");
            formKey.setValue("key_" + this.getFormKey());
            if (ObjectUtil.isNotEmpty(users)) {
                dataType.setValue("USERS");
                String usersUser = String.join(",", users);
                userTask.setAssignee(usersUser);
                userTask.setCandidateUsers(users);
            } else if (ObjectUtil.isNotEmpty(roles)) {
                dataType.setValue("ROLES");
                roles.replaceAll(role -> TaskConstants.ROLE_GROUP_PREFIX + role);
                userTask.setCandidateGroups(roles);
            } else if (ObjectUtil.isNotEmpty(dept)) {
                dataType.setValue("DEPTS");
                dept.replaceAll(dept -> TaskConstants.DEPT_GROUP_PREFIX + dept);
                userTask.setCandidateGroups(dept);
            } else if (ObjectUtil.isNotEmpty(postLeaders)) {
                applyLeaderListener(userTask, postLeaders, ApprovalLeaderListeners.class.getName(), "post");
            } else if (ObjectUtil.isNotEmpty(jobLeaders)) {
                applyLeaderListener(userTask, jobLeaders, ApprovalLeaderListeners.class.getName(), "job");
            }
            // }else if (ObjectUtil.isNotEmpty(postLeaders)) {
            // dataType.setValue("JOBS");
            // postLeaders.replaceAll(job -> TaskConstants.JOB_GROUP_PREFIX + postLeaders);
            // userTask.setCandidateGroups(postLeaders);
            // }else if (ObjectUtil.isNotEmpty(jobLeaders)) {
            // dataType.setValue("POSTS");
            // jobLeaders.replaceAll(post -> TaskConstants.POST_GROUP_PREFIX + jobLeaders);
            // userTask.setCandidateGroups(jobLeaders);
            // }
            if (StringUtils.isNotBlank(dataType.getValue())) {
                arr.add(dataType); // 仅当有值时添加
            }
        }

        // 处理 text
        StringBuilder sbUser = new StringBuilder();
        switch (approvalType) {
            case 3:
            case 5:
                sbUser.append("流程发起人");
                userTask.setAssignee("${" + TaskConstants.PROCESS_INITIATOR + "}");
                break;
            case 9:
                sbUser.append("系统自动拒绝");
                userTask.setAssignee("${" + TaskConstants.PROCESS_INITIATOR + "}");
                applyLeaderListener(userTask, null, ApprovalAutoEndListeners.class.getName(), null);
                break;
            default:
                if (ObjectUtil.isNotEmpty(users)) {
                    for (int i = 0; i < users.size(); i++) {
                        SysUser user = sysUserServiceImpl.selectUserById(users.get(i));
                        sbUser.append(Optional.ofNullable(user.getUserName()).orElse(""));
                        if (i < users.size() - 1)
                            sbUser.append(",");
                    }
                }
        }

        if (StringUtils.isNotBlank(sbUser.toString())) {
            ExtensionAttribute text = new ExtensionAttribute();

            text.setName("flowable:text");
            text.setValue(sbUser.toString());
            arr.add(text);
        }

        // 最终设置属性
        if (!arr.isEmpty()) {
            Map<String, List<ExtensionAttribute>> attributes = new HashMap<>();
            attributes.put("http://flowable.org/bpmn", arr);
            userTask.setAttributes(attributes);
        }
        // arr.add(es3);

        // 创建自定义扩展元素
        ExtensionElement propertiesBtn = new ExtensionElement();
        propertiesBtn.setName("flowable:propertiesBtn");

        // 创建并添加 buttonOprArr 属性
        ExtensionElement property = new ExtensionElement();
        property.setName("flowable:property");

        // 创建 ExtensionAttribute 来封装属性
        ExtensionAttribute propertyName = new ExtensionAttribute();
        propertyName.setName("name");
        propertyName.setValue("buttonOprArr");

        // 先传成int再转字符串
        String button = operations.stream().map(Integer::parseInt).map(String::valueOf).collect(Collectors.joining());
        ExtensionAttribute propertyValue = new ExtensionAttribute();
        propertyValue.setName("value");
        propertyValue.setValue(button);

        // 将属性添加到扩展元素
        property.addAttribute(propertyName);
        property.addAttribute(propertyValue);
        // 将 property 添加到 propertiesBtn 扩展元素
        propertiesBtn.addChildElement(property);

        // 将扩展元素添加到用户任务
        userTask.addExtensionElement(propertiesBtn);

        // 处理多个超时处理器
        if (timeoutHandlers != null && !timeoutHandlers.isEmpty()) {
            // 添加任务完成时清理定时器的 TaskListener
            FlowableListener clearListener = new FlowableListener();
            clearListener.setEvent(TaskListener.EVENTNAME_COMPLETE);
            clearListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            clearListener.setImplementation(ClearTimeoutJobListener.class.getName());
            userTask.getTaskListeners().add(clearListener);

            // 为每个超时处理器创建边界事件和服务任务
            int handlerIndex = 0;
            for (TimeoutHandler handler : timeoutHandlers) {
                // 自动生成 handlerId（使用递增索引）
                String handlerId = String.valueOf(handlerIndex++);

                // 1. 创建非中断型边界事件
                BoundaryEvent boundaryEvent = new BoundaryEvent();
                boundaryEvent.setId(userTask.getId() + "_timeout_" + handlerId);
                boundaryEvent.setAttachedToRef(userTask);
                boundaryEvent.setCancelActivity(false); // 非中断

                TimerEventDefinition timerDef = new TimerEventDefinition();
                timerDef.setTimeDuration(convertToISO8601(handler.getTriggerTime(), handler.getTriggerTimeUnit()));
                boundaryEvent.getEventDefinitions().add(timerDef);

                // 2. 创建服务任务
                ServiceTask timeoutServiceTask = new ServiceTask();
                timeoutServiceTask.setId(userTask.getId() + "_timeoutHandler_" + handlerId);
                timeoutServiceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);

                // 根据触发类型选择监听器
                if (handler.getTriggerType() == 1) {
                    // 消息通知
                    timeoutServiceTask.setImplementation(TimeoutNotificationListener.class.getName());

                    // 添加参数
                    FieldExtension userIdField = new FieldExtension();
                    userIdField.setFieldName("notificationUserId");
                    userIdField.setStringValue(handler.getNotificationUserId());
                    timeoutServiceTask.getFieldExtensions().add(userIdField);

                    FieldExtension priorityField = new FieldExtension();
                    priorityField.setFieldName("priority");
                    priorityField.setStringValue(String.valueOf(
                        handler.getPriority() != null ? handler.getPriority() : this.priority));
                    timeoutServiceTask.getFieldExtensions().add(priorityField);

                    FieldExtension dataField = new FieldExtension();
                    dataField.setFieldName("data");
                    dataField.setStringValue(
                        handler.getData() != null ? handler.getData() : this.data);
                    timeoutServiceTask.getFieldExtensions().add(dataField);

                } else if (handler.getTriggerType() == 2) {
                    // 自动通过
                    timeoutServiceTask.setImplementation(TimeoutAutoApproveListener.class.getName());
                }

                // 3. 创建连线
                SequenceFlow timeoutFlow = new SequenceFlow(
                    boundaryEvent.getId(),
                    timeoutServiceTask.getId());
                timeoutFlow.setId(boundaryEvent.getId() + "-to-" + timeoutServiceTask.getId());

                // 4. 添加到元素列表
                elements.add(boundaryEvent);
                elements.add(timeoutServiceTask);
                elements.add(timeoutFlow);
            }
        }

        if (ObjectUtil.isEmpty(users) && ObjectUtil.isEmpty(postLeaders) && ObjectUtil.isEmpty(jobLeaders)
            || approvalType != 9 && approvalType != 3 && approvalType != 5 && approvalType != 6) {
            // 多人审批方式
            MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = new MultiInstanceLoopCharacteristics();
            if (this.getMulti() == ApprovalMultiEnum.SEQUENTIAL) {
                multiInstanceLoopCharacteristics.setSequential(true);
            } else if (this.getMulti() == ApprovalMultiEnum.JOINT) {
                multiInstanceLoopCharacteristics.setSequential(false);
                if (Objects.nonNull(this.getMultiPercent()) && this.getMultiPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percent = this.getMultiPercent().divide(new BigDecimal(100), 2, RoundingMode.DOWN);
                    multiInstanceLoopCharacteristics.setCompletionCondition(String.format("${nrOfCompletedInstances" +
                            "/nrOfInstances >= %s}",
                        percent));
                }
            } else if (this.getMulti() == ApprovalMultiEnum.SINGLE) {
                multiInstanceLoopCharacteristics.setSequential(false);
                multiInstanceLoopCharacteristics.setCompletionCondition("${nrOfCompletedInstances > 0}");
            }
            String variable = String.format("assignee");
            multiInstanceLoopCharacteristics.setElementVariable(variable);
            multiInstanceLoopCharacteristics.setInputDataItem(String.format("${multiInstanceHandler.getUserIds" +
                "(execution)" +
                "}"));
            userTask.setLoopCharacteristics(multiInstanceLoopCharacteristics);
            userTask.setAssignee(String.format("${%s}", variable));
        } else {
            FlowableListener autoSkipNullListener = new FlowableListener();
            autoSkipNullListener.setEvent(TaskListener.EVENTNAME_CREATE);
            // 2. 指定监听器实现类（全限定名）
            autoSkipNullListener.setImplementation(AutoSkipNullAssigneeListener.class.getName());
            autoSkipNullListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            // 3. 把 listener 挂到当前 userTask
            userTask.getTaskListeners().add(autoSkipNullListener);
        }
        // 处理 noNotifyAllSteps 扩展属性
        if (noNotifyAllSteps != null && noNotifyAllSteps) {
            ExtensionAttribute noNotifyAttr = new ExtensionAttribute();
            noNotifyAttr.setName("flowable:noNotifyAllSteps");
            noNotifyAttr.setValue("true");
            arr.add(noNotifyAttr);
        }
        elements.add(userTask);
        // 下一个节点的连线
        Node child = this.getChildNode();
        SequenceFlow sequenceFlow = this.buildSequence(child);
        elements.add(sequenceFlow);
        // 下一个节点
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }

    // 辅助方法：将时间单位转换为 ISO 8601 格式（如 1天 → PT24H）
    private String convertToISO8601(int time, int unit) {
        switch (unit) {
            case 1:
                return "P" + time + "D"; // 天数格式化为 PnD
            case 2:
                return "PT" + time + "H";
            case 3:
                return "PT" + time + "M";
            case 4:
                return "PT" + time + "S"; // 秒数格式化为 PTnS
            default:
                throw new IllegalArgumentException("不支持该时间单位: " + unit);
        }
    }

    /**
     * 添加 HTTP 请求的扩展属性
     */
    private void addHttpExtensions(FlowableListener flowableListener, NodeListener nodeListener) {
        // 设置 Implementation 和 ImplementationType
        // 设置 HTTP 方法
        FieldExtension methodProperty = new FieldExtension();
        methodProperty.setFieldName("requestMethod");
        methodProperty.setStringValue(nodeListener.getMethod());
        flowableListener.getFieldExtensions().add(methodProperty);

        FieldExtension requestUrl = new FieldExtension();
        // URL地址必须得叫requestUrl才能解析成功
        requestUrl.setFieldName("requestUrl");
        requestUrl.setStringValue(nodeListener.getUrl());
        flowableListener.getFieldExtensions().add(requestUrl);

        // 设置请求头
        if (nodeListener.getHeaders() != null) {
            String headersJson = new Gson().toJson(nodeListener.getHeaders()); // 使用 Gson 将 List 转换为 JSON
            FieldExtension headersField = new FieldExtension();
            headersField.setFieldName("headers");
            headersField.setStringValue(headersJson);
            flowableListener.getFieldExtensions().add(headersField);
        }

        // 设置请求参数
        if (nodeListener.getParams() != null) {
            String paramsJson = new Gson().toJson(nodeListener.getParams()); // 使用 Gson 将 List 转换为 JSON
            FieldExtension paramsField = new FieldExtension();
            paramsField.setFieldName("params");
            paramsField.setStringValue(paramsJson);
            flowableListener.getFieldExtensions().add(paramsField);
        }

        // 设置参数类型（JSON 或 Form）
        FieldExtension paramsTypeProperty = new FieldExtension();
        paramsTypeProperty.setFieldName("paramsType");
        paramsTypeProperty.setStringValue(nodeListener.getParamsType() == 1 ? "json" : "form");
        flowableListener.getFieldExtensions().add(paramsTypeProperty);
    }

    /**
     * 给 userTask 设置“创建时”监听器并指定 assignee
     *
     * @param userTask   当前的 UserTask 节点
     * @param leaderList 候选审批人列表，取第一个作为 assignee
     */
    private void applyLeaderListener(UserTask userTask, List<String> leaderList, String implementation,
                                     String leaderPrefix) {
        // 1. 创建 FlowableListener，并指定在任务创建时触发
        FlowableListener listener = new FlowableListener();
        listener.setEvent(TaskListener.EVENTNAME_CREATE);
        // 2. 指定监听器实现类（全限定名）
        listener.setImplementation(implementation);
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        // 3. 把 listener 挂到当前 userTask
        userTask.getTaskListeners().add(listener);
        // 4. 设置第一个候选人为 assignee
        if (ObjectUtil.isNotEmpty(leaderList)) {
            userTask.setAssignee(leaderPrefix + "-" + leaderList.get(0));
        }
    }

}
