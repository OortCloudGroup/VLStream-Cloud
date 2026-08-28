/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
}
