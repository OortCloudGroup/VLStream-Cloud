package com.ruoyi.workflow.convert.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;

import java.util.ArrayList;
import java.util.List;



@EqualsAndHashCode(callSuper = true)
@Data
public class EndNode extends Node {

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 结束节点
        EndEvent endEvent = new EndEvent();
        endEvent.setId(this.getId());
        endEvent.setName(this.getNodeName());
        endEvent.setExecutionListeners(this.buidExecutionListener());
        elements.add(endEvent);
        return elements;
    }
}
