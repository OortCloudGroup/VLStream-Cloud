/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

//package com.ruoyi.workflow.convert.node;
//
//import com.ruoyi.workflow.convert.enums.NotifyTypeEnum;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import org.flowable.bpmn.model.FlowElement;
//import org.flowable.bpmn.model.ImplementationType;
//import org.flowable.bpmn.model.SequenceFlow;
//import org.flowable.bpmn.model.ServiceTask;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
///**
// * 服务节点
// */
//@EqualsAndHashCode(callSuper = true)
//@Data
//public class NotifyNode extends AssigneeNode {
//    private List<NotifyTypeEnum> types;
//    private String subject;
//    private String content;
//
//    @Override
//    public List<FlowElement> convert() {
//        ArrayList<FlowElement> elements = new ArrayList<>();
//        // 服务节点
//        ServiceTask serviceTask = new ServiceTask();
//        serviceTask.setId(this.getId());
//        serviceTask.setName(this.getNodeName());
//        serviceTask.setAsynchronous(true);
//        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
//        serviceTask.setImplementation("${notifyDelegate}");
//        elements.add(serviceTask);
//        // 下一个节点的连线
//        Node child = this.getChildNode();
//        SequenceFlow sequenceFlow = this.buildSequence(child);
//        elements.add(sequenceFlow);
//        // 下一个节点
//        if (Objects.nonNull(child)) {
//            child.setBranchId(this.getBranchId());
//            List<FlowElement> flowElements = child.convert();
//            elements.addAll(flowElements);
//        }
//        return elements;
//    }
//}
