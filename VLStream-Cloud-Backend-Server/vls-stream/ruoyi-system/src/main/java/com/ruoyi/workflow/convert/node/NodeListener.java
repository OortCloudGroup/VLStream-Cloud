/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import com.ruoyi.workflow.convert.listeners.ApprovalListeners;
import lombok.Data;
import org.flowable.bpmn.model.ImplementationType;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeListener {
    private String event;
    private String implementation= ApprovalListeners.class.getName();
    private String implementationType= ImplementationType.IMPLEMENTATION_TYPE_CLASS;
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
}
