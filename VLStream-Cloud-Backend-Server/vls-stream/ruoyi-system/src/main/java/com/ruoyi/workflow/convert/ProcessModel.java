/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 * @description: workflowmodel
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
        // null / empty
        bpmnModel.setTargetNamespace("https://flowable.org/bpmn20");
        // workflow
        Process process = new Process();
        // Set workflow id
        process.setId(this.getCode());
        // Set workflow name
        process.setName(this.getName());
        // whether need to Push
        if (notifyAllSteps) {
            Map<String, List<ExtensionAttribute>> attributes = new HashMap<>();
            ExtensionAttribute extensionAttribute = new ExtensionAttribute();
            extensionAttribute.setName("flowable:notifyAllSteps");
            extensionAttribute.setValue("true");
            attributes.put("flowable:notifyAllSteps", Collections.singletonList(extensionAttribute));
            process.setAttributes(attributes);
        }

        // Set workflow
        process.setDocumentation(this.getRemark());
        // Build all node
        Node node = this.getProcess();
        List<FlowElement> flowElementList = node.convert();
        System.out.println("=========dddddddd=========" + JSONUtil.toJsonStr(flowElementList));
        for (FlowElement flowElement : flowElementList) {
            process.addFlowElement(flowElement);
        }
        // Set workflow
        bpmnModel.addProcess(process);
        //
        new BpmnAutoLayout(bpmnModel).execute();
        return bpmnModel;
    }
}
