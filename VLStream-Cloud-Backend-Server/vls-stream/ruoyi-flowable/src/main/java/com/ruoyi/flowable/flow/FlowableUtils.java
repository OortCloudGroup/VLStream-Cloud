/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.flow;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.util.*;

/**
 * @author XuanXuan
 * @date 2021-04-03 23:57
 */
@Slf4j
public class FlowableUtils {

    /**
     * node, Get
     * @param source
     * @return
     */
    public static List<SequenceFlow> getElementIncomingFlows(FlowElement source) {
        List<SequenceFlow> sequenceFlows = null;
        if (source instanceof FlowNode) {
            sequenceFlows = ((FlowNode) source).getIncomingFlows();
        } else if (source instanceof Gateway) {
            sequenceFlows = ((Gateway) source).getIncomingFlows();
        } else if (source instanceof SubProcess) {
            sequenceFlows = ((SubProcess) source).getIncomingFlows();
        } else if (source instanceof StartEvent) {
            sequenceFlows = ((StartEvent) source).getIncomingFlows();
        } else if (source instanceof EndEvent) {
            sequenceFlows = ((EndEvent) source).getIncomingFlows();
        }
        return sequenceFlows;
    }

    /**
     * node, Get
     * @param source
     * @return
     */
    public static List<SequenceFlow> getElementOutgoingFlows(FlowElement source) {
        List<SequenceFlow> sequenceFlows = null;
        if (source instanceof FlowNode) {
            sequenceFlows = ((FlowNode) source).getOutgoingFlows();
        } else if (source instanceof Gateway) {
            sequenceFlows = ((Gateway) source).getOutgoingFlows();
        } else if (source instanceof SubProcess) {
            sequenceFlows = ((SubProcess) source).getOutgoingFlows();
        } else if (source instanceof StartEvent) {
            sequenceFlows = ((StartEvent) source).getOutgoingFlows();
        } else if (source instanceof EndEvent) {
            sequenceFlows = ((EndEvent) source).getOutgoingFlows();
        }
        return sequenceFlows;
    }

    /**
     * Get full node , sub workflownode
     * @param flowElements
     * @param allElements
     * @return
     */
    public static Collection<FlowElement> getAllElements(Collection<FlowElement> flowElements, Collection<FlowElement> allElements) {
        allElements = allElements == null ? new ArrayList<>() : allElements;

        for (FlowElement flowElement : flowElements) {
            allElements.add(flowElement);
            if (flowElement instanceof SubProcess) {
                // sub workflow, Get sub workflow
                allElements = FlowableUtils.getAllElements(((SubProcess) flowElement).getFlowElements(), allElements);
            }
        }
        return allElements;
    }

