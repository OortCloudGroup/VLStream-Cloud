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
 * @description：独占分支(排他网关)
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExclusiveNode extends BranchNode {


    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 独占分支
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
        // 子节点
        if (!CollectionUtils.isEmpty(children)) {
            for (Node next : children) {
                String branchId = Optional.ofNullable(this.getChildNode()).map(Node::getId).orElse(this.getBranchId());
                next.setBranchId(branchId);
                elements.addAll(next.convert());
            }
        }
        // 下一个节点
        Node child = this.getChildNode();
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }
}
