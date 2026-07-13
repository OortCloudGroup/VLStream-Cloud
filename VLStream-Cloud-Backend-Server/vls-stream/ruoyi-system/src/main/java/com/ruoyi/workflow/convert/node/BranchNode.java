/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

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
