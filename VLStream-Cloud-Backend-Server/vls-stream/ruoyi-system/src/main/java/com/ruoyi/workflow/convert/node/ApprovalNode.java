/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * @description: approvalnode
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Component
public class ApprovalNode extends Node {
    private static final Logger log = LoggerFactory.getLogger(ApprovalNode.class);
    // sysUserService
    private static final SysUserServiceImpl sysUserServiceImpl = SpringUtil.getBean(SysUserServiceImpl.class);
    // approvalobject 1 , 2 department, 3 , 4 role, 5 , 6 , 9
    private Integer approvalType = 1;
    //
    private List<String> users;
    // department
    private List<String> dept;
    // approverrole
    private List<String> roles;
    // current leader(job)
    private List<String> postLeaders;
    // current leader(post)
    private List<String> jobLeaders;
    // main 1 , 2 , 3 , 4
    private Integer leader = 1;
    // notification - 0: pstn notification 1: workup 2: workup dialog 3: workup
    // 4:APP prompt / tip service 5:
    private Integer priority;
    // notification -
    private String data;
    // notification array, : [0, 1, 2]
    private List<Integer> channelTypes;
    // Push
    private Boolean noNotifyAllSteps;
    // approval
    private ApprovalMultiEnum multi;
    // will
    private BigDecimal multiPercent;
    // approver is empty 1 , 2 , 3 approvaladministrator, 4
    private int emptyApproType;
    // approver is empty
    private List<String> emptyApproUser;
    // approver whether
    private Boolean shouldSign = false;
    // Process
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<TimeoutHandler> timeoutHandlers = new ArrayList<>();
    // if approval ,whether finish
    private Boolean disAgreenEnd = false;
    // operation dict value , , , , etc.
    private List<String> operations = new ArrayList<>();
    // tasklistener
    private List<NodeListener> taskListeners;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // usernode
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

        // if configuration priority, Push listener
        if (this.priority != null) {
            if (StringUtils.isBlank(this.data)) {
                throw new RuntimeException("通知方式-通知说明文字不能为空");
            }
            FlowableListener notificationListener = new FlowableListener();
            notificationListener.setEvent(TaskListener.EVENTNAME_CREATE);
            notificationListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            notificationListener.setImplementation(ApprovalNotificationListener.class.getName());

            // priority parameter
            FieldExtension priorityField = new FieldExtension();
            priorityField.setFieldName("priority");
            priorityField.setStringValue(String.valueOf(this.priority));
            notificationListener.getFieldExtensions().add(priorityField);

            // data parameter
            FieldExtension dataField = new FieldExtension();
            dataField.setFieldName("data");
            dataField.setStringValue(this.data);
            notificationListener.getFieldExtensions().add(dataField);

            // channelTypes parameter
            if (this.channelTypes != null && !this.channelTypes.isEmpty()) {
                FieldExtension channelTypesField = new FieldExtension();
                channelTypesField.setFieldName("channelTypes");
                // Gson List to JSON
                channelTypesField.setStringValue(new Gson().toJson(this.channelTypes));
                notificationListener.getFieldExtensions().add(channelTypesField);
            }

            userTask.getTaskListeners().add(notificationListener);
        }
        // full Initialize
        // ExtensionAttribute dataType = new ExtensionAttribute();
        // ExtensionAttribute text = new ExtensionAttribute();

        List<ExtensionAttribute> arr = new ArrayList<>();

