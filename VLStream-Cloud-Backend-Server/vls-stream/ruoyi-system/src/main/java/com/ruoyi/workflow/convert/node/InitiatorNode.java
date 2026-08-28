/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.node;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.*;

import java.util.*;

/**
 * node
 */
public class InitiatorNode extends Node {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<NodeFormProperty> formProperties = new ArrayList<>();
    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // usernode
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


        // Custom element
        ExtensionElement propertiesBtn = new ExtensionElement();
        propertiesBtn.setName("flowable:propertiesBtn");

        // buttonOprArr property
        ExtensionElement property = new ExtensionElement();
        property.setName("flowable:property");

        // ExtensionAttribute property
        ExtensionAttribute buttonOprArr = new ExtensionAttribute();
        buttonOprArr.setName("name");
        buttonOprArr.setValue("buttonOprArr");

        ExtensionAttribute buttonOprArrValue = new ExtensionAttribute();
        buttonOprArrValue.setName("value");
        buttonOprArrValue.setValue("0,1,2,3,4");

        // property element
        property.addAttribute(buttonOprArr);
        property.addAttribute(buttonOprArrValue);

        // property propertiesBtn element
        propertiesBtn.addChildElement(property);

        // element usertask
        userTask.addExtensionElement(propertiesBtn);


        elements.add(userTask);
        // node
        Node child = this.getChildNode();
        SequenceFlow sequenceFlow = this.buildSequence(child);
        elements.add(sequenceFlow);
        // node
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }
}
