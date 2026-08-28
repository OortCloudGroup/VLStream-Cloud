/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import com.google.gson.Gson;
import com.ruoyi.workflow.convert.delegate.HttpTriggerDelegate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flowable.bpmn.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * node
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TriggerNode extends Node {

    // 1: , 2:
    private Integer triggerType;
    //
    private String url;
    // method
    private String method = "GET";
    // parameter 1 json , 2 form
    private Integer paramsType = 1;
    //
    private List<HeaderOrParams> headers = new ArrayList<>();
    // parameter
    private List<HeaderOrParams> params = new ArrayList<>();

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // servicenode
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId(this.getId());
        serviceTask.setName(this.getNodeName());
        if (triggerType != null) {
            serviceTask.setType(triggerType == 1 ? ServiceTask.HTTP_TASK : ServiceTask.MAIL_TASK);
        }
        // configuration field
        if (Objects.equals(triggerType, 1)) {
            // configuration
            addHttpExtensions(serviceTask);
        } else if (Objects.equals(triggerType, 2)) {
            // configuration
            serviceTask.setImplementation("triggerMessage");
        }
        elements.add(serviceTask);


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

    /**
     * HTTP extension properties
     */
    private void addHttpExtensions(ServiceTask serviceTask) {
        // Set Implementation and ImplementationType
        serviceTask.setImplementation(HttpTriggerDelegate.class.getName());
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);


        // Set HTTP method
        FieldExtension methodProperty = new FieldExtension();
        methodProperty.setFieldName("requestMethod");
        methodProperty.setStringValue(this.method);
        serviceTask.getFieldExtensions().add(methodProperty);

        FieldExtension requestUrl = new FieldExtension();
        // URL requestUrl can Parse successfully
        requestUrl.setFieldName("requestUrl");
        requestUrl.setStringValue(this.url);
        serviceTask.getFieldExtensions().add(requestUrl);

        // Set
        if (headers != null) {
            String headersJson = new Gson().toJson(headers);  // Gson List Convert to JSON
            FieldExtension headersField = new FieldExtension();
            headersField.setFieldName("headers");
            headersField.setStringValue(headersJson);
            serviceTask.getFieldExtensions().add(headersField);
        }

        // Set parameter
        if (params != null) {
            String paramsJson = new Gson().toJson(params);  // Gson List Convert to JSON
            FieldExtension paramsField = new FieldExtension();
            paramsField.setFieldName("params");
            paramsField.setStringValue(paramsJson);
            serviceTask.getFieldExtensions().add(paramsField);
        }

        // Set parameter (JSON Form)
        FieldExtension paramsTypeProperty = new FieldExtension();
        paramsTypeProperty.setFieldName("paramsType");
        paramsTypeProperty.setStringValue(this.paramsType == 1 ? "json" : "form");
        serviceTask.getFieldExtensions().

            add(paramsTypeProperty);
    }
}