        // Process dataType
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
                arr.add(dataType); // value
            }
        }

        // Process text
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

        // Set property
        if (!arr.isEmpty()) {
            Map<String, List<ExtensionAttribute>> attributes = new HashMap<>();
            attributes.put("http://flowable.org/bpmn", arr);
            userTask.setAttributes(attributes);
        }
        // arr.add(es3);

        // Custom element
        ExtensionElement propertiesBtn = new ExtensionElement();
        propertiesBtn.setName("flowable:propertiesBtn");

        // buttonOprArr property
        ExtensionElement property = new ExtensionElement();
        property.setName("flowable:property");

        // ExtensionAttribute property
        ExtensionAttribute propertyName = new ExtensionAttribute();
        propertyName.setName("name");
        propertyName.setValue("buttonOprArr");

        // int
        String button = operations.stream().map(Integer::parseInt).map(String::valueOf).collect(Collectors.joining());
        ExtensionAttribute propertyValue = new ExtensionAttribute();
        propertyValue.setName("value");
        propertyValue.setValue(button);

        // property element
        property.addAttribute(propertyName);
        property.addAttribute(propertyValue);
        // property propertiesBtn element
        propertiesBtn.addChildElement(property);

        // element usertask
        userTask.addExtensionElement(propertiesBtn);

        // Process Process
        if (timeoutHandlers != null && !timeoutHandlers.isEmpty()) {
            // task TaskListener
            FlowableListener clearListener = new FlowableListener();
            clearListener.setEvent(TaskListener.EVENTNAME_COMPLETE);
            clearListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            clearListener.setImplementation(ClearTimeoutJobListener.class.getName());
            userTask.getTaskListeners().add(clearListener);

            // to each Process event and servicetask
            int handlerIndex = 0;
            for (TimeoutHandler handler : timeoutHandlers) {
                // Generate handlerId ( )
                String handlerId = String.valueOf(handlerIndex++);

                // 1. non- in event
                BoundaryEvent boundaryEvent = new BoundaryEvent();
                boundaryEvent.setId(userTask.getId() + "_timeout_" + handlerId);
                boundaryEvent.setAttachedToRef(userTask);
                boundaryEvent.setCancelActivity(false); // non- in

                TimerEventDefinition timerDef = new TimerEventDefinition();
                timerDef.setTimeDuration(convertToISO8601(handler.getTriggerTime(), handler.getTriggerTimeUnit()));
                boundaryEvent.getEventDefinitions().add(timerDef);

                // 2. servicetask
                ServiceTask timeoutServiceTask = new ServiceTask();
                timeoutServiceTask.setId(userTask.getId() + "_timeoutHandler_" + handlerId);
                timeoutServiceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);

                // listener
                if (handler.getTriggerType() == 1) {
                    // notification
                    timeoutServiceTask.setImplementation(TimeoutNotificationListener.class.getName());

                    // parameter
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
                    //
                    timeoutServiceTask.setImplementation(TimeoutAutoApproveListener.class.getName());
                }

                // 3.
                SequenceFlow timeoutFlow = new SequenceFlow(
                    boundaryEvent.getId(),
                    timeoutServiceTask.getId());
                timeoutFlow.setId(boundaryEvent.getId() + "-to-" + timeoutServiceTask.getId());

                // 4. element
                elements.add(boundaryEvent);
                elements.add(timeoutServiceTask);
                elements.add(timeoutFlow);
            }
        }

        if (ObjectUtil.isEmpty(users) && ObjectUtil.isEmpty(postLeaders) && ObjectUtil.isEmpty(jobLeaders)
            || approvalType != 9 && approvalType != 3 && approvalType != 5 && approvalType != 6) {
            // approval
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
            // 2. listener ( full )
            autoSkipNullListener.setImplementation(AutoSkipNullAssigneeListener.class.getName());
            autoSkipNullListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            // 3. listener current userTask
            userTask.getTaskListeners().add(autoSkipNullListener);
        }
        // Process noNotifyAllSteps extension properties
        if (noNotifyAllSteps != null && noNotifyAllSteps) {
            ExtensionAttribute noNotifyAttr = new ExtensionAttribute();
            noNotifyAttr.setName("flowable:noNotifyAllSteps");
            noNotifyAttr.setValue("true");
            arr.add(noNotifyAttr);
        }
        elements.add(userTask);
        // node
        Node child = this.getChildNode();
        SequenceFlow sequenceFlow = this.buildSequence(child);
        elements.add(sequenceFlow);
        // node
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }

    // method : Convert to ISO 8601 ( 1 → PT24H)
    private String convertToISO8601(int time, int unit) {
        switch (unit) {
            case 1:
                return "P" + time + "D"; // Format to PnD
            case 2:
                return "PT" + time + "H";
            case 3:
                return "PT" + time + "M";
            case 4:
                return "PT" + time + "S"; // Format to PTnS
            default:
                throw new IllegalArgumentException("不支持该时间单位: " + unit);
        }
    }

    /**
     * HTTP extension properties
     */
    private void addHttpExtensions(FlowableListener flowableListener, NodeListener nodeListener) {
        // Set Implementation and ImplementationType
        // Set HTTP method
        FieldExtension methodProperty = new FieldExtension();
        methodProperty.setFieldName("requestMethod");
        methodProperty.setStringValue(nodeListener.getMethod());
        flowableListener.getFieldExtensions().add(methodProperty);

        FieldExtension requestUrl = new FieldExtension();
        // URL requestUrl can Parse successfully
        requestUrl.setFieldName("requestUrl");
        requestUrl.setStringValue(nodeListener.getUrl());
        flowableListener.getFieldExtensions().add(requestUrl);

        // Set
        if (nodeListener.getHeaders() != null) {
            String headersJson = new Gson().toJson(nodeListener.getHeaders()); // Gson List Convert to JSON
            FieldExtension headersField = new FieldExtension();
            headersField.setFieldName("headers");
            headersField.setStringValue(headersJson);
            flowableListener.getFieldExtensions().add(headersField);
        }

        // Set parameter
        if (nodeListener.getParams() != null) {
            String paramsJson = new Gson().toJson(nodeListener.getParams()); // Gson List Convert to JSON
            FieldExtension paramsField = new FieldExtension();
            paramsField.setFieldName("params");
            paramsField.setStringValue(paramsJson);
            flowableListener.getFieldExtensions().add(paramsField);
        }

        // Set parameter (JSON Form)
        FieldExtension paramsTypeProperty = new FieldExtension();
        paramsTypeProperty.setFieldName("paramsType");
        paramsTypeProperty.setStringValue(nodeListener.getParamsType() == 1 ? "json" : "form");
        flowableListener.getFieldExtensions().add(paramsTypeProperty);
    }

    /**
     * userTask Set " "listener assignee
     *
     * @param userTask current UserTask node
     * @param leaderList approver , to assignee
     */
    private void applyLeaderListener(UserTask userTask, List<String> leaderList, String implementation,
                                     String leaderPrefix) {
        // 1. FlowableListener, in task
        FlowableListener listener = new FlowableListener();
        listener.setEvent(TaskListener.EVENTNAME_CREATE);
        // 2. listener ( full )
        listener.setImplementation(implementation);
        listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        // 3. listener current userTask
        userTask.getTaskListeners().add(listener);
        // 4. Set candidate user to assignee
        if (ObjectUtil.isNotEmpty(leaderList)) {
            userTask.setAssignee(leaderPrefix + "-" + leaderList.get(0));
        }
    }

}
