/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.FlowElement;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @description: ( )
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExclusiveNode extends BranchNode {


    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        //
        ExclusiveGateway exclusiveGateway = new ExclusiveGateway();
        exclusiveGateway.setId(this.getId());
        exclusiveGateway.setName(this.getNodeName());
        elements.add(exclusiveGateway);
        List<ConditionNode> children = this.getConditionNodes();
        children.stream()
                .filter(ConditionNode::getDef)
                .findFirst()
                .ifPresent(conditionNode -> {
                    exclusiveGateway.setDefaultFlow(conditionNode.getId());
                });
        // sub node
        if (!CollectionUtils.isEmpty(children)) {
            for (Node next : children) {
                String branchId = Optional.ofNullable(this.getChildNode()).map(Node::getId).orElse(this.getBranchId());
                next.setBranchId(branchId);
                elements.addAll(next.convert());
            }
        }
        // node
        Node child = this.getChildNode();
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }
}
