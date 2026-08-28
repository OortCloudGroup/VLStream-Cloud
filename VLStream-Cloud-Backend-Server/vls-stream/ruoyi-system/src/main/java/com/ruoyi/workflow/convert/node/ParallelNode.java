/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flowable.bpmn.model.FlowElement;

import java.util.List;

/**
 * @Title: ParallelNode
 * @description: node
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ParallelNode extends BranchNode {
    private String name;

    @Override
    public List<FlowElement> convert() {
        //
        return null;
    }
}
