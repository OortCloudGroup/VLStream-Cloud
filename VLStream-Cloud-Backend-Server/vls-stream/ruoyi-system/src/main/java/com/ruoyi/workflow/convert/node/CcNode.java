/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * @description: node
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CcNode extends Node {
    //
    private Boolean canSelectCS = false;
    //
    private List<String> users;
    // department
    private List<String> dept;
    // formproperty
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();    // operation
    private Map<String, Boolean> operations = new LinkedHashMap<>();

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // servicenode
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId(this.getId());
        serviceTask.setName(this.getNodeName());
        // serviceTask.setAsynchronous(true);
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        serviceTask.setImplementation("${ccDelegate}");
        elements.add(serviceTask);
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

}