    /**
     * Get tasknode , before
     * @param source node
     * @param hasSequenceFlow already ID, Check whether
     * @param userTaskList already usertasknode
     * @return
     */
    public static List<UserTask> iteratorFindParentUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // if node to startnode, in sub node, sub node
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            userTaskList = iteratorFindParentUserTasks(source.getSubProcess(), hasSequenceFlow, userTaskList);
        }

        // , Get
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                // to usernode, Add node
                if (sequenceFlow.getSourceFlowElement() instanceof UserTask) {
                    userTaskList.add((UserTask) sequenceFlow.getSourceFlowElement());
                    continue;
                }
                // to sub workflow, sub workflowstartnode node
                if (sequenceFlow.getSourceFlowElement() instanceof SubProcess) {
                    // Get sub workflowusertasknode
                    List<UserTask> childUserTaskList = findChildProcessUserTasks((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, null);
                    // if node, node, ,
                    if (childUserTaskList != null && childUserTaskList.size() > 0) {
                        userTaskList.addAll(childUserTaskList);
                        continue;
                    }
                }
                //
                userTaskList = iteratorFindParentUserTasks(sequenceFlow.getSourceFlowElement(), hasSequenceFlow, userTaskList);
            }
        }
        return userTaskList;
    }

    /**
     * in tasknode, Get sub tasknode , after
     * @param source node
     * @param runTaskKeyList in task Key, Validate tasknodewhether is in node
     * @param hasSequenceFlow already ID, Check whether
     * @param userTaskList need to usertask
     * @return
     */
    public static List<UserTask> iteratorFindChildUserTasks(FlowElement source, List<String> runTaskKeyList, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;

        // if node to startnode, in sub node, sub node
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            userTaskList = iteratorFindChildUserTasks(source.getSubProcess(), runTaskKeyList, hasSequenceFlow, userTaskList);
        }

        // , Get
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                // if to usertask , tasknode Key in task in in ,
                if (sequenceFlow.getTargetFlowElement() instanceof UserTask && runTaskKeyList.contains((sequenceFlow.getTargetFlowElement()).getId())) {
                    userTaskList.add((UserTask) sequenceFlow.getTargetFlowElement());
                    continue;
                }
                // if node to sub workflownode , from node in nodestartGet
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    List<UserTask> childUserTaskList = iteratorFindChildUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), runTaskKeyList, hasSequenceFlow, null);
                    // if node, node, ,
                    if (childUserTaskList != null && childUserTaskList.size() > 0) {
                        userTaskList.addAll(childUserTaskList);
                        continue;
                    }
                }
                //
                userTaskList = iteratorFindChildUserTasks(sequenceFlow.getTargetFlowElement(), runTaskKeyList, hasSequenceFlow, userTaskList);
            }
        }
        return userTaskList;
    }

    /**
     * Get sub workflowusertasknode
     * @param source node
     * @param hasSequenceFlow already ID, Check whether
     * @param userTaskList need to usertask
     * @return
     */
    public static List<UserTask> findChildProcessUserTasks(FlowElement source, Set<String> hasSequenceFlow, List<UserTask> userTaskList) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        userTaskList = userTaskList == null ? new ArrayList<>() : userTaskList;

        // , Get
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                // if to usertask , tasknode Key in task in in ,
                if (sequenceFlow.getTargetFlowElement() instanceof UserTask) {
                    userTaskList.add((UserTask) sequenceFlow.getTargetFlowElement());
                    continue;
                }
                // if node to sub workflownode , from node in nodestartGet
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    List<UserTask> childUserTaskList = findChildProcessUserTasks((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, null);
                    // if node, node, ,
                    if (childUserTaskList != null && childUserTaskList.size() > 0) {
                        userTaskList.addAll(childUserTaskList);
                        continue;
                    }
                }
                //
                userTaskList = findChildProcessUserTasks(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, userTaskList);
            }
        }
        return userTaskList;
    }

    /**
     * from after before , Get all
     * @param source node
     * @param passRoads already collection
     * @param hasSequenceFlow already ID, Check whether
     * @param targets
     * @param dirtyRoads to data , to need to , set
     * @return
     */
    public static Set<String> iteratorFindDirtyRoads(FlowElement source, List<String> passRoads, Set<String> hasSequenceFlow, List<String> targets, Set<String> dirtyRoads) {
        passRoads = passRoads == null ? new ArrayList<>() : passRoads;
        dirtyRoads = dirtyRoads == null ? new HashSet<>() : dirtyRoads;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // if node to startnode, in sub node, sub node
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            dirtyRoads = iteratorFindDirtyRoads(source.getSubProcess(), passRoads, hasSequenceFlow, targets, dirtyRoads);
        }

        // , Get
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                // Add
                passRoads.add(sequenceFlow.getSourceFlowElement().getId());
                // if to , to , in , after
                if (targets.contains(sequenceFlow.getSourceFlowElement().getId())) {
                    dirtyRoads.addAll(passRoads);
                    continue;
                }
                // if node to startnode, in sub node, sub node
                if (sequenceFlow.getSourceFlowElement() instanceof SubProcess) {
                    dirtyRoads = findChildProcessAllDirtyRoad((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, dirtyRoads);
                    // whether in sub workflow , true is , false
                    Boolean isInChildProcess = dirtyTargetInChildProcess((StartEvent) ((SubProcess) sequenceFlow.getSourceFlowElement()).getFlowElements().toArray()[0], null, targets, null);
                    if (isInChildProcess) {
                        // already in sub workflow , finish
                        continue;
                    }
                }
                //
                dirtyRoads = iteratorFindDirtyRoads(sequenceFlow.getSourceFlowElement(), passRoads, hasSequenceFlow, targets, dirtyRoads);
            }
        }
        return dirtyRoads;
    }

    /**
     * Get sub workflow
     * , then is sub workflow, also will sub workflow usertasknode, sub workflow in node full is
     * @param source node
     * @param hasSequenceFlow already ID, Check whether
     * @param dirtyRoads to data , to need to , set
     * @return
     */
    public static Set<String> findChildProcessAllDirtyRoad(FlowElement source, Set<String> hasSequenceFlow, Set<String> dirtyRoads) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        dirtyRoads = dirtyRoads == null ? new HashSet<>() : dirtyRoads;

        // , Get
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                //
                dirtyRoads.add(sequenceFlow.getTargetFlowElement().getId());
                // if node to sub workflownode , from node in nodestartGet
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    dirtyRoads = findChildProcessAllDirtyRoad((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, dirtyRoads);
                }
                //
                dirtyRoads = findChildProcessAllDirtyRoad(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, dirtyRoads);
            }
        }
        return dirtyRoads;
    }

    /**
     * Check finishnodewhether in sub workflow
     * @param source node
     * @param hasSequenceFlow already ID, Check whether
     * @param targets Check nodewhether in sub workflow , only need to in , only sub workflow to
     * @param inChildProcess whether in sub workflow , true is , false
     * @return
     */
    public static Boolean dirtyTargetInChildProcess(FlowElement source, Set<String> hasSequenceFlow, List<String> targets, Boolean inChildProcess) {
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;
        inChildProcess = inChildProcess == null ? false : inChildProcess;

        // , Get
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null && !inChildProcess) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                // if in sub workflow in , only sub workflow to
                if (targets.contains(sequenceFlow.getTargetFlowElement().getId())) {
                    inChildProcess = true;
                    break;
                }
                // if node to sub workflownode , from node in nodestartGet
                if (sequenceFlow.getTargetFlowElement() instanceof SubProcess) {
                    inChildProcess = dirtyTargetInChildProcess((FlowElement) (((SubProcess) sequenceFlow.getTargetFlowElement()).getFlowElements().toArray()[0]), hasSequenceFlow, targets, inChildProcess);
                }
                //
                inChildProcess = dirtyTargetInChildProcess(sequenceFlow.getTargetFlowElement(), hasSequenceFlow, targets, inChildProcess);
            }
        }
        return inChildProcess;
    }

    /**
     * from after before , Check node current nodewhether is
     * in sub workflow in , in from sub workflow workflow
     * @param source node
     * @param isSequential whether
     * @param hasSequenceFlow already ID, Check whether
     * @param targetKsy node
     * @return
     */
    public static Boolean iteratorCheckSequentialReferTarget(FlowElement source, String targetKsy, Set<String> hasSequenceFlow, Boolean isSequential) {
        isSequential = isSequential == null ? true : isSequential;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // if node to startnode, in sub node, sub node
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            isSequential = iteratorCheckSequentialReferTarget(source.getSubProcess(), targetKsy, hasSequenceFlow, isSequential);
        }

        // , Get
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                // if node already Check to , after need to Execute ,
                if (isSequential == false) {
                    break;
                }
                // in node, ,
                if (targetKsy.equals(sequenceFlow.getSourceFlowElement().getId())) {
                    continue;
                }
                if (sequenceFlow.getSourceFlowElement() instanceof StartEvent) {
                    isSequential = false;
                    break;
                }
                // then
                isSequential = iteratorCheckSequentialReferTarget(sequenceFlow.getSourceFlowElement(), targetKsy, hasSequenceFlow, isSequential);
            }
        }
        return isSequential;
    }

    /**
     * from after before , Get node all
     * in sub workflow, is in workflow
     * @param source node
     * @param passRoads already collection
     * @param roads
     * @return
     */
    public static List<List<UserTask>> findRoad(FlowElement source, List<UserTask> passRoads, Set<String> hasSequenceFlow, List<List<UserTask>> roads) {
        passRoads = passRoads == null ? new ArrayList<>() : passRoads;
        roads = roads == null ? new ArrayList<>() : roads;
        hasSequenceFlow = hasSequenceFlow == null ? new HashSet<>() : hasSequenceFlow;

        // if node to startnode, in sub node, sub node
        if (source instanceof StartEvent && source.getSubProcess() != null) {
            roads = findRoad(source.getSubProcess(), passRoads, hasSequenceFlow, roads);
        }

        // , Get
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null && sequenceFlows.size() != 0) {
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                //
                if (sequenceFlow.getSourceFlowElement() instanceof UserTask) {
                    passRoads.add((UserTask) sequenceFlow.getSourceFlowElement());
                }
                //
                roads = findRoad(sequenceFlow.getSourceFlowElement(), passRoads, hasSequenceFlow, roads);
            }
        } else {
            //
            roads.add(passRoads);
        }
        return roads;
    }

    /**
     * history nodedata , and data
     * @param allElements full nodeinfo
     * @param historicTaskInstanceList history taskinstanceinfo, data start
     * @return
     */
    public static List<String> historicTaskInstanceClean(Collection<FlowElement> allElements, List<HistoricTaskInstance> historicTaskInstanceList) {
        // will node
        List<String> multiTask = new ArrayList<>();
        allElements.forEach(flowElement -> {
            if (flowElement instanceof UserTask) {
                // if node to to will to , node to will node
                if (((UserTask) flowElement).getBehavior() instanceof ParallelMultiInstanceBehavior || ((UserTask) flowElement).getBehavior() instanceof SequentialMultiInstanceBehavior) {
                    multiTask.add(flowElement.getId());
                }
            }
        });
        // loop , LIFO: after
        Stack<HistoricTaskInstance> stack = new Stack<>();
        historicTaskInstanceList.forEach(item -> stack.push(item));
        // after history taskinstance
        List<String> lastHistoricTaskInstanceList = new ArrayList<>();
        // in can only , in data data , need to history nodedata
        // usertask key
        StringBuilder userTaskKey = null;
        // task key, in
        List<String> deleteKeyList = new ArrayList<>();
        // data
        List<Set<String>> dirtyDataLineList = new ArrayList<>();
        // will , will instance 1 , need to data
        // will Process
        int multiIndex = -1;
        // will Process key
        StringBuilder multiKey = null;
        // will Process operation
        boolean multiOpera = false;
        while (!stack.empty()) {
            // from start userTaskKey is key
            // whether is data
            final boolean[] isDirtyData = {false};
            for (Set<String> oldDirtyDataLine : dirtyDataLineList) {
                if (oldDirtyDataLine.contains(stack.peek().getTaskDefinitionKey())) {
                    isDirtyData[0] = true;
                }
            }
            // Delete is empty, from datastart
            // MI_END: will after, not node Delete , in Process
            if (stack.peek().getDeleteReason() != null && !stack.peek().getDeleteReason().equals("MI_END")) {
                // to
                String dirtyPoint = "";
                if (stack.peek().getDeleteReason().indexOf("Change activity to ") >= 0) {
                    dirtyPoint = stack.peek().getDeleteReason().replace("Change activity to ", "");
                }
                // will Delete
                if (stack.peek().getDeleteReason().indexOf("Change parent activity to ") >= 0) {
                    dirtyPoint = stack.peek().getDeleteReason().replace("Change parent activity to ", "");
                }
                FlowElement dirtyTask = null;
                // Get node
                // if is , will data ,
                for (FlowElement flowElement : allElements) {
                    if (flowElement.getId().equals(stack.peek().getTaskDefinitionKey())) {
                        dirtyTask = flowElement;
                    }
                }
                // Get data
                Set<String> dirtyDataLine = FlowableUtils.iteratorFindDirtyRoads(dirtyTask, null, null, Arrays.asList(dirtyPoint.split(",")), null);
                // also is ,
                dirtyDataLine.add(stack.peek().getTaskDefinitionKey());
                log.info(stack.peek().getTaskDefinitionKey() + "点脏路线集合：" + dirtyDataLine);
                // is full new need to
                boolean isNewDirtyData = true;
                for (int i = 0; i < dirtyDataLineList.size(); i++) {
                    // if node in , can is node,
                    // , before node to , only , also then is full
                    if (dirtyDataLineList.get(i).contains(userTaskKey.toString())) {
                        isNewDirtyData = false;
                        dirtyDataLineList.get(i).addAll(dirtyDataLine);
                    }
                }
                // already full new
                if (isNewDirtyData) {
                    // deleteKey , Generate new instancerecord , deleteKey is value
                    // , after Generate instancerecord then is record
                    // to Generate Key, from Delete in Get , to in
                    deleteKeyList.add(dirtyPoint + ",");
                    dirtyDataLineList.add(dirtyDataLine);
                }
                // after, in
                isDirtyData[0] = true;
            }
            // if is , is data, history instance Key
            if (!isDirtyData[0]) {
                lastHistoricTaskInstanceList.add(stack.peek().getTaskDefinitionKey());
            }
            // Validate whether finish
            for (int i = 0; i < deleteKeyList.size(); i ++) {
                // if data will , record and Key, after , will data start
                if (multiKey == null && multiTask.contains(stack.peek().getTaskDefinitionKey())
                        && deleteKeyList.get(i).contains(stack.peek().getTaskDefinitionKey())) {
                    multiIndex = i;
                    multiKey = new StringBuilder(stack.peek().getTaskDefinitionKey());
                }
                // will dataProcess , node will null / empty
                // if in will data in Key , will data in node then finish , will data
                if (multiKey != null && !multiKey.toString().equals(stack.peek().getTaskDefinitionKey())) {
                    deleteKeyList.set(multiIndex , deleteKeyList.get(multiIndex).replace(stack.peek().getTaskDefinitionKey() + ",", ""));
                    multiKey = null;
                    // finish Validate Delete
                    multiOpera = true;
                }
                // dataProcess
                // after data, data Process , Delete datainfo
                // data new instance in whether data
                if (multiKey == null && deleteKeyList.get(i).contains(stack.peek().getTaskDefinitionKey())) {
                    // Delete
                    deleteKeyList.set(i , deleteKeyList.get(i).replace(stack.peek().getTaskDefinitionKey() + ",", ""));
                }
                // if in element , datafinish
                if ("".equals(deleteKeyList.get(i))) {
                    // Delete data
                    deleteKeyList.remove(i);
                    dirtyDataLineList.remove(i);
                    break;
                }
            }
            // will dataProcess need to in loop Process , can
            // will data is before will , is Validate
            if (multiOpera && deleteKeyList.size() > multiIndex && "".equals(deleteKeyList.get(multiIndex))) {
                // Delete data
                deleteKeyList.remove(multiIndex);
                dirtyDataLineList.remove(multiIndex);
                multiIndex = -1;
                multiOpera = false;
            }
            // pop() method and peek() method , in value , will value from in
            // new userTaskKey in loop in
            userTaskKey = new StringBuilder(stack.pop().getTaskDefinitionKey());
        }
        log.info("清洗后的历史节点数据：" + lastHistoricTaskInstanceList);
        return lastHistoricTaskInstanceList;
    }

    /**
     * Get workflow not node
     * @param bpmnModel workflowmodel
     * @param unfinishedTaskSet not finish tasknode
     * @param finishedSequenceFlowSet already
     * @param finishedTaskSet already tasknode
     * @return
     */
    public static Set<String> dfsFindRejects(BpmnModel bpmnModel, Set<String> unfinishedTaskSet, Set<String> finishedSequenceFlowSet, Set<String> finishedTaskSet) {
        if (ObjectUtil.isNull(bpmnModel)) {
            throw new ServiceException("流程模型不存在");
        }
        Collection<FlowElement> allElements = getAllElements(bpmnModel.getMainProcess().getFlowElements(), null);
        Set<String> rejectedSet = new HashSet<>();
        for (FlowElement flowElement : allElements) {
            // usernode not finishelement
            if (flowElement instanceof UserTask && unfinishedTaskSet.contains(flowElement.getId())) {
                List<String> hasSequenceFlow = iteratorFindFinishes(flowElement, null);
                List<String> rejects = iteratorFindRejects(flowElement, finishedSequenceFlowSet, finishedTaskSet, hasSequenceFlow, null);
                rejectedSet.addAll(rejects);
            }
        }
        return rejectedSet;
    }

    /**
     * Get node , before
     * @param source node
     * @param hasSequenceFlow already ID, Check whether
     * @return
     */
    public static List<String> iteratorFindFinishes(FlowElement source, List<String> hasSequenceFlow) {
        hasSequenceFlow = hasSequenceFlow == null ? new ArrayList<>() : hasSequenceFlow;

        // , Get
        List<SequenceFlow> sequenceFlows = getElementIncomingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement finishedElement = sequenceFlow.getSourceFlowElement();
                // to sub workflow, sub workflowstartnode node
                if (finishedElement instanceof SubProcess) {
                    FlowElement firstElement = (StartEvent) ((SubProcess) finishedElement).getFlowElements().toArray()[0];
                    // Get sub workflow
                    hasSequenceFlow.addAll(iteratorFindFinishes(firstElement, null));
                }
                //
                hasSequenceFlow = iteratorFindFinishes(finishedElement, hasSequenceFlow);
            }
        }
        return hasSequenceFlow;
    }

    /**
     * in tasknode, Get sub tasknode , after
     * @param source node
     * @param finishedSequenceFlowSet already
     * @param finishedTaskSet already tasknode
     * @param hasSequenceFlow already ID, Check whether
     * @param rejectedList not element
     * @return
     */
    public static List<String> iteratorFindRejects(FlowElement source, Set<String> finishedSequenceFlowSet, Set<String> finishedTaskSet
        , List<String> hasSequenceFlow, List<String> rejectedList) {
        hasSequenceFlow = hasSequenceFlow == null ? new ArrayList<>() : hasSequenceFlow;
        rejectedList = rejectedList == null ? new ArrayList<>() : rejectedList;

        // , Get
        List<SequenceFlow> sequenceFlows = getElementOutgoingFlows(source);

        if (sequenceFlows != null) {
            // loop element
            for (SequenceFlow sequenceFlow: sequenceFlows) {
                // if , loop , loop
                if (hasSequenceFlow.contains(sequenceFlow.getId())) {
                    continue;
                }
                // already
                hasSequenceFlow.add(sequenceFlow.getId());
                FlowElement targetElement = sequenceFlow.getTargetFlowElement();
                // not node
                if (finishedTaskSet.contains(targetElement.getId())) {
                    rejectedList.add(targetElement.getId());
                }
                // not
                if (finishedSequenceFlowSet.contains(sequenceFlow.getId())) {
                    rejectedList.add(sequenceFlow.getId());
                }
                // if node to sub workflownode , from node in nodestartGet
                if (targetElement instanceof SubProcess) {
                    FlowElement firstElement = (FlowElement) (((SubProcess) targetElement).getFlowElements().toArray()[0]);
                    List<String> childList = iteratorFindRejects(firstElement, finishedSequenceFlowSet, finishedTaskSet, hasSequenceFlow, null);
                    // if node, node, ,
                    if (childList != null && childList.size() > 0) {
                        rejectedList.addAll(childList);
                        continue;
                    }
                }
                //
                rejectedList = iteratorFindRejects(targetElement, finishedSequenceFlowSet, finishedTaskSet, hasSequenceFlow, rejectedList);
            }
        }
        return rejectedList;
    }

}
