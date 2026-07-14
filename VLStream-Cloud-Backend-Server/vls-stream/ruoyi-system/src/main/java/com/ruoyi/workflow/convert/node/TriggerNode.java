/*
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
 * 触发器节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TriggerNode extends Node {

    //  1:网络请求，2:消息
    private Integer triggerType;
    // 请求地址
    private String url;
    // 请求方法
    private String method = "GET";
    // 请求参数类型 1 json , 2 form
    private Integer paramsType = 1;
    // 请求头
    private List<HeaderOrParams> headers = new ArrayList<>();
    // 请求参数
    private List<HeaderOrParams> params = new ArrayList<>();

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 服务节点
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setId(this.getId());
        serviceTask.setName(this.getNodeName());
        if (triggerType != null) {
            serviceTask.setType(triggerType == 1 ? ServiceTask.HTTP_TASK : ServiceTask.MAIL_TASK);
        }
        //配置扩展字段
        if (Objects.equals(triggerType, 1)) {
            // 网络请求配置
            addHttpExtensions(serviceTask);
        } else if (Objects.equals(triggerType, 2)) {
            // 消息触发器配置
            serviceTask.setImplementation("triggerMessage");
        }
        elements.add(serviceTask);


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

    /**
     * 添加 HTTP 请求的扩展属性
     */
    private void addHttpExtensions(ServiceTask serviceTask) {
        // 设置 Implementation 和 ImplementationType
        serviceTask.setImplementation(HttpTriggerDelegate.class.getName());
        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);


        // 设置 HTTP 方法
        FieldExtension methodProperty = new FieldExtension();
        methodProperty.setFieldName("requestMethod");
        methodProperty.setStringValue(this.method);
        serviceTask.getFieldExtensions().add(methodProperty);

        FieldExtension requestUrl = new FieldExtension();
        //URL地址必须得叫requestUrl才能解析成功
        requestUrl.setFieldName("requestUrl");
        requestUrl.setStringValue(this.url);
        serviceTask.getFieldExtensions().add(requestUrl);

        // 设置请求头
        if (headers != null) {
            String headersJson = new Gson().toJson(headers);  // 使用 Gson 将 List 转换为 JSON
            FieldExtension headersField = new FieldExtension();
            headersField.setFieldName("headers");
            headersField.setStringValue(headersJson);
            serviceTask.getFieldExtensions().add(headersField);
        }

        // 设置请求参数
        if (params != null) {
            String paramsJson = new Gson().toJson(params);  // 使用 Gson 将 List 转换为 JSON
            FieldExtension paramsField = new FieldExtension();
            paramsField.setFieldName("params");
            paramsField.setStringValue(paramsJson);
            serviceTask.getFieldExtensions().add(paramsField);
        }

        //设置参数类型（JSON 或 Form）
        FieldExtension paramsTypeProperty = new FieldExtension();
        paramsTypeProperty.setFieldName("paramsType");
        paramsTypeProperty.setStringValue(this.paramsType == 1 ? "json" : "form");
        serviceTask.getFieldExtensions().

            add(paramsTypeProperty);
    }
}
