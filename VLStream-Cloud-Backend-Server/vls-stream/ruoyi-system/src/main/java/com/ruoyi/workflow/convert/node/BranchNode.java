package com.ruoyi.workflow.convert.node;

import lombok.Data;
import org.flowable.bpmn.model.FlowElement;

import java.util.List;

/**
 * @description：分支节点
 */
@Data
public abstract class BranchNode extends Node {
    private List<ConditionNode> conditionNodes;

    public abstract List<FlowElement> convert();

}
