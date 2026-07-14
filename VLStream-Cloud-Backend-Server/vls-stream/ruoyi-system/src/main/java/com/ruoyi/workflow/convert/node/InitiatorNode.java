/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.*;

import java.util.*;

/**
 * 发起人节点
 */
public class InitiatorNode extends Node {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();
    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 用户节点
        UserTask userTask = new UserTask();
        userTask.setId(this.getId());
        userTask.setName(this.getNodeName());
//        if(StringUtils.isNotBlank(this.getFormKey())){
//            userTask.setFormKey("key_"+this.getFormKey());
//        }
        // userTask.setAsynchronous(true);
        userTask.setExecutionListeners(this.buidExecutionListener());
//        userTask.setAssignee(String.format("${%s}", variable));
        userTask.setAssignee("${"+BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR+"}");


        Map<String, List<ExtensionAttribute>> attributes = new HashMap<String, List<ExtensionAttribute>>();
        ExtensionAttribute es = new ExtensionAttribute();
        es.setName("flowable:dataType");
        es.setValue("INITIATOR");
        ExtensionAttribute es1 = new ExtensionAttribute();
        es1.setName("flowable:text");
        es1.setValue("流程发起人");
        List<ExtensionAttribute> arr = new ArrayList<ExtensionAttribute>();
        arr.add(es);
        arr.add(es1);
        attributes.put("http://flowable.org/bpmn", arr);
        userTask.setAttributes(attributes);


        // 创建自定义扩展元素
        ExtensionElement propertiesBtn = new ExtensionElement();
        propertiesBtn.setName("flowable:propertiesBtn");

        // 创建并添加 buttonOprArr 属性
        ExtensionElement property = new ExtensionElement();
        property.setName("flowable:property");

        // 创建 ExtensionAttribute 来封装属性
        ExtensionAttribute buttonOprArr = new ExtensionAttribute();
        buttonOprArr.setName("name");
        buttonOprArr.setValue("buttonOprArr");

        ExtensionAttribute buttonOprArrValue = new ExtensionAttribute();
        buttonOprArrValue.setName("value");
        buttonOprArrValue.setValue("0,1,2,3,4");

        // 将属性添加到扩展元素
        property.addAttribute(buttonOprArr);
        property.addAttribute(buttonOprArrValue);

        // 将 property 添加到 propertiesBtn 扩展元素
        propertiesBtn.addChildElement(property);

        // 将扩展元素添加到用户任务
        userTask.addExtensionElement(propertiesBtn);


        elements.add(userTask);
        // 下一个节点的连线
        Node child = this.getChildNode();
        SequenceFlow sequenceFlow = this.buildSequence(child);
        elements.add(sequenceFlow);
        // 下一个节点
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }
}
