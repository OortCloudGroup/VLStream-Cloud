/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.util.io.StringStreamSource;

import java.util.*;

/**
 * @author KonBAI
 * @createTime 2022/3/26 19:04
 */
public class ModelUtils {

    private static final BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

    /**
     * xml bpmnModelobject
     *
     * @param xml xml
     * @return bpmnModelobject
     */
    public static BpmnModel getBpmnModel(String xml) {
        return bpmnXMLConverter.convertToBpmnModel(new StringStreamSource(xml), false, false);
    }

    /**
     * bpmnModel xml
     *
     * @deprecated in will bpmn
     * @param bpmnModel bpmnModelobject
     * @return xml
     */
    @Deprecated
    public static String getBpmnXmlStr(BpmnModel bpmnModel) {
        return StrUtil.utf8Str(getBpmnXml(bpmnModel));
    }

    /**
     * bpmnModel xmlobject
     *
     * @deprecated in bpmn
     * @param bpmnModel bpmnModelobject
     * @return xml
     */
    @Deprecated
    public static byte[] getBpmnXml(BpmnModel bpmnModel) {
        return bpmnXMLConverter.convertToXML(bpmnModel);
    }

    /**
     * node, Get
     *
     * @param source node
     * @return
     */
    public static List<SequenceFlow> getElementIncomingFlows(FlowElement source) {
        List<SequenceFlow> sequenceFlows = new ArrayList<>();
        if (source instanceof FlowNode) {
            sequenceFlows = ((FlowNode) source).getIncomingFlows();
        }
        return sequenceFlows;
    }


    /**
     * node, Get
     *
     * @param source node
     * @return
     */
    public static List<SequenceFlow> getElementOutgoingFlows(FlowElement source) {
        List<SequenceFlow> sequenceFlows = new ArrayList<>();
        if (source instanceof FlowNode) {
            sequenceFlows = ((FlowNode) source).getOutgoingFlows();
        }
        return sequenceFlows;
    }

    /**
     * Get startnode
     *
     * @param model bpmnModelobject
     * @return startnode ( not startnode, null)
     */
    public static StartEvent getStartEvent(BpmnModel model) {
        Process process = model.getMainProcess();
        FlowElement startElement = process.getInitialFlowElement();
        if (startElement instanceof StartEvent) {
            return (StartEvent) startElement;
        }
        return getStartEvent(process.getFlowElements());
    }

