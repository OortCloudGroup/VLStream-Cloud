/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.flow;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Xuan xuan
 * @date 2021/4/19 20:51
 */
public class FindNextNodeUtil {

    /**
     * Get usertask
     *
     * @param repositoryService
     * @param map
     * @return
     */
    public static List<UserTask> getNextUserTasks(RepositoryService repositoryService, org.flowable.task.api.Task task, Map<String, Object> map) {
        List<UserTask> data = new ArrayList<>();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process mainProcess = bpmnModel.getMainProcess();
        Collection<FlowElement> flowElements = mainProcess.getFlowElements();
        String key = task.getTaskDefinitionKey();
        FlowElement flowElement = bpmnModel.getFlowElement(key);
        next(flowElements, flowElement, map, data);
        return data;
    }

    public static void next(Collection<FlowElement> flowElements, FlowElement flowElement, Map<String, Object> map, List<UserTask> nextUser) {
        // if is finishnode
        if (flowElement instanceof EndEvent) {
            // if is sub task finishnode
            if (getSubProcess(flowElements, flowElement) != null) {
                flowElement = getSubProcess(flowElements, flowElement);
            }
        }
        // Get Task info--
        List<SequenceFlow> outGoingFlows = null;
        if (flowElement instanceof Task) {
            outGoingFlows = ((Task) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof Gateway) {
            outGoingFlows = ((Gateway) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof StartEvent) {
            outGoingFlows = ((StartEvent) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof SubProcess) {
            outGoingFlows = ((SubProcess) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof CallActivity) {
            outGoingFlows = ((CallActivity) flowElement).getOutgoingFlows();
        }
        if (outGoingFlows != null && outGoingFlows.size() > 0) {
            // all -- correctExecute
            for (SequenceFlow sequenceFlow : outGoingFlows) {
                // 1. , to true
                // 2.
                String expression = sequenceFlow.getConditionExpression();
                if (expression == null || expressionResult(map, expression.substring(expression.lastIndexOf("{") + 1, expression.lastIndexOf("}")))) {
                    // node
                    String nextFlowElementID = sequenceFlow.getTargetRef();
                    if (checkSubProcess(nextFlowElementID, flowElements, nextUser)) {
                        continue;
                    }

                    // Query node info
                    FlowElement nextFlowElement = getFlowElementById(nextFlowElementID, flowElements);
                    // workflow
                    if (nextFlowElement instanceof CallActivity) {
                        CallActivity ca = (CallActivity) nextFlowElement;
                        if (ca.getLoopCharacteristics() != null) {
                            UserTask userTask = new UserTask();
                            userTask.setId(ca.getId());

                            userTask.setId(ca.getId());
                            userTask.setLoopCharacteristics(ca.getLoopCharacteristics());
                            userTask.setName(ca.getName());
                            nextUser.add(userTask);
                        }
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // usertask
                    if (nextFlowElement instanceof UserTask) {
                        nextUser.add((UserTask) nextFlowElement);
                    }
                    //
                    else if (nextFlowElement instanceof ExclusiveGateway) {
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    //
                    else if (nextFlowElement instanceof ParallelGateway) {
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // task
                    else if (nextFlowElement instanceof ReceiveTask) {
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // servicetask
                    else if (nextFlowElement instanceof ServiceTask) {
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // sub task
                    else if (nextFlowElement instanceof StartEvent) {
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // finishnode
                    else if (nextFlowElement instanceof EndEvent) {
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                }
            }
        }
    }

    /**
     * Check whether is instance sub workflowand need to Set collection variable
     */
    public static boolean checkSubProcess(String Id, Collection<FlowElement> flowElements, List<UserTask> nextUser) {
        for (FlowElement flowElement1 : flowElements) {
            if (flowElement1 instanceof SubProcess && flowElement1.getId().equals(Id)) {

                SubProcess sp = (SubProcess) flowElement1;
                if (sp.getLoopCharacteristics() != null) {
                    String inputDataItem = sp.getLoopCharacteristics().getInputDataItem();
                    UserTask userTask = new UserTask();
                    userTask.setId(sp.getId());
                    userTask.setLoopCharacteristics(sp.getLoopCharacteristics());
                    userTask.setName(sp.getName());
                    nextUser.add(userTask);
                    return true;
                }
            }
        }

        return false;

    }

    /**
     * Query node whether sub task in node, if is , sub task
     *
     * @param flowElements full workflow nodecollection
     * @param flowElement current node
     * @return
     */
    public static FlowElement getSubProcess(Collection<FlowElement> flowElements, FlowElement flowElement) {
        for (FlowElement flowElement1 : flowElements) {
            if (flowElement1 instanceof SubProcess) {
                for (FlowElement flowElement2 : ((SubProcess) flowElement1).getFlowElements()) {
                    if (flowElement.equals(flowElement2)) {
                        return flowElement1;
                    }
                }
            }
        }
        return null;
    }


    /**
     * IDQuery workflownodeobject, if is sub task, sub task startnode
     *
     * @param Id nodeID
     * @param flowElements workflownodecollection
     * @return
     */
    public static FlowElement getFlowElementById(String Id, Collection<FlowElement> flowElements) {
        for (FlowElement flowElement : flowElements) {
            if (flowElement.getId().equals(Id)) {
                // if is sub task, Query sub task startnode
                if (flowElement instanceof SubProcess) {
                    return getStartFlowElement(((SubProcess) flowElement).getFlowElements());
                }
                return flowElement;
            }
            if (flowElement instanceof SubProcess) {
                FlowElement flowElement1 = getFlowElementById(Id, ((SubProcess) flowElement).getFlowElements());
                if (flowElement1 != null) {
                    return flowElement1;
                }
            }
        }
        return null;
    }

    /**
     * workflow startnode
     *
     * @param flowElements nodecollection
     * @description:
     */
    public static FlowElement getStartFlowElement(Collection<FlowElement> flowElements) {
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof StartEvent) {
                return flowElement;
            }
        }
        return null;
    }

    /**
     * Validate el
     *
     * @param map
     * @param expression
     * @return
     */
    public static boolean expressionResult(Map<String, Object> map, String expression) {
        Expression exp = AviatorEvaluator.compile(expression);
        final Object execute = exp.execute(map);
        return Boolean.parseBoolean(String.valueOf(execute));
    }
}
