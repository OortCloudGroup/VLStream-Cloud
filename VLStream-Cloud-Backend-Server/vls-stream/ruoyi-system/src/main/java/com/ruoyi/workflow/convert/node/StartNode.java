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
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Title: StartNode
 * @Author：蔡晓峰
 * @Date：2023/11/26 14:16
 * @github：https://github.com/tsai996/lowflow-design
 * @gitee：https://gitee.com/cai_xiao_feng/lowflow-design
 * @description：启动节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StartNode extends Node {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();
    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 开始节点
        StartEvent startEvent = new StartEvent();
        startEvent.setId(this.getId());
        startEvent.setName(this.getNodeName());
        startEvent.setFormKey("key_"+this.getFormKey());
        startEvent.setExecutionListeners(this.buidExecutionListener());
        elements.add(startEvent);
        // 下一个节点的连线
        Node child = this.getChildNode();
        SequenceFlow sequenceFlow = this.buildSequence(child);
        elements.add(sequenceFlow);
        // 下一个节点
        if (Objects.nonNull(child)) {
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }
}
