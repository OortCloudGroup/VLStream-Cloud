/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;

import java.util.*;

/**
 * @description：抄送节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CcNode extends Node {
    // 允许发起人添加抄送人
    private Boolean canSelectCS = false;
    // 抄送人员列表
    private List<String> users;
    // 抄送部门列表
    private List<String> dept;
    // 表单属性
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();    // 操作权限
    private Map<String, Boolean> operations = new LinkedHashMap<>();

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 服务节点
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId(this.getId());
        serviceTask.setName(this.getNodeName());
        // serviceTask.setAsynchronous(true);
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        serviceTask.setImplementation("${ccDelegate}");
        elements.add(serviceTask);
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

}
