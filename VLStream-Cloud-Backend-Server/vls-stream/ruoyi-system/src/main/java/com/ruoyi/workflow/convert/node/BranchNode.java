/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.node;

import lombok.Data;
import org.flowable.bpmn.model.FlowElement;

import java.util.List;

/**
 * @description: node
 */
@Data
public abstract class BranchNode extends Node {
    private List<ConditionNode> conditionNodes;

    public abstract List<FlowElement> convert();

}
