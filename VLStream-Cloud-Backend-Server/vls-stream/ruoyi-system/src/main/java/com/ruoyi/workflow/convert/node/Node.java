/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.SequenceFlow;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description：节点
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType", defaultImpl = Node.class, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartNode.class, name = "start"),
        @JsonSubTypes.Type(value = CcNode.class, name = "cc"),
        @JsonSubTypes.Type(value = ApprovalNode.class, name = "approval"),
        @JsonSubTypes.Type(value = ConditionNode.class, name = "condition"),
        @JsonSubTypes.Type(value = ExclusiveNode.class, name = "exclusive"),
        @JsonSubTypes.Type(value = TimerNode.class, name = "timer"),
        @JsonSubTypes.Type(value = InitiatorNode.class, name = "initiator"),
        @JsonSubTypes.Type(value = MessageNode.class, name = "messageNode"),
        // @JsonSubTypes.Type(value = NotifyNode.class, name = "notify"),
        @JsonSubTypes.Type(value = EndNode.class, name = "end"),
        @JsonSubTypes.Type(value = TriggerNode.class, name = "trigger")
})
public abstract class Node implements Serializable {
    private static final long serialVersionUID = 132324315232123L;
    // 节点表单key
    private String formKey;
    // 节点id
    private String id;
    // 父节点id
    private String pid;
    // 节点名称
    private String nodeName;
    // 节点类型
    @JsonTypeId
    private String nodeType;
    // 执行监听器
    private List<NodeListener> executionListeners;
    // 子节点
    private Node childNode;
    // 服务任务(触发器)
    private String serviceTask;
    // 分支id
    @JsonIgnore
    private String branchId;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    protected List<NodeFormProperty> formProperties = new ArrayList<>();

    private Integer type;

    public abstract List<FlowElement> convert();

    public List<FlowableListener> buidExecutionListener() {
        if (!CollectionUtils.isEmpty(this.executionListeners)) {
            return this.executionListeners.stream().filter(l -> StringUtils.isNotBlank(l.getImplementation()))
                    .map(listener -> {
                        FlowableListener executionListener = new FlowableListener();
                        executionListener.setEvent(listener.getEvent());
                        executionListener.setImplementationType(listener.getImplementationType());
                        executionListener.setImplementation(listener.getImplementation());
                        return executionListener;
                    }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public SequenceFlow buildSequence(Node next) {
        String sourceRef;
        String targetRef;
        if (Objects.nonNull(next)) {
            sourceRef = next.getPid();
            targetRef = next.getId();
        } else {
            if (StringUtils.isNotBlank(this.branchId)) {
                sourceRef = this.id;
                targetRef = this.branchId;
            } else {
                throw new RuntimeException(String.format("节点 %s 的下一个节点不能为空", this.id));
            }
        }
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId(String.format("%s-%s", sourceRef, targetRef));
        sequenceFlow.setSourceRef(sourceRef);
        sequenceFlow.setTargetRef(targetRef);
        return sequenceFlow;
    }

}
