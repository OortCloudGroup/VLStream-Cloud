/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workorder.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.jackson.ApiResponse;
import com.ruoyi.common.utils.ApiHeaderUtil;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.common.enums.ProcessStatus;
import com.ruoyi.flowable.common.enums.WorkOrderStatus;
import com.ruoyi.flowable.common.enums.WorkOrderUrgency;
import com.ruoyi.flowable.core.WFormInfo;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.flowable.utils.ProcessUtils;
import com.ruoyi.flowable.utils.TaskUtils;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.domain.WorkOrderSynthesis;
import com.ruoyi.workflow.domain.bo.ProcessViewLogBo;
import com.ruoyi.workflow.service.*;
import com.ruoyi.workflow.service.impl.CategoryLookupService;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.bo.WorkOrderBo;
import com.ruoyi.workorder.domain.vo.WorkOrderVo;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import com.ruoyi.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.flowable.common.engine.api.query.Query;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static com.ruoyi.flowable.common.constant.ProcessConstants.PROCESS_STATUS_KEY;

/**
 * work orderService layer Process
 *
 * @author
 * @date 2025-01-02
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements IWorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final RepositoryService repositoryService;
    private final UserService userService;
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ProcessEngine processEngine;
    private final IWorkOrderAppService workOrderAppService;
    private final IWorkOrderSynthesisService workOrderSynthesisService;
    private final CategoryLookupService categoryLookupService;
    private final ISysDeptService sysDeptService;
    private final ISysUserService sysUserService;
    private final SysUserServiceImpl sysUserServiceImpl;
    private final IWfUserInterfaceFieldService wfUserInterfaceFieldService;
    private final IProcessViewLogService processViewLogService;

    @Resource
    @Lazy
    IWfProcessService processService;

//    @Value("${http.apaas-workflowforms}")
    private String workFlowFormsUrl;

    /**
     * Query work order
     */
    @Override
    public WorkOrderVo queryById(String id) {
        return workOrderMapper.selectVoById(id);
    }

    /**
     * Query work order list
     */
    @Override
    public TableDataInfo<WorkOrderVo> queryPageList(WorkOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, false);
        Page<WorkOrderVo> result = workOrderMapper.selectVoPage(pageQuery.build(), lqw);
        // result.getRecords().forEach(item -> {
        // item.setWorkorderId();
        // });
        return TableDataInfo.build(result);
    }

    @Override
    public TableDataInfo<WorkOrderVo> selectPageOwnWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
            PageQuery pageQuery,
            SysUser sysUser) {
        workOrderBo.setWorkOrderAppAll(processQuery.getWorkOrderAppAll());
        workOrderBo.setWorkOrderSynthesisAll(processQuery.getWorkOrderSynthesisAll());
        // PageQuery build() method Build object
        Page<WorkOrderVo> page = pageQuery.build();
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).startedBy(sysUser.getUserId())
                .orderByProcessInstanceStartTime().desc();
        // Build
        ProcessUtils.buildProcessSearch(historicProcessInstanceQuery, processQuery, processEngine, sysUser);

        // workflow
        if (com.ruoyi.common.utils.StringUtils.isNotBlank(processQuery.getState())) {
            historicProcessInstanceQuery.variableValueEquals(PROCESS_STATUS_KEY,
                    processQuery.getState());
        }

        List<HistoricProcessInstance> result = new ArrayList<>();
        List<HistoricProcessInstance> filteredDef;
        long pageTotal;
        if (Boolean.TRUE.equals(processQuery.getWorkOrderAppAll())) {
            List<String> appList = workOrderAppService.list().stream().map(item -> item.getAppId())
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                for (String appId : appList) {
                    List<HistoricProcessInstance> list = historicProcessInstanceQuery.processDefinitionCategory(appId)
                            .list();
                    if (ObjectUtil.isNotEmpty(list)) {
                        result.addAll(list);
                    }
                }
            }
            pageTotal = result.size();
            filteredDef = result;
        } else if (Boolean.TRUE.equals(processQuery.getWorkOrderSynthesisAll())) {
            List<String> wfSynthesisList = workOrderSynthesisService.list().stream().map(item -> item.getSynthesisId())
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
                for (String synthesisId : wfSynthesisList) {
                    List<HistoricProcessInstance> list = historicProcessInstanceQuery
                            .processDefinitionCategory(synthesisId).list();
                    if (ObjectUtil.isNotEmpty(list)) {
                        result.addAll(list);
                    }
                }
            }
            pageTotal = result.size();
            filteredDef = result;
        } else {
            pageTotal = historicProcessInstanceQuery.count();
            if (pageTotal <= 0) {
                return TableDataInfo.build();
            }
            filteredDef = historicProcessInstanceQuery.list();
        }

        // Get workOrderMap, after
        Map<String, WorkOrderVo> workOrderMap = getWorkOrderMap(workOrderBo);

        // in HistoricProcessInstance and workOrderMap in data
        List<HistoricProcessInstance> matchedList = filteredDef.stream()
                .filter(hisIns -> workOrderMap.containsKey(hisIns.getId()))
                .collect(Collectors.toList());
        int listSize = matchedList.size();
        int offset = (int) (page.getSize() * (page.getCurrent() - 1));
        // if offset , null / empty
        if (offset >= listSize) {
            page.setTotal(listSize);
            page.setRecords(new ArrayList<>());
            return TableDataInfo.build(page);
        }
        int toIndex = (int) Math.min(offset + page.getSize(), listSize);
        List<HistoricProcessInstance> historicProcessInstances = matchedList.subList(offset, toIndex);
        List<WorkOrderVo> workOrderVoList = new ArrayList<>();
        for (HistoricProcessInstance hisIns : historicProcessInstances) {
            // map find work order
            WorkOrderVo workOrderVo = workOrderMap.get(hisIns.getId());
            // Get workflow
            HistoricVariableInstance processStatusVariable = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hisIns.getId()).variableName(PROCESS_STATUS_KEY).singleResult();
            String processStatus = null;
            if (ObjectUtil.isNotNull(processStatusVariable)) {
                processStatus = Convert.toStr(processStatusVariable.getValue());
            }
            // old workflow
            if (processStatus == null) {
                processStatus = ObjectUtil.isNull(hisIns.getEndTime()) ? ProcessStatus.RUNNING.getStatus()
                        : ProcessStatus.COMPLETED.getStatus();
            }
            workOrderVo.setProcessStatus(processStatus);
            workOrderVo.setCreateTime(hisIns.getStartTime());
            workOrderVo.setProcDefVersion(hisIns.getProcessDefinitionVersion());
            workOrderVo.setProcessKey(hisIns.getProcessDefinitionKey());
            workOrderVo.setCategoryName(categoryLookupService.queryCategoryName(workOrderVo.getWorkorderId(),
                    processQuery.getCategoryType()));
            // current workflow
            List<Task> taskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(hisIns.getId()).includeIdentityLinks().list();
            if (CollUtil.isNotEmpty(taskList)) {
                workOrderVo.setProcessStatus(
                        taskList.stream().map(Task::getName).filter(com.ruoyi.common.utils.StringUtils::isNotEmpty)
                                .distinct().collect(Collectors.joining(",")));
            }
            if (workOrderVo.getProcessStatus() == null) {
                workOrderVo.setProcessStatus("审批已完成");
            }
            workOrderVoList.add(workOrderVo);
        }
        page.setTotal(listSize);
        page.setRecords(workOrderVoList);
        return TableDataInfo.build(page);
    }

    // @Override
    // public TableDataInfo<WorkOrderVo> selectPageTodoWorkOrderList(ProcessQuery
    // processQuery, WorkOrderBo workOrderBo,
    // PageQuery pageQuery, SysUser sysUser) {
    // workOrderBo.setWorkOrderAppAll(processQuery.getWorkOrderAppAll());
    // workOrderBo.setWorkOrderSynthesisAll(processQuery.getWorkOrderSynthesisAll());
    // Page<WorkOrderVo> page = new Page<>();
    // TaskQuery taskQuery =
    // taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).active().includeProcessVariables()
    // .taskAssignee(String.valueOf(sysUser.getUserId())).orderByTaskCreateTime().desc();
    // // Build
    // ProcessUtils.buildProcessSearch(taskQuery, processQuery, processEngine,
    // sysUser);
    //
    // // workflow
    // if (com.ruoyi.common.utils.StringUtils.isNotBlank(processQuery.getState())) {
    // taskQuery.processVariableValueEquals(ProcessConstants.PROCESS_STATUS_KEY,
    // processQuery.getState());
    // }
    //
    // processCategoryForTaskQuery(processQuery, taskQuery);
    //
    // long pageTotal = taskQuery.count();
    // if (pageTotal <= 0) {
    // return TableDataInfo.build();
    // }
    //
    //
    // int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
    // List<Task> taskList = taskQuery.listPage(offset, pageQuery.getPageSize());
    // Map<String, WorkOrderVo> workOrderMap = getWorkOrderMap(workOrderBo);
    // List<WorkOrderVo> workOrderVoList = new ArrayList<>();
    // for (Task task : taskList) {
    // // map find work order
    // WorkOrderVo workOrders = workOrderMap.get(task.getProcessInstanceId());
    // if (workOrders == null) {
    // continue; //
    // }
    // WorkOrderVo workOrderVo = new WorkOrderVo();
    // // current workflowinfo
    // workOrderVo.setTaskId(task.getId());
    // workOrderVo.setWorkorderId(workOrders.getWorkorderId());
    // workOrderVo.setTitle(workOrders.getTitle());
    // workOrderVo.setWorkorderIdExtend(workOrders.getWorkorderIdExtend());
    // workOrderVo.setTaskName(task.getName());
    // workOrderVo.setId(workOrders.getId());
    // workOrderVo.setAssignId(workOrders.getAssignId());
    // workOrderVo.setCreateTime(task.getCreateTime());
    // // workflow definitioninfo
    // ProcessDefinition pd =
    // repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(sysUser.getTenantId())
    // .processDefinitionId(task.getProcessDefinitionId()).singleResult();
    // workOrderVo.setProcDefName(pd.getName());
    // workOrderVo.setProcDefVersion(pd.getVersion());
    // workOrderVo.setProcInstId(task.getProcessInstanceId());
    // workOrderVo.setCategoryName(categoryLookupService.queryCategoryName(workOrders.getWorkorderId(),
    // processQuery.getCategoryType()));
    //
    // workOrderVo.setProcessStatus(String.valueOf(task.getProcessVariables().get(ProcessConstants
    // .PROCESS_STATUS_KEY)));
    // workOrderVoList.add(workOrderVo);
    // }
    // page.setTotal(workOrderVoList.size());
    // page.setRecords(workOrderVoList);
    // return TableDataInfo.build(page);
    // }
    //
    // @Override
    // public TableDataInfo<WorkOrderVo>
    // selectPageFinishedWorkOrderList(ProcessQuery processQuery,
    // WorkOrderBo workOrderBo, PageQuery pageQuery,
    // SysUser sysUser) {
    // workOrderBo.setWorkOrderAppAll(processQuery.getWorkOrderAppAll());
    // workOrderBo.setWorkOrderSynthesisAll(processQuery.getWorkOrderSynthesisAll());
    // Page<WorkOrderVo> page = new Page<>();
    // HistoricTaskInstanceQuery taskInstanceQuery =
    // historyService.createHistoricTaskInstanceQuery().includeProcessVariables().finished().taskAssignee
    // (String.valueOf(sysUser.getUserId()))
    //// .taskAssignee(TaskUtils.getUserId())
    // .orderByHistoricTaskInstanceEndTime().desc();
    // // Build
    // ProcessUtils.buildProcessSearch(taskInstanceQuery, processQuery,
    // processEngine, sysUser);
    // if (Boolean.TRUE.equals(processQuery.getWorkOrderAppAll())) {
    // List<String> appList = workOrderAppService.list()
    // .stream()
    // .map(WorkOrderApp::getAppId)
    // .collect(Collectors.toList());
    // if (ObjectUtil.isNotEmpty(appList)) {
    // taskInstanceQuery.processCategoryIn(appList);
    // }
    // }
    // // if processQuery Get full
    // else if (Boolean.TRUE.equals(processQuery.getWorkOrderSynthesisAll())) {
    // List<String> wfSynthesisList = workOrderSynthesisService.list()
    // .stream()
    // .map(WorkOrderSynthesis::getSynthesisId)
    // .collect(Collectors.toList());
    // if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
    // taskInstanceQuery.processCategoryIn(wfSynthesisList);
    // }
    // }
    //
    // int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
    // List<HistoricTaskInstance> historicTaskInstanceList =
    // taskInstanceQuery.listPage(offset,
    // pageQuery.getPageSize());
    // Map<String, WorkOrderVo> workOrderMap = getWorkOrderMap(workOrderBo);
    // List<WorkOrderVo> workOrderVoList = new ArrayList<>();
    // for (HistoricTaskInstance histTask : historicTaskInstanceList) {
    // WorkOrderVo workOrders = workOrderMap.get(histTask.getProcessInstanceId());
    // if (workOrders == null) {
    // continue; //
    // }
    // WorkOrderVo workOrderVo = new WorkOrderVo();
    // // current workflowinfo
    // workOrderVo.setTaskId(histTask.getId());
    // workOrderVo.setWorkorderId(workOrders.getWorkorderId());
    // workOrderVo.setTitle(workOrders.getTitle());
    // workOrderVo.setWorkorderIdExtend(workOrders.getWorkorderIdExtend());
    // workOrderVo.setTaskName(histTask.getName());
    // workOrderVo.setId(workOrders.getId());
    // workOrderVo.setAssignId(workOrders.getAssignId());
    // workOrderVo.setCreateTime(histTask.getCreateTime());
    // // workflow definitioninfo
    // ProcessDefinition pd =
    // repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(sysUser.getTenantId())
    // .processDefinitionId(histTask.getProcessDefinitionId()).singleResult();
    // workOrderVo.setProcDefName(pd.getName());
    // workOrderVo.setProcDefVersion(pd.getVersion());
    // workOrderVo.setProcInstId(histTask.getProcessInstanceId());
    // workOrderVo.setCategoryName(categoryLookupService.queryCategoryName(workOrders.getWorkorderId(),
    // processQuery.getCategoryType()));
    // // workflow variable
    // workOrderVo.setProcessStatus(String.valueOf(histTask.getProcessVariables().get(ProcessConstants
    // .PROCESS_STATUS_KEY)));
    // HistoricProcessInstance historicProcessInstance =
    // historyService.createHistoricProcessInstanceQuery().processInstanceTenantId(sysUser.getTenantId())
    // .processInstanceId(histTask.getProcessInstanceId()).singleResult();
    // String userId = historicProcessInstance.getStartUserId();
    // String userName = userService.selectUserNameById(userId);
    // workOrderVo.setStartUserId(userId);
    // workOrderVo.setStartUserName(userName);
    // workOrderVoList.add(workOrderVo);
    // }
    // page.setTotal(workOrderVoList.size());
    // page.setRecords(workOrderVoList);
    // return TableDataInfo.build(page);
    // }

    /**
     * : Task / HistoricTaskInstance fieldfill VO
     *
     * @param baseMap getWorkOrderMap(workOrderBo) full Map, method
     *                     getWorkOrderMap
     * @param processQuery Query
     * @param sysUser Query workflow definition
     * @param voKey workflow instance ID, from baseMap WorkOrderVo
     * @param taskId task ID
     * @param taskName task
     * @param startTime taskstart (createTime)
     * @param procDefId workflow definition ID
     * @param variables workflow variable Map ( PROCESS_STATUS_KEY)
     * @return field WorkOrderVo; if baseMap in key, null
     */
    private WorkOrderVo fillCommonFields(
            Map<String, WorkOrderVo> baseMap,
            ProcessQuery processQuery,
            SysUser sysUser,
            String voKey,
            String taskId,
            String taskName,
            Date startTime,
            String procDefId,
            Map<String, Object> variables) {
        WorkOrderVo w = baseMap.get(voKey);
        if (w == null) {
            return null;
        }
        // fill field
        w.setTaskId(taskId);
        w.setTaskName(taskName);
        w.setCreateTime(startTime);
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId())
                .processDefinitionId(procDefId)
                .singleResult();
        w.setProcDefName(pd.getName());
        w.setProcDefVersion(pd.getVersion());
        w.setProcInsId(voKey);
        w.setCategoryName(categoryLookupService.queryCategoryName(w.getWorkorderId(),
                processQuery.getCategoryType()));
        w.setProcessStatus(String.valueOf(variables.get(PROCESS_STATUS_KEY)));
        return w;
    }

    /**
     * Query
     *
     * @param <Q> is Flowable Query
     * @param <T> Query (Task HistoricTaskInstance)
     */
    private <Q extends Query<?, ?>, T> TableDataInfo<WorkOrderVo> selectPageCommon(
            ProcessQuery processQuery,
            WorkOrderBo workOrderBo,
            PageQuery pageQuery,
            SysUser sysUser,
            Function<SysUser, Q> querySupplier,
            BiConsumer<Q, ProcessQuery> customConditionConfigurer,
            ToLongFunction<Q> countFunc,
            BiFunction<Q, Integer, List<T>> listPageFunc,
            Function<T, WorkOrderVo> converter) {
        // 1. Bo
        workOrderBo.setWorkOrderAppAll(processQuery.getWorkOrderAppAll());
        workOrderBo.setWorkOrderSynthesisAll(processQuery.getWorkOrderSynthesisAll());

        // 2. Query
        Q query = querySupplier.apply(sysUser);
        // 3. ProcessUtils, in Q is Query<?,?> sub
        ProcessUtils.buildProcessSearch(query, processQuery, processEngine, sysUser);
        // 4.
        customConditionConfigurer.accept(query, processQuery);

        // 5. 、 、Convert
        long total = countFunc.applyAsLong(query);
        if (total <= 0) {
            return TableDataInfo.build();
        }
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<T> rawList = listPageFunc.apply(query, offset);

        List<WorkOrderVo> voList = rawList.stream()
                .map(converter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Page<WorkOrderVo> page = new Page<>();
        page.setTotal(total);
        page.setRecords(voList);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<WorkOrderVo> selectPageTodoWorkOrderList(
            ProcessQuery processQuery, WorkOrderBo workOrderBo,
            PageQuery pageQuery, SysUser sysUser) {
        Map<String, WorkOrderVo> baseMap = getWorkOrderMap(workOrderBo);
        Set<String> processInstances = baseMap.keySet();

        // if workflow instance, null / empty
        if (CollUtil.isEmpty(processInstances)) {
            return TableDataInfo.build();
        }

        return selectPageCommon(
                processQuery, workOrderBo, pageQuery, sysUser,
                // : TaskQuery
                user -> taskService.createTaskQuery()
                        .taskTenantId(user.getTenantId())
                        .active()
                        .processInstanceIdIn(processInstances)
                        .includeProcessVariables()
                        .taskAssignee(user.getUserId())
                        .orderByTaskCreateTime().desc(),
                // : TaskQuery
                (query, pq) -> {
                    TaskQuery tq = (TaskQuery) query;
                    // workflow
                    if (StringUtils.isNotBlank(pq.getState())) {
                        tq.processVariableValueEquals(PROCESS_STATUS_KEY, pq.getState());
                    }
                    //
                    processCategoryForTaskQuery(pq, tq);
                },
                // : count and listPage
                q -> ((TaskQuery) q).count(),
                (q, offset) -> ((TaskQuery) q).listPage(offset, pageQuery.getPageSize()),
                // : Task -> WorkOrderVo
                task -> {
                    // only 「 fill 」
                    return fillCommonFields(
                            baseMap,
                            processQuery,
                            sysUser,
                            task.getProcessInstanceId(),
                            task.getId(),
                            task.getName(),
                            task.getCreateTime(),
                            task.getProcessDefinitionId(),
                            task.getProcessVariables());
                });
    }

    // @Override
    // public TableDataInfo<WorkOrderVo> selectPageFinishedWorkOrderList(
    // ProcessQuery processQuery, WorkOrderBo workOrderBo,
    // PageQuery pageQuery, SysUser sysUser) {
    //
    // Map<String, WorkOrderVo> baseMap = getWorkOrderMap(workOrderBo);
    // Set<String> processInstances = baseMap.keySet();
    // return selectPageCommon(
    // processQuery, workOrderBo, pageQuery, sysUser,
    // // : already HistoricTaskInstanceQuery
    // user -> historyService.createHistoricTaskInstanceQuery()
    // .includeProcessVariables()
    // .finished()
    // .processInstanceIdIn(processInstances)
    // .taskAssignee(String.valueOf(user.getUserId()))
    // .orderByHistoricTaskInstanceEndTime().desc(),
    // // : HistoricTask
    // (query, pq) -> {
    // HistoricTaskInstanceQuery hq = (HistoricTaskInstanceQuery) query;
    // // full
    // if (Boolean.TRUE.equals(pq.getWorkOrderAppAll())) {
    // List<String> appList = workOrderAppService.list()
    // .stream().map(WorkOrderApp::getAppId).collect
    // (Collectors.toList());
    // if (!appList.isEmpty()) hq.processCategoryIn(appList);
    // }
    // // full
    // else if (Boolean.TRUE.equals(pq.getWorkOrderSynthesisAll())) {
    // List<String> synList = workOrderSynthesisService.list()
    // .stream().map
    // (WorkOrderSynthesis::getSynthesisId).collect
    // (Collectors.toList());
    // if (!synList.isEmpty()) hq.processCategoryIn(synList);
    // }
    // },
    // // : count and listPage
    // q -> ((HistoricTaskInstanceQuery) q).count(),
    // (q, offset) -> ((HistoricTaskInstanceQuery) q)
    // .listPage(offset, pageQuery.getPageSize()),
    // // : HistoricTaskInstance -> WorkOrderVo
    // hist -> {
    // WorkOrderVo vo = fillCommonFields(
    // baseMap,
    // processQuery,
    // sysUser,
    // hist.getProcessInstanceId(),
    // hist.getId(),
    // hist.getName(),
    // hist.getCreateTime(),
    // hist.getProcessDefinitionId(),
    // hist.getProcessVariables()
    // );
    // if (vo == null) {
    // return null;
    // }
    // // already : info
    // HistoricProcessInstance pi =
    // historyService.createHistoricProcessInstanceQuery()
    // .processInstanceTenantId(sysUser.getTenantId())
    // .processInstanceId(hist.getProcessInstanceId())
    // .singleResult();
    // vo.setStartUserId(pi.getStartUserId());
    // vo.setStartUserName(userService.selectUserNameById(pi.getStartUserId()));
    // return vo;
    // }
    // );
    //
    //
    // }

    @Override
    public TableDataInfo<WorkOrderVo> selectPageFinishedWorkOrderList(
            ProcessQuery processQuery, WorkOrderBo workOrderBo,
            PageQuery pageQuery, SysUser sysUser) {

        // 1. Build work order map, after fill
        Map<String, WorkOrderVo> baseMap = getWorkOrderMap(workOrderBo);
        Set<String> processInstances = baseMap.keySet();

        // if workflow instance, null / empty ( processInstanceIdIn null / empty collection )
        if (processInstances == null || processInstances.isEmpty()) {
            Page<WorkOrderVo> emptyPage = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return TableDataInfo.build(emptyPage);
        }

        // 2. HistoricTaskInstanceQuery ( and selectPageCommon in Query )
        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables() // Query workflow variable, after
                // fillCommonFields need to
                .finished() // only Query already ( already )
                .processInstanceIdIn(processInstances) // in workflow instancecollection in
                .taskAssignee(String.valueOf(sysUser.getUserId()))
                .orderByHistoricTaskInstanceEndTime().desc(); //
        // ( main need to is to API )

        // 3. Query ( and lambda in )
        if (Boolean.TRUE.equals(processQuery.getWorkOrderAppAll())) {
            List<String> appList = workOrderAppService.list().stream()
                    .map(WorkOrderApp::getAppId)
                    .collect(Collectors.toList());
            if (!appList.isEmpty()) {
                taskQuery.processCategoryIn(appList);
            }
        } else if (Boolean.TRUE.equals(processQuery.getWorkOrderSynthesisAll())) {
            List<String> synList = workOrderSynthesisService.list().stream()
                    .map(WorkOrderSynthesis::getSynthesisId)
                    .collect(Collectors.toList());
            if (!synList.isEmpty()) {
                taskQuery.processCategoryIn(synList);
            }
        }

        // 4. all history task ( to )
        List<HistoricTaskInstance> allHistoricTasks = taskQuery.list();

        // 5. processInstanceId : only createTime new
        Map<String, HistoricTaskInstance> latestByProcIns = new HashMap<>(allHistoricTasks.size());
        for (HistoricTaskInstance hti : allHistoricTasks) {
            String procInsId = hti.getProcessInstanceId();
            if (procInsId == null) {
                // workflow instance id record ( Update )
                continue;
            }
            HistoricTaskInstance existing = latestByProcIns.get(procInsId);
            if (existing == null) {
                latestByProcIns.put(procInsId, hti);
            } else {
                Date existCreate = existing.getCreateTime();
                Date curCreate = hti.getCreateTime();
                long existMillis = (existCreate == null) ? Long.MIN_VALUE : existCreate.getTime();
                long curMillis = (curCreate == null) ? Long.MIN_VALUE : curCreate.getTime();
                // if current record createTime , Replace
                if (curMillis > existMillis) {
                    latestByProcIns.put(procInsId, hti);
                }
            }
        }

        // 6. to List createTime ( in before)
        List<HistoricTaskInstance> dedupList = new ArrayList<>(latestByProcIns.values());
        dedupList.sort((a, b) -> {
            Date da = a.getCreateTime();
            Date db = b.getCreateTime();
            long ta = (da == null) ? Long.MIN_VALUE : da.getTime();
            long tb = (db == null) ? Long.MIN_VALUE : db.getTime();
            return Long.compare(tb, ta); // : in before
        });

        // 7. ( after collection)
        int pageSize = pageQuery.getPageSize();
        int pageNum = pageQuery.getPageNum();
        int offset = pageSize * (pageNum - 1);
        int total = dedupList.size();
        List<HistoricTaskInstance> pageHistoricTasks;
        if (offset >= total) {
            pageHistoricTasks = Collections.emptyList();
        } else {
            int toIndex = Math.min(offset + pageSize, total);
            pageHistoricTasks = dedupList.subList(offset, toIndex);
        }

        // 8. after HistoricTaskInstance WorkOrderVo ( fillCommonFields)
        List<WorkOrderVo> voList = new ArrayList<>();
        for (HistoricTaskInstance hist : pageHistoricTasks) {
            WorkOrderVo vo = fillCommonFields(
                    baseMap,
                    processQuery,
                    sysUser,
                    hist.getProcessInstanceId(), // workflow instance id
                    hist.getId(), // historic task id
                    hist.getName(), // task
                    hist.getCreateTime(), // createTime, /
                    hist.getProcessDefinitionId(), // workflow definition id
                    hist.getProcessVariables() // workflow variable
            );
            if (vo == null) {
                continue; // fillCommonFields can in baseMap null
            }

            // already field: info (from history workflow instance in )
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId())
                    .processInstanceId(hist.getProcessInstanceId())
                    .singleResult();
            if (pi != null) {
                vo.setStartUserId(pi.getStartUserId());
                vo.setStartUserName(userService.selectUserNameById(pi.getStartUserId()));
            }
            voList.add(vo);
        }

        // 9. object
        Page<WorkOrderVo> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.setRecords(voList);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<WorkOrderVo> selectPageClaimWorkOrderList(ProcessQuery processQuery, WorkOrderBo workOrderBo,
            PageQuery pageQuery, SysUser sysUser) {
        Page<WorkOrderVo> page = new Page<>();
        TaskQuery taskQuery = null;
        Map<String, WorkOrderVo> workOrderMap = getWorkOrderMap(workOrderBo);
        Set<String> processInstances = workOrderMap.keySet();
        if (CollUtil.isEmpty(processInstances)) {
            return TableDataInfo.build(Collections.emptyList());
        }

        try {
            taskQuery = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).active()
                    .includeProcessVariables().processInstanceIdIn(processInstances)
                    .taskCandidateUser(sysUser.getUserId())
                    .taskCandidateGroupIn(TaskUtils.getCandidateGroup(sysUser)).orderByTaskCreateTime().desc();

        } catch (Exception e) {
            throw new RuntimeException("当前用户无所属组织机构");
        }
        // Build
        ProcessUtils.buildProcessSearch(taskQuery, processQuery, processEngine, sysUser);

        processCategoryForTaskQuery(processQuery, taskQuery);

        long pageTotal = taskQuery.count();
        if (pageTotal <= 0) {
            return TableDataInfo.build();
        }

        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<Task> taskList = taskQuery.listPage(offset, pageQuery.getPageSize());
        List<WorkOrderVo> flowList = new ArrayList<>();
        for (Task task : taskList) {
            WorkOrderVo workOrders = workOrderMap.get(task.getProcessInstanceId());
            if (workOrders == null) {
                continue; //
            }
            // current workflowinfo
            workOrders.setTaskId(task.getId());
            workOrders.setCreateTime(task.getCreateTime());
            workOrders.setTaskName(task.getName());
            // workflow definitioninfo
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            workOrders.setProcDefName(pd.getName());
            workOrders.setProcDefVersion(pd.getVersion());
            workOrders.setCategoryName(categoryLookupService.queryCategoryName(workOrders.getWorkorderId(),
                    processQuery.getCategoryType()));
            // workflow info
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String userId = historicProcessInstance.getStartUserId();
            String userName = userService.selectUserNameById(userId);
            workOrders.setStartUserId(userId);
            workOrders.setStartUserName(userName);
            workOrders.setId(workOrders.getId());
            workOrders.setTitle(workOrders.getTitle());
            flowList.add(workOrders);
        }
        page.setTotal(flowList.size());
        page.setRecords(flowList);
        return TableDataInfo.build(page);
    }

    private Map<String, WorkOrderVo> getWorkOrderMap(WorkOrderBo workOrderBo) {
        List<WorkOrderVo> workOrderList = queryList(workOrderBo);
        return workOrderList.stream()
                .filter(wo -> wo.getProcInsId() != null) // null key
                .collect(Collectors.toMap(
                        WorkOrderVo::getProcInsId,
                        Function.identity(),
                        (existing, replacement) -> existing // ,
                ));
    }

    /**
     * processQuery Check need to from service in Get , after taskQuery Process .
     * @param processQuery Query parameter
     * @param taskQuery need to Set Query object
     */
    public void processCategoryForTaskQuery(ProcessQuery processQuery, TaskQuery taskQuery) {
        // if processQuery Get full APP
        if (Boolean.TRUE.equals(processQuery.getWorkOrderAppAll())) {
            List<String> appList = workOrderAppService.list()
                    .stream()
                    .map(WorkOrderApp::getAppId)
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                taskQuery.processCategoryIn(appList);
            }
        }
        // if processQuery Get full
        else if (Boolean.TRUE.equals(processQuery.getWorkOrderSynthesisAll())) {
            List<String> wfSynthesisList = workOrderSynthesisService.list()
                    .stream()
                    .map(WorkOrderSynthesis::getSynthesisId)
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
                taskQuery.processCategoryIn(wfSynthesisList);
            }
        }
    }

    /**
     * Query work order list
     */
    @Override
    public List<WorkOrderVo> queryList(WorkOrderBo bo) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, false);
        return workOrderMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WorkOrder> buildQueryWrapper(WorkOrderBo bo, Boolean jobFlag) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WorkOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getSystemId()), WorkOrder::getSystemId, bo.getSystemId());
        lqw.like(StringUtils.isNotBlank(bo.getProjectId()), WorkOrder::getProjectId, bo.getProjectId());
        lqw.eq(StringUtils.isNotBlank(bo.getWorkorderId()), WorkOrder::getWorkorderId, bo.getWorkorderId());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessKey()), WorkOrder::getProcessKey, bo.getProcessKey());
        lqw.eq(StringUtils.isNotBlank(bo.getWorkorderNumber()), WorkOrder::getWorkorderNumber, bo.getWorkorderNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getTitle()), WorkOrder::getTitle, bo.getTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), WorkOrder::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getWorkorderStatus()), WorkOrder::getWorkorderStatus, bo.getWorkorderStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getPriority()), WorkOrder::getPriority, bo.getPriority());
        lqw.eq(StringUtils.isNotBlank(bo.getProcessStatus()), WorkOrder::getProcessStatus, bo.getProcessStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getSource()), WorkOrder::getSource, bo.getSource());
        lqw.eq(StringUtils.isNotBlank(bo.getCompensation()), WorkOrder::getCompensation, bo.getCompensation());
        lqw.eq(StringUtils.isNotBlank(bo.getEvaluate()), WorkOrder::getEvaluate, bo.getEvaluate());
        lqw.eq(StringUtils.isNotBlank(bo.getRoomNumber()), WorkOrder::getRoomNumber, bo.getRoomNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getAttachmentUrls()), WorkOrder::getAttachmentUrls, bo.getAttachmentUrls());
        lqw.ge(bo.getCreateTime() != null, WorkOrder::getCreateTime, bo.getCreateTime());
        lqw.le(bo.getUpdateTime() != null, WorkOrder::getUpdateTime, bo.getUpdateTime());
        // if (Boolean.TRUE.equals(jobFlag) &&
        // !Boolean.TRUE.equals(bo.getWorkOrderAppAll())&& !Boolean.TRUE.equals(bo
        // .getWorkOrderSynthesisAll())) {
        // lqw.orderByDesc(WorkOrder::getWorkOrderJobSerial);
        // lqw.orderByAsc(WorkOrder::getWorkOrderJobFlag);
        // lqw.isNotNull(WorkOrder::getWorkOrderJobSerial);
        // } else if
        // (!Boolean.TRUE.equals(bo.getWorkOrderAppAll())&&!Boolean.TRUE.equals(bo.getWorkOrderSynthesisAll
        // ())) {
        // lqw.isNull(WorkOrder::getWorkOrderJobSerial);
        // }
        if (Boolean.TRUE.equals(bo.getWorkOrderAppAll())) {
            List<String> workOrderAppList = workOrderAppService.list().stream().map(WorkOrderApp::getAppId)
                    .collect(Collectors.toList());
            lqw.in(ObjectUtil.isNotEmpty(workOrderAppList), WorkOrder::getWorkorderId, workOrderAppList);
        }
        if (Boolean.TRUE.equals(bo.getWorkOrderSynthesisAll())) {
            List<String> workOrderSynthesisAll = workOrderSynthesisService.list().stream()
                    .map(WorkOrderSynthesis::getSynthesisId).collect(Collectors.toList());
            lqw.in(ObjectUtil.isNotEmpty(workOrderSynthesisAll), WorkOrder::getWorkorderId, workOrderSynthesisAll);
        }
        lqw.orderByDesc(WorkOrder::getUpdateTime, WorkOrder::getUpdateTime);
        return lqw;
    }

    /**
     * Add work order
     */
    @Override
    public WorkOrder insertByBo(WorkOrderBo bo, SysUser sysUser) {
        WorkOrder add = BeanUtil.toBean(bo, WorkOrder.class);
        add.setCreateBy(sysUser.getUserName());
        add.setUserId(sysUser.getUserId());
        add.setTenantId(sysUser.getTenantId());
        add.setProcessStatus(ProcessStatus.RUNNING.getStatus());
        add.setPriority(WorkOrderUrgency.NORMAL.getStatus());
        if (StringUtils.isNotBlank(bo.getWorkorderStatus())) {
            add.setWorkorderStatus(bo.getWorkorderStatus());
        } else {
            add.setWorkorderStatus(WorkOrderStatus.PENDING_DISPATCH.getStatus());

        }
        long nanoTime = System.nanoTime(); // Get current
        int random = new Random().nextInt(9999); // ,
        String id = Long.toHexString(nanoTime) + Integer.toHexString(random);
        add.setWorkorderNumber(id.substring(0, Math.min(10, id.length())));
        validEntityBeforeSave(add);
        boolean flag = workOrderMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return add;
    }

    /**
     * Update work order
     */
    @Override
    public Boolean updateByBo(WorkOrderBo bo) {
        WorkOrder update = BeanUtil.toBean(bo, WorkOrder.class);
        validEntityBeforeSave(update);
        return workOrderMapper.updateById(update) > 0;
    }

    /**
     * before dataValidate
     */
    private void validEntityBeforeSave(WorkOrder entity) {
        // TODO dataValidate ,
    }

    /**
     * Batch delete work order
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO Validate ,Check whether need to Validate
        }
        return workOrderMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * work orderfinish new
     *
     * @param processInstanceId
     * @return whether new successfully
     * @throws IllegalArgumentException parameterValidate failed
     */
    public boolean updateWorkOrderToPending(String processInstanceId, String status, String assignId) {
        // Query work order
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrder::getProcInsId, processInstanceId);
        WorkOrder existingOrder = getOne(queryWrapper);
        if (ObjectUtil.isNull(existingOrder)) {
            log.warn("未找到对应的工单");
            return false;
        }

        // LambdaUpdateWrapper Set null (updateById null value )
        LambdaUpdateWrapper<WorkOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WorkOrder::getId, existingOrder.getId())
                .set(WorkOrder::getWorkorderStatus, status)
                .set(WorkOrder::getTaskId, null); // Set to null

        // Execute new
        return update(updateWrapper);
    }

    /**
     * new work order
     *
     * @param task task ( non- null / empty to task)
     * @return whether new successfully
     * @throws IllegalArgumentException parameterValidate failed
     *
     */
    public boolean updateWorkOrderToPending(Task task, String status, String assignId) {
        // Query work order
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrder::getProcInsId, task.getProcessInstanceId());
        WorkOrder existingOrder = getOne(queryWrapper);
        if (ObjectUtil.isNull(existingOrder)) {
            log.warn("未找到对应的工单");
            return false;
        }
        WorkOrderBo workOrderBo = new WorkOrderBo();
        // Set
        if (StringUtils.isNotBlank(assignId)) {
            workOrderBo.setAssignId(assignId);
        }
        // new work order and ID
        workOrderBo.setWorkorderStatus(status);
        if (ObjectUtil.isNotNull(task)) {
            workOrderBo.setTaskId(task.getId());
        }
        workOrderBo.setId(existingOrder.getId());

        // Execute new
        return updateByBo(workOrderBo);
    }

    /**
     * Query work order list
     */
    @Override
    public TableDataInfo<WorkOrderVo> queryImmediatePageList(WorkOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, false);
        Page<WorkOrderVo> result = workOrderMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * Query work order list
     */
    @Override
    public List<WorkOrderVo> queryImmediateList(WorkOrderBo bo) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, false);
        return workOrderMapper.selectVoList(lqw);
    }

    @Override
    public TableDataInfo<Object> queryLoopPageList(WorkOrderBo bo, PageQuery pageQuery, SysUser sysUser) {
        Page<WorkOrderVo> page = pageQuery.build();
        // PageQuery build() method Build object
        List<String> fieldCodes = JSON.parseArray(wfUserInterfaceFieldService.getFieldCodes(sysUser.getUserId(),
                bo.getApiPath()), String.class);
        int offset = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        Map<String, WorkOrderVo> workOrderMap = getWorkOrderMap(bo);
        if (ObjectUtils.isEmpty(workOrderMap)) {
            return TableDataInfo.build();
        }
        Map<String, SysUser> sysUserMap = sysUserServiceImpl.list().stream().filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(
                        SysUser::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing // ,
                ));
        Optional<Set<String>> processInstances = Optional.ofNullable(workOrderMap.keySet());
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .processInstanceIds(processInstances.orElseGet(HashSet::new))
                .orderByProcessInstanceStartTime().desc();
        long count = historicProcessInstanceQuery.count();
        if (count == 0) {
            return TableDataInfo.build();
        }
        List<HistoricProcessInstance> historicProcessInstanceList = historicProcessInstanceQuery.listPage(offset,
                pageQuery.getPageSize());
        List<String> historicProcessInstanceIdList = historicProcessInstanceList.stream().distinct()
                .map(historicProcessInstance -> historicProcessInstance.getId()).collect(Collectors.toList());
        List<Task> taskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceIdIn(historicProcessInstanceIdList).includeIdentityLinks().list();
        List<WorkOrderVo> workOrderVoList = new ArrayList<>();
        for (HistoricProcessInstance historicProcessInstance : historicProcessInstanceList) {
            WorkOrderVo workOrders = workOrderMap.get(historicProcessInstance.getId());

            if (CollUtil.isNotEmpty(taskList)) {
                workOrders.setCurrentActivityName(
                        taskList.stream().filter(task -> task.getProcessInstanceId().equals(workOrders.getProcInsId()))
                                .map(Task::getName).filter(com.ruoyi.common.utils.StringUtils::isNotEmpty).distinct()
                                .collect(Collectors.joining(",")));
            }
            if (workOrders.getCurrentActivityName() == null || historicProcessInstance.getEndTime() != null) {
                workOrders.setCurrentActivityName("已完成");
            }

            SysUser sysUser1 = sysUserMap.get(workOrders.getUserId());
            if (ObjectUtil.isNotNull(sysUser1)) {
                workOrders.setCurrentAssignName(sysUser1.getUserName());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                if (ObjectUtil.isNotNull(sysUser1)) {
                    JsonNode rootNode = objectMapper.readTree(sysUser1.getDeptInfo());
                    List<String> deptNames = new ArrayList<>();
                    if (rootNode.isArray()) {
                        for (JsonNode node : rootNode) {
                            String deptName = node.get("dept_name").asText();
                            deptNames.add(deptName);
                        }
                        // department name
                        workOrders.setDeptName(String.join(",", deptNames));
                    }
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("部门信息解析失败", e);
            }

            if (historicProcessInstance.getEndTime() != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                workOrders.setEndTime(simpleDateFormat.format(historicProcessInstance.getEndTime()));
                workOrders.setProcessingTime(DateUtils.getDatePoor(historicProcessInstance.getEndTime(),
                        historicProcessInstance.getStartTime()));
            } else {
                workOrders.setProcessingTime(DateUtils.getDatePoor(new Date(),
                        historicProcessInstance.getStartTime()));
            }
            workOrderVoList.add(workOrders);
        }
        // userconfiguration field
        List<Object> result;
        if (fieldCodes == null || fieldCodes.isEmpty()) {
            // not configurationfield, WfTaskVo object
            result = new ArrayList<>(workOrderVoList);
        } else {
            // configuration field field
            result = workOrderVoList.stream().map(vo -> {
                // WfTaskVo Convert to Map
                Map<String, Object> fieldAll = BeanUtil.beanToMap(vo, false, true);

                // only configuration field
                Map<String, Object> filteredFields = new LinkedHashMap<>();
                for (String code : fieldCodes) {
                    if (fieldAll.containsKey(code)) {
                        filteredFields.put(code, fieldAll.get(code));
                    }
                }
                return filteredFields;
            }).collect(Collectors.toList());
        }
        // Build
        Page<Object> resultPage = new Page<>();
        resultPage.setCurrent(page.getCurrent());
        resultPage.setSize(page.getSize());
        resultPage.setTotal(count);
        resultPage.setRecords(result);
        return TableDataInfo.build(resultPage);
    }

    @Override
    public List<WorkOrderVo> queryLoopList(WorkOrderBo bo) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, true);
        return workOrderMapper.selectVoList(lqw);
    }

    /**
     * Generate work order PDF
     *
     * @param wordOrderIds work orderID
     * @return PDF
     * @throws IOException IO
     */
    public ByteArrayOutputStream generatePdf(List<String> wordOrderIds, SysUser sysUser) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Initialize in ( )
        PdfFont font = PdfFontFactory.createFont(
                "STSongStd-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont boldFont = PdfFontFactory.createFont(
                "STSongStd-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        DeviceRgb headerColor = new DeviceRgb(220, 220, 220); //

        // Query work orderdata (MyBatis PlusQuery )
        LambdaQueryWrapper<WorkOrder> lqw = new LambdaQueryWrapper<>();
        lqw.in(WorkOrder::getId, wordOrderIds);
        List<WorkOrder> workOrderList = workOrderMapper.selectList(lqw);

        // each work orderGenerate PDF
        for (WorkOrder workOrder : workOrderList) {
            // work order
            Div workOrderDiv = new Div();
            workOrderDiv.setWidth(UnitValue.createPercentValue(100));

            // 1.
            Paragraph title = new Paragraph(workOrder.getTitle())
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);

            // 2. table (work order + )
            Table headerTable = createHeaderTable(font, workOrder.getWorkorderNumber());

            // 3. main table
            Table mainTable = createMainTable(font, boldFont, headerColor, workOrder);

            // element work order
            workOrderDiv.add(title).add(headerTable).add(mainTable);

            // work order
            LayoutResult layoutResult = workOrderDiv
                    .createRendererSubTree()
                    .setParent(document.getRenderer())
                    .layout(new LayoutContext(new LayoutArea(0, PageSize.A4)));
            float requiredHeight = layoutResult.getOccupiedArea().getBBox().getHeight();

            // Get current page null / empty ( full Get )
            int totalPages = pdfDoc.getNumberOfPages();
            Rectangle currentPageSize = totalPages > 0 ? pdfDoc.getPage(totalPages).getPageSize() : PageSize.A4; //
            // A4
            float currentY = totalPages > 0
                    ? document.getRenderer().getCurrentArea().getBBox().getY()
                    : PageSize.A4.getTop(); // to page

            float remainingSpace = currentY - (currentPageSize.getBottom() + document.getBottomMargin() + 20);

            // null / empty ( full Check )
            if (remainingSpace < requiredHeight && totalPages > 0) {
                document.add(new AreaBreak());
            }
            // work order
            document.add(workOrderDiv);
            // workflow log
            ProcessViewLogBo processViewLog = new ProcessViewLogBo();
            processViewLog.setProcessInstanceId(workOrder.getProcInsId());
            processViewLog.setProcessKey(workOrder.getProcessKey());
            processViewLog.setOperationType("打印");
            processViewLog.setProcessStatus((String) runtimeService.getVariable(workOrder.getProcInsId(),
                    ProcessConstants.PROCESS_STATUS_KEY));
            processViewLog.setViewTime(new Date());
            processViewLogService.insertByBo(processViewLog, sysUser);
        }

        document.close();
        return baos;
    }

    /**
     * infotable (work order + )
     *
     * @param font
     * @param workorderNumber work order
     * @return tableobject
     */
    private Table createHeaderTable(PdfFont font, String workorderNumber) {
        Table headerTable = new Table(new float[] { 1, 3 }) // 1:3
                .useAllAvailableWidth()
                .setMarginBottom(10);

        // work order ( )
        Paragraph orderPara = new Paragraph("工单编号：" + workorderNumber);
        orderPara.setFont(font);
        Cell orderCell = new Cell()
                .add(orderPara)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(5)
                .setBorder(Border.NO_BORDER);

        // ( )
        Paragraph timePara = new Paragraph("打印时间：" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        timePara.setFont(font);
        Cell timeCell = new Cell()
                .add(timePara)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(5)
                .setBorder(Border.NO_BORDER);

        headerTable.addCell(orderCell);
        headerTable.addCell(timeCell);
        return headerTable;
    }

    /**
     * main table
     *
     * @param font
     * @param boldFont
     * @param headerColor
     * @param workOrder work orderdata
     * @return tableobject
     */
    private Table createMainTable(
            PdfFont font, PdfFont boldFont, DeviceRgb headerColor, WorkOrder workOrder) {
        float[] columnWidths = { 150f, 150f, 150f, 150f }; // etc.
        Table table = new Table(columnWidths).useAllAvailableWidth();

        // data
        addBasicInfo(table, boldFont, font, headerColor, workOrder);
        addEventDescription(table, boldFont, font, headerColor,
                Optional.ofNullable(workOrder.getDescription()).orElse("-"));
        addAttachments(table, boldFont, font, headerColor, workOrder.getAttachmentUrls());
        addFormInfo(table, boldFont, font, headerColor, workOrder);
        addApprovalRecords(table, font, headerColor, workOrder);

        return table;
    }

    /**
     * info ( / item /work order etc.)
     *
     * @param table main table
     * @param boldFont
     * @param font
     * @param headerColor
     * @param workOrder work orderdata
     */
    private String resolveLocalProjectName(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return "-";
        }
        try {
            SysDeptView dept = sysDeptService.selectDeptById(projectId);
            if (dept == null || dept.getDeptName() == null || dept.getDeptName().trim().isEmpty()) {
                return "-";
            }
            return dept.getDeptName();
        } catch (Exception e) {
            log.warn("本地部门信息获取失败, projectId={}", projectId, e);
            return "-";
        }
    }

    private void addBasicInfo(
            Table table, PdfFont boldFont, PdfFont font, DeviceRgb headerColor, WorkOrder workOrder) {
        //
        table.addCell(createHeaderCell("所属系统", boldFont, headerColor));
        table.addCell(createValueCell(Optional.ofNullable(workOrder.getSystemId()).orElse("-"), font));

        // item
        table.addCell(createHeaderCell("项目名称", boldFont, headerColor));
        table.addCell(createValueCell(resolveLocalProjectName(workOrder.getProjectId()), font));

        // work order ( need to API)
        table.addCell(createHeaderCell("工单类型", boldFont, headerColor));
        String[] split = Optional.ofNullable(workOrder.getWorkorderIdExtend()).orElse("-").split(",");

        // Build API URL
        String workUrl = split[0].equals("1")
                ? String.format("%s/WorkOrder/app/list", workFlowFormsUrl)
                : String.format("%s/workorder/synthesis/list", workFlowFormsUrl);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(workUrl)
                .get()
                .addHeader("AccessToken", AuthorizationInterceptor.getToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);

            // correct
            ApiResponse<List<ApiResponse.DataItem>> apiResponse = objectMapper.readValue(
                    response.body().string(),
                    new TypeReference<ApiResponse<List<ApiResponse.DataItem>>>() {
                    });

            String typeName = "获取失败";
            if (apiResponse.getCode() == 200) {
                List<ApiResponse.DataItem> dataList = apiResponse.getData();
                if (dataList != null) {
                    for (ApiResponse.DataItem data : dataList) {
                        if (split[0].equals("1")) {
                            if (workOrder.getWorkorderId().equals(data.getAppId())) {
                                typeName = data.getApplicationName();
                                break;
                            }
                        } else {
                            if (workOrder.getWorkorderId().equals(data.getSynthesisId())) {
                                typeName = data.getCategoryName();
                                break;
                            }
                        }
                    }
                }
            }
            table.addCell(createValueCell(typeName, font));
        } catch (IOException e) {
            table.addCell(createValueCell("获取失败", font));
        }

        // workflow ( value )
        table.addCell(createHeaderCell("流程名称", boldFont, headerColor));
        table.addCell(createValueCell("工程维修", font));
    }

    /**
     * Parse
     *
     * @param attachmentUrls JSON
     * @return
     */
    private int parseAttachmentCount(String attachmentUrls) {
        try {
            return JSONArray.parseArray(attachmentUrls).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * forminfo (department/ / info etc.)
     *
     * @param table main table
     * @param boldFont
     * @param font
     * @param headerColor
     * @param workOrder work orderdata
     */
    private void addFormInfo(
            Table table, PdfFont boldFont, PdfFont font, DeviceRgb headerColor, WorkOrder workOrder) {
        String token = AuthorizationInterceptor.getToken();

        table.addCell(new Cell(1, 4).add(createHeaderCell("表单信息", boldFont, headerColor)));
        HistoricProcessInstance historicProcIns = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(workOrder.getProcInsId())
                .includeProcessVariables()
                .singleResult();
        if (!Objects.isNull(historicProcIns)) {
            WFormInfo apiResponse = (WFormInfo) processService.selectFormContent(
                    historicProcIns.getProcessDefinitionId(),
                    historicProcIns.getDeploymentId(), workOrder.getProcInsId());

            List<Map<String, Object>> widgetList = apiResponse.getWidgetList();

            // Update : Process
            if (widgetList.size() == 1) {
                Map<String, Object> stringObjectMap = widgetList.get(0);
                Map<String, Object> options = (Map<String, Object>) stringObjectMap.get("options");
                if (options != null) {
                    // 3
                    Cell combinedCell = new Cell(1, 2)
                            .add(new Paragraph(String.valueOf(options.get("label"))).setFont(font).setFontSize(10))
                            .setTextAlignment(TextAlignment.LEFT)
                            .setPadding(5);
                    table.addCell(combinedCell);
                    // Convert method Process defaultValue
                    String displayValue = convertDefaultValue(options);
                    // null / empty table
                    table.addCell(
                            new Cell(1, 2)
                                    .add(new Paragraph(displayValue).setFont(font).setFontSize(10))
                                    .setTextAlignment(TextAlignment.LEFT)
                                    .setPadding(5));
                }
            } else {
                for (int i = 0; i < widgetList.size(); i++) {
                    Map<String, Object> stringObjectMap = widgetList.get(i);
                    Map<String, Object> options = (Map<String, Object>) stringObjectMap.get("options");

                    if (ObjectUtils.isNotEmpty(options)) {
                        table.addCell(
                                createHeaderCell(options.get("label").toString(), boldFont, headerColor));
                        String displayValue = convertDefaultValue(options);
                        if (i == widgetList.size() - 1 && widgetList.size() % 2 != 0) {
                            table.addCell(new Cell(1, 3).add(createValueCell(displayValue, font)));
                        } else {
                            table.addCell(createValueCell(displayValue, font));
                        }
                    }
                }
            }
        }
    }

    /**
     * Convert defaultValue, if is array Convert to optionItems in value label value ,
     */
    private String convertDefaultValue(Map<String, Object> options) {
        Object defaultValue = options.get("defaultValue");
        if (defaultValue instanceof List) {
            List<?> list = (List<?>) defaultValue;
            // areaOptionItems in optionItems
            Map<String, Object> stringObjectMap = (Map<String, Object>) options.get("areaOptionItems");
            List<Map<String, Object>> optionItems = (List<Map<String, Object>>) stringObjectMap.get("optionItems");
            if (ObjectUtils.isNotEmpty(options)
                    && ObjectUtils.isNotEmpty(optionItems)) {
                return list.stream()
                        // each item in optionItems in find label
                        .map(
                                item -> optionItems.stream()
                                        .filter(optionItem -> optionItem.get("value").toString().equals(item))
                                        .findFirst()
                                        .map(optionItem -> optionItem.get("label").toString())
                                        .orElse(""))
                        .filter(label -> !label.isEmpty())
                        .collect(Collectors.joining(","));
            }
            // if areaOptionItems, Check whether in options.getOptionItems() ( : to
            // ApiResponse.OptionItems）
            if (ObjectUtils.isNotEmpty(optionItems)) {
                return list.stream()
                        .map(
                                item -> optionItems.stream()
                                        .filter(optionItem -> optionItem.get("value").toString().equals(item))
                                        .findFirst()
                                        .map(optionItem -> optionItem.get("label").toString())
                                        .orElse(""))
                        .filter(label -> !label.isEmpty())
                        .collect(Collectors.joining(","));
            }
        }
        // if defaultValue is List,
        return defaultValue != null ? defaultValue.toString() : "";
    }

    /**
     * event
     *
     * @param table main table
     * @param boldFont
     * @param font
     * @param headerColor
     * @param description event
     */
    private void addEventDescription(
            Table table, PdfFont boldFont, PdfFont font, DeviceRgb headerColor, String description) {
        Cell descHeader = createHeaderCell("事件描述", boldFont, headerColor);
        Cell descValue = createValueCell(description, font);
        table.addCell(new Cell(1, 1).add(descHeader));
        table.addCell(new Cell(1, 3).add(descValue));
    }

    /**
     * info
     *
     * @param table main table
     * @param boldFont
     * @param font
     * @param headerColor
     * @param attachmentUrls JSON
     */
    private void addAttachments(
            Table table, PdfFont boldFont, PdfFont font, DeviceRgb headerColor, String attachmentUrls) {
        int attachmentCount = parseAttachmentCount(attachmentUrls);
        Cell attachmentHeader = createHeaderCell("上传图片/视频数量", boldFont, headerColor);
        Cell attachmentValue = createValueCell(String.valueOf(attachmentCount), font);
        table.addCell(new Cell(1, 1).add(attachmentHeader));
        table.addCell(new Cell(1, 3).add(attachmentValue));
    }

    /**
     * approvalrecord ( data)
     *
     * @param table main table
     * @param font
     * @param headerColor
     */
    private void addApprovalRecords(
            Table table, PdfFont font, DeviceRgb headerColor, WorkOrder workOrder) {
        Cell headerCell = createHeaderCell("审批记录", font, headerColor);
        table.addCell(new Cell(1, 4).add(headerCell));

        OkHttpClient client = new OkHttpClient();

        Request.Builder builder = new Request.Builder()
                .url(
                        String.format(
                                "%s/workflow/process/historyProcNodeList?historicProcIns=%s",
                                workFlowFormsUrl, workOrder.getProcInsId()))
                .get();
        ApiHeaderUtil.transferHeaders(builder);
        try (Response response = client.newCall(builder.build()).execute()) {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);

            // to List
            ApiResponse<List<ApiResponse.HistoryProcNodeList>> apiResponse = objectMapper.readValue(
                    response.body().string(),
                    new TypeReference<ApiResponse<List<ApiResponse.HistoryProcNodeList>>>() {
                    });

            List<ApiResponse.HistoryProcNodeList> historyProcNodeList = apiResponse.getData();

            // ( info)
            List<ApiResponse.HistoryProcNodeList> reversedNodeList = new ArrayList<>(historyProcNodeList);
            Collections.reverse(reversedNodeList);

            // after node (Process info)
            for (ApiResponse.HistoryProcNodeList node : reversedNodeList) {
                // Process info (from after nodestart)
                Paragraph record1 = new Paragraph(
                        String.format(
                                "%s/%s/%s",
                                node.getAssigneeName(), node.getActivityName(), node.getEndTime()))
                        .setFont(font);
                table.addCell(new Cell(1, 2).add(record1));
                // node (Process , )
                List<ApiResponse.CommentList> commentList = node.getCommentList();
                if (ObjectUtil.isNotEmpty(commentList)) {
                    if (commentList.size() > 1) {
                        // Process after
                        ApiResponse.CommentList lastComment = commentList.get(commentList.size() - 1);
                        Paragraph record2 = new Paragraph(lastComment.getFullMessage()).setFont(font);
                        table.addCell(new Cell(1, 2).add(record2));

                        // Process ( )
                        List<ApiResponse.CommentList> subList = commentList.subList(0, commentList.size() - 1);
                        for (ApiResponse.CommentList comment : subList) {
                            Paragraph record3 = new Paragraph(
                                    String.format(
                                            "%s/%s/%s",
                                            node.getAssigneeName(), node.getActivityName(), comment.getTime()))
                                    .setFont(font);
                            table.addCell(new Cell(1, 2).add(record3));
                            Paragraph record4 = new Paragraph(comment.getFullMessage()).setFont(font);
                            table.addCell(new Cell(1, 2).add(record4));
                        }
                    } else {
                        //
                        ApiResponse.CommentList comment = commentList.get(0);
                        Paragraph record2 = new Paragraph(comment.getFullMessage()).setFont(font);
                        table.addCell(new Cell(1, 2).add(record2));
                    }
                } else {
                    Paragraph record2 = new Paragraph("").setFont(font);
                    table.addCell(new Cell(1, 2).add(record2));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     *
     * @param text
     * @param font
     * @param bgColor
     * @return object
     */
    private Cell createHeaderCell(String text, PdfFont font, DeviceRgb bgColor) {
        Paragraph p = new Paragraph(text).setFont(font);
        Cell cell = new Cell().add(p).setTextAlignment(TextAlignment.CENTER).setPadding(5);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        return cell;
    }

    /**
     *
     *
     * @param text
     * @param font
     * @return object
     */
    private Cell createValueCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font))
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(5);
    }
}
