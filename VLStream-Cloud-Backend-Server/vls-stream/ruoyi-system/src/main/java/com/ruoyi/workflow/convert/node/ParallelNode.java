/*
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
 * @description：并行节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ParallelNode extends BranchNode {
    private String name;

    @Override
    public List<FlowElement> convert() {
        // 待添加
        return null;
    }
}
