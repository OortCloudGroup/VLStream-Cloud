/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.workflow.convert.node.Node;
import lombok.Data;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description：流程模型
 */
@Data
public class ProcessModel {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String code;
    private String name;
    private Node process;
    private Integer version;
    private Integer sort;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;
    private String remark;
    private boolean notifyAllSteps;

    public BpmnModel toBpmnModel() {
        BpmnModel bpmnModel = new BpmnModel();
        // 命名空间
        bpmnModel.setTargetNamespace("https://flowable.org/bpmn20");
        // 创建一个流程
        Process process = new Process();
        // 设置流程的id
        process.setId(this.getCode());
        // 设置流程的name
        process.setName(this.getName());
        //是否需要消息推送
        if (notifyAllSteps) {
            Map<String, List<ExtensionAttribute>> attributes = new HashMap<>();
            ExtensionAttribute extensionAttribute = new ExtensionAttribute();
            extensionAttribute.setName("flowable:notifyAllSteps");
            extensionAttribute.setValue("true");
            attributes.put("flowable:notifyAllSteps", Collections.singletonList(extensionAttribute));
            process.setAttributes(attributes);
        }

        // 设置流程的文档
        process.setDocumentation(this.getRemark());
        // 递归构建所有节点
        Node node = this.getProcess();
        List<FlowElement> flowElementList = node.convert();
        System.out.println("=========dddddddd=========" + JSONUtil.toJsonStr(flowElementList));
        for (FlowElement flowElement : flowElementList) {
            process.addFlowElement(flowElement);
        }
        // 设置流程
        bpmnModel.addProcess(process);
        // 自动布局
        new BpmnAutoLayout(bpmnModel).execute();
        return bpmnModel;
    }
}
