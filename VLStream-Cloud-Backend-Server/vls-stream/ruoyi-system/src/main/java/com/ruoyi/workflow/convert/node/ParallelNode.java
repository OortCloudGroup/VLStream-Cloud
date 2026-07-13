/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