    /**
     * Get startnode
     *
     * @param flowElements workflowelementcollection
     * @return startnode ( not startnode, null)
     */
    public static StartEvent getStartEvent(Collection<FlowElement> flowElements) {
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof StartEvent) {
                return (StartEvent) flowElement;
            }
        }
        return null;
    }

    /**
     * Get finishnode
     *
     * @param model bpmnModelobject
     * @return finishnode ( not startnode, null)
     */
    public static EndEvent getEndEvent(BpmnModel model) {
        Process process = model.getMainProcess();
        return getEndEvent(process.getFlowElements());
    }

    /**
     * Get finishnode
     *
     * @param flowElements workflowelementcollection
     * @return finishnode ( not startnode, null)
     */
    public static EndEvent getEndEvent(Collection<FlowElement> flowElements) {
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof EndEvent) {
                return (EndEvent) flowElement;
            }
        }
        return null;
    }

    public static UserTask getUserTaskByKey(BpmnModel model, String taskKey) {
        Process process = model.getMainProcess();
        FlowElement flowElement = process.getFlowElement(taskKey);
        if (flowElement instanceof UserTask) {
            return (UserTask) flowElement;
        }
        return null;
    }

    /**
     * Get workflowelementinfo
     *
     * @param model bpmnModelobject
     * @param flowElementId elementID
     * @return elementinfo
     */
    public static FlowElement getFlowElementById(BpmnModel model, String flowElementId) {
        Process process = model.getMainProcess();
        return process.getFlowElement(flowElementId);
    }

    /**
     * Get elementformKey ( startnode and usernode )
     *
     * @param flowElement element
     * @return formKey
     */
    public static String getFormKey(FlowElement flowElement) {
        if (flowElement != null) {
            if (flowElement instanceof StartEvent) {
                return ((StartEvent) flowElement).getFormKey();
            } else if (flowElement instanceof UserTask) {
                return ((UserTask) flowElement).getFormKey();
            }
        }
        return null;
    }

    /**
     * Get startnodeproperty value
     * @param model bpmnModelobject
     * @param name property
     * @return property value
     */
    public static String getStartEventAttributeValue(BpmnModel model, String name) {
        StartEvent startEvent = getStartEvent(model);
        return getElementAttributeValue(startEvent, name);
    }

    /**
     * Get finishnodeproperty value
     * @param model bpmnModelobject
     * @param name property
     * @return property value
     */
    public static String getEndEventAttributeValue(BpmnModel model, String name) {
        EndEvent endEvent = getEndEvent(model);
        return getElementAttributeValue(endEvent, name);
    }

    /**
     * Get usertasknodeproperty value
     * @param model bpmnModelobject
     * @param taskKey taskKey
     * @param name property
     * @return property value
     */
    public static String getUserTaskAttributeValue(BpmnModel model, String taskKey, String name) {
        UserTask userTask = getUserTaskByKey(model, taskKey);
        return getElementAttributeValue(userTask, name);
    }

    /**
     * Get elementproperty value
     * @param baseElement workflowelement
     * @param name property
     * @return property value
     */
    public static String getElementAttributeValue(BaseElement baseElement, String name) {
        if (baseElement != null) {
            List<ExtensionAttribute> attributes = baseElement.getAttributes().get(name);
            if (attributes != null && !attributes.isEmpty()) {
                attributes.iterator().next().getValue();
                Iterator<ExtensionAttribute> attrIterator = attributes.iterator();
                if(attrIterator.hasNext()) {
                    ExtensionAttribute attribute = attrIterator.next();
                    return attribute.getValue();
                }
            }
        }
        return null;
    }

    public static boolean isMultiInstance(BpmnModel model, String taskKey) {
        UserTask userTask = getUserTaskByKey(model, taskKey);
        if (ObjectUtil.isNotNull(userTask)) {
            return userTask.hasMultiInstanceLoopCharacteristics();
        }
        return false;
    }

    /**
     * Get all usertasknode
     *
     * @param model bpmnModelobject
     * @return usertasknode
     */
    public static Collection<UserTask> getAllUserTaskEvent(BpmnModel model) {
        Process process = model.getMainProcess();
        Collection<FlowElement> flowElements = process.getFlowElements();
        return getAllUserTaskEvent(flowElements, null);
    }

    /**
     * Get all usertasknode
     * @param flowElements workflowelementcollection
     * @param allElements all workflowelementcollection
     * @return usertasknode
     */
    public static Collection<UserTask> getAllUserTaskEvent(Collection<FlowElement> flowElements, Collection<UserTask> allElements) {
        allElements = allElements == null ? new ArrayList<>() : allElements;
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask) {
                allElements.add((UserTask) flowElement);
            }
            if (flowElement instanceof SubProcess) {
                // sub workflow, Get sub workflow
                allElements = getAllUserTaskEvent(((SubProcess) flowElement).getFlowElements(), allElements);
            }
        }
        return allElements;
    }

    /**
     * find node usertask
     * @param source node
     * @return
     */
    public static List<UserTask> findNextUserTasks(FlowElement source) {
        return findNextUserTasks(source, null, null);
    }

    /**
     * find node usertask
     * @param source node
     * @param hasSequenceFlow already ID, Check whether
     * @param userTaskList usertask
     * @return
     */
    public static List<UserTask> findNextUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        hasSequenceFlow = Optional.ofNullable(hasSequenceFlow).orElse(new HashSet<>());
        userTaskList = Optional.ofNullable(userTaskList).orElse(new ArrayList<>());
        // Get
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);
        if (!sequenceFlows.isEmpty()) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
                if (targetFlowElement instanceof UserTask) {
                    // node to usertask, in
                    userTaskList.add((UserTask) targetFlowElement);
                } else {
                    // node non-usertask, find node
                    findNextUserTasks(targetFlowElement, hasSequenceFlow, userTaskList);
                }
            }
        }
        return userTaskList;
    }

    /**
     * from after before , Check node current nodewhether is
     * in sub workflow in , in from sub workflow workflow
     * @param source node
     * @param target node
     * @param visitedElements already ID, Check whether
     * @return
     */
    public static boolean isSequentialReachable(FlowElement source, FlowElement target, Set<String> visitedElements) {
        visitedElements = visitedElements == null ? new HashSet<>() : visitedElements;
        if (source instanceof StartEvent && isInEventSubprocess(source)) {
            return false;
        }

        // , Get
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);
        if (sequenceFlows != null && sequenceFlows.size() > 0) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (visitedElements.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                visitedElements.add(sequenceFlow.getId());
                FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
                // in node, ,
                if (target.getId().equals(sourceFlowElement.getId())) {
                    continue;
                }
                // if node to ,
                if (sourceFlowElement instanceof ParallelGateway) {
                    return false;
                }
                // then
                boolean isSequential = isSequentialReachable(sourceFlowElement, target, visitedElements);
                if (!isSequential) {
                    return false;
                }
            }
        }
        return true;
    }

    protected static boolean isInEventSubprocess(FlowElement flowElement) {
        FlowElementsContainer flowElementsContainer = flowElement.getParentContainer();
        while (flowElementsContainer != null) {
            if (flowElementsContainer instanceof EventSubProcess) {
                return true;
            }

            if (flowElementsContainer instanceof FlowElement) {
                flowElementsContainer = ((FlowElement) flowElementsContainer).getParentContainer();
            } else {
                flowElementsContainer = null;
            }
        }
        return false;
    }
}
