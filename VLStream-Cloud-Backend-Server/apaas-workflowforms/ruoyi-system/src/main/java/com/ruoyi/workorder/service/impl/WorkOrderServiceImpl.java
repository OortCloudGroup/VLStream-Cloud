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
import okhttp3.*;
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
import org.springframework.beans.factory.annotation.Value;
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
 * 工单Service业务层处理
 *
 * @author 雷超群
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

    @Value("${http.apaas-workflowforms}")
    private String workFlowFormsUrl;

    /**
     * 查询工单
     */
    @Override
    public WorkOrderVo queryById(String id) {
        return workOrderMapper.selectVoById(id);
    }

    /**
     * 查询工单列表
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
        // 使用PageQuery类的build()方法构建分页对象
        Page<WorkOrderVo> page = pageQuery.build();
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).startedBy(sysUser.getUserId())
                .orderByProcessInstanceStartTime().desc();
        // 构建搜索条件
        ProcessUtils.buildProcessSearch(historicProcessInstanceQuery, processQuery, processEngine, sysUser);

        // 添加流程状态筛选条件
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

        // 先获取workOrderMap，用于后续筛选
        Map<String, WorkOrderVo> workOrderMap = getWorkOrderMap(workOrderBo);

        // 筛选出同时存在于HistoricProcessInstance和workOrderMap中的数据
        List<HistoricProcessInstance> matchedList = filteredDef.stream()
                .filter(hisIns -> workOrderMap.containsKey(hisIns.getId()))
                .collect(Collectors.toList());
        int listSize = matchedList.size();
        int offset = (int) (page.getSize() * (page.getCurrent() - 1));
        // 如果offset超出列表范围，返回空结果
        if (offset >= listSize) {
            page.setTotal(listSize);
            page.setRecords(new ArrayList<>());
            return TableDataInfo.build(page);
        }
        int toIndex = (int) Math.min(offset + page.getSize(), listSize);
        List<HistoricProcessInstance> historicProcessInstances = matchedList.subList(offset, toIndex);
        List<WorkOrderVo> workOrderVoList = new ArrayList<>();
        for (HistoricProcessInstance hisIns : historicProcessInstances) {
            // 通过 map 常数级查找对应工单
            WorkOrderVo workOrderVo = workOrderMap.get(hisIns.getId());
            // 获取流程状态
            HistoricVariableInstance processStatusVariable = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hisIns.getId()).variableName(PROCESS_STATUS_KEY).singleResult();
            String processStatus = null;
            if (ObjectUtil.isNotNull(processStatusVariable)) {
                processStatus = Convert.toStr(processStatusVariable.getValue());
            }
            // 兼容旧流程
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
            // 当前所处流程
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
    // // 构建搜索条件
    // ProcessUtils.buildProcessSearch(taskQuery, processQuery, processEngine,
    // sysUser);
    //
    // // 添加流程状态筛选条件
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
    // // 通过 map 常数级查找对应工单
    // WorkOrderVo workOrders = workOrderMap.get(task.getProcessInstanceId());
    // if (workOrders == null) {
    // continue; // 没有匹配则跳过
    // }
    // WorkOrderVo workOrderVo = new WorkOrderVo();
    // // 当前流程信息
    // workOrderVo.setTaskId(task.getId());
    // workOrderVo.setWorkorderId(workOrders.getWorkorderId());
    // workOrderVo.setTitle(workOrders.getTitle());
    // workOrderVo.setWorkorderIdExtend(workOrders.getWorkorderIdExtend());
    // workOrderVo.setTaskName(task.getName());
    // workOrderVo.setId(workOrders.getId());
    // workOrderVo.setAssignId(workOrders.getAssignId());
    // workOrderVo.setCreateTime(task.getCreateTime());
    // // 流程定义信息
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
    // // 构建搜索条件
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
    // // 如果 processQuery 指定获取全部综合分类
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
    // continue; // 没有匹配则跳过
    // }
    // WorkOrderVo workOrderVo = new WorkOrderVo();
    // // 当前流程信息
    // workOrderVo.setTaskId(histTask.getId());
    // workOrderVo.setWorkorderId(workOrders.getWorkorderId());
    // workOrderVo.setTitle(workOrders.getTitle());
    // workOrderVo.setWorkorderIdExtend(workOrders.getWorkorderIdExtend());
    // workOrderVo.setTaskName(histTask.getName());
    // workOrderVo.setId(workOrders.getId());
    // workOrderVo.setAssignId(workOrders.getAssignId());
    // workOrderVo.setCreateTime(histTask.getCreateTime());
    // // 流程定义信息
    // ProcessDefinition pd =
    // repositoryService.createProcessDefinitionQuery().processDefinitionTenantId(sysUser.getTenantId())
    // .processDefinitionId(histTask.getProcessDefinitionId()).singleResult();
    // workOrderVo.setProcDefName(pd.getName());
    // workOrderVo.setProcDefVersion(pd.getVersion());
    // workOrderVo.setProcInstId(histTask.getProcessInstanceId());
    // workOrderVo.setCategoryName(categoryLookupService.queryCategoryName(workOrders.getWorkorderId(),
    // processQuery.getCategoryType()));
    // // 流程变量
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
     * 通用：把 Task / HistoricTaskInstance 的共有字段填充到 VO 上
     *
     * @param baseMap      事先通过 getWorkOrderMap(workOrderBo) 构造好的全量 Map，使得此方法不用重复调用
     *                     getWorkOrderMap
     * @param processQuery 用于查询分类类型
     * @param sysUser      用于查询流程定义的租户
     * @param voKey        流程实例 ID，用来从 baseMap 拿到对应的 WorkOrderVo
     * @param taskId       任务 ID
     * @param taskName     任务名称
     * @param startTime    任务开始时间（createTime）
     * @param procDefId    流程定义 ID
     * @param variables    流程变量 Map（用来取 PROCESS_STATUS_KEY）
     * @return 填好公共字段的 WorkOrderVo；如果 baseMap 中没有对应 key，返回 null
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
        // 填充共享字段
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
     * 通用分页查询模板
     *
     * @param <Q> 必须是 Flowable 的 Query 类型
     * @param <T> 查询结果的原生类型（Task 或 HistoricTaskInstance）
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
        // 1. 准备 Bo
        workOrderBo.setWorkOrderAppAll(processQuery.getWorkOrderAppAll());
        workOrderBo.setWorkOrderSynthesisAll(processQuery.getWorkOrderSynthesisAll());

        // 2. 构造 Query
        Q query = querySupplier.apply(sysUser);
        // 3. 调用 ProcessUtils，现在 Q 肯定是 Query<?,?> 的子类
        ProcessUtils.buildProcessSearch(query, processQuery, processEngine, sysUser);
        // 4. 额外筛选
        customConditionConfigurer.accept(query, processQuery);

        // 5. 统计、分页、转换逻辑不变
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

        // 如果没有流程实例，直接返回空结果集
        if (CollUtil.isEmpty(processInstances)) {
            return TableDataInfo.build();
        }

        return selectPageCommon(
                processQuery, workOrderBo, pageQuery, sysUser,
                // 一：如何构造待办 TaskQuery
                user -> taskService.createTaskQuery()
                        .taskTenantId(user.getTenantId())
                        .active()
                        .processInstanceIdIn(processInstances)
                        .includeProcessVariables()
                        .taskAssignee(user.getUserId())
                        .orderByTaskCreateTime().desc(),
                // 二：针对 TaskQuery 的额外条件
                (query, pq) -> {
                    TaskQuery tq = (TaskQuery) query;
                    // 流程状态
                    if (StringUtils.isNotBlank(pq.getState())) {
                        tq.processVariableValueEquals(PROCESS_STATUS_KEY, pq.getState());
                    }
                    // 分类
                    processCategoryForTaskQuery(pq, tq);
                },
                // 三：count 和 listPage
                q -> ((TaskQuery) q).count(),
                (q, offset) -> ((TaskQuery) q).listPage(offset, pageQuery.getPageSize()),
                // 四：Task -> WorkOrderVo
                task -> {
                    // 只剩一行「调用公共填充」
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
    // // 一：如何构造已办 HistoricTaskInstanceQuery
    // user -> historyService.createHistoricTaskInstanceQuery()
    // .includeProcessVariables()
    // .finished()
    // .processInstanceIdIn(processInstances)
    // .taskAssignee(String.valueOf(user.getUserId()))
    // .orderByHistoricTaskInstanceEndTime().desc(),
    // // 二：针对 HistoricTask 的额外条件
    // (query, pq) -> {
    // HistoricTaskInstanceQuery hq = (HistoricTaskInstanceQuery) query;
    // // 全部应用分类
    // if (Boolean.TRUE.equals(pq.getWorkOrderAppAll())) {
    // List<String> appList = workOrderAppService.list()
    // .stream().map(WorkOrderApp::getAppId).collect
    // (Collectors.toList());
    // if (!appList.isEmpty()) hq.processCategoryIn(appList);
    // }
    // // 全部综合分类
    // else if (Boolean.TRUE.equals(pq.getWorkOrderSynthesisAll())) {
    // List<String> synList = workOrderSynthesisService.list()
    // .stream().map
    // (WorkOrderSynthesis::getSynthesisId).collect
    // (Collectors.toList());
    // if (!synList.isEmpty()) hq.processCategoryIn(synList);
    // }
    // },
    // // 三：count 和 listPage
    // q -> ((HistoricTaskInstanceQuery) q).count(),
    // (q, offset) -> ((HistoricTaskInstanceQuery) q)
    // .listPage(offset, pageQuery.getPageSize()),
    // // 四：HistoricTaskInstance -> WorkOrderVo
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
    // // 添加已办独有：启动人信息
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

        // 1. 先构建基础的 work order map，用于后续填充
        Map<String, WorkOrderVo> baseMap = getWorkOrderMap(workOrderBo);
        Set<String> processInstances = baseMap.keySet();

        // 如果没有流程实例，直接返回空分页（避免调用 processInstanceIdIn 空集合导致异常）
        if (processInstances == null || processInstances.isEmpty()) {
            Page<WorkOrderVo> emptyPage = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return TableDataInfo.build(emptyPage);
        }

        // 2. 构造 HistoricTaskInstanceQuery （和原 selectPageCommon 中一模一样的基础查询）
        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables() // 一并查询流程变量，后面
                // fillCommonFields 需要用到
                .finished() // 只查询已完成（已办）
                .processInstanceIdIn(processInstances) // 限定在我们关心的流程实例集合中
                .taskAssignee(String.valueOf(sysUser.getUserId()))
                .orderByHistoricTaskInstanceEndTime().desc(); //
        // 原始排序（这里主要是为了 API 一致性）

        // 3. 应用额外的查询条件（与原 lambda 中内容一致）
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

        // 4. 把所有匹配到的历史任务一次性拉回（为去重做准备）
        List<HistoricTaskInstance> allHistoricTasks = taskQuery.list();

        // 5. 根据 processInstanceId 去重：只保留 createTime 最新的一条
        Map<String, HistoricTaskInstance> latestByProcIns = new HashMap<>(allHistoricTasks.size());
        for (HistoricTaskInstance hti : allHistoricTasks) {
            String procInsId = hti.getProcessInstanceId();
            if (procInsId == null) {
                // 没有流程实例 id 的记录跳过（按需可修改）
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
                // 如果当前记录的 createTime 更晚，则替换
                if (curMillis > existMillis) {
                    latestByProcIns.put(procInsId, hti);
                }
            }
        }

        // 6. 转为 List 并按 createTime 倒序排序（最近的在前）
        List<HistoricTaskInstance> dedupList = new ArrayList<>(latestByProcIns.values());
        dedupList.sort((a, b) -> {
            Date da = a.getCreateTime();
            Date db = b.getCreateTime();
            long ta = (da == null) ? Long.MIN_VALUE : da.getTime();
            long tb = (db == null) ? Long.MIN_VALUE : db.getTime();
            return Long.compare(tb, ta); // 倒序：最近的在前
        });

        // 7. 内存分页（基于去重后的集合）
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

        // 8. 把分页后的 HistoricTaskInstance 转成 WorkOrderVo（复用 fillCommonFields）
        List<WorkOrderVo> voList = new ArrayList<>();
        for (HistoricTaskInstance hist : pageHistoricTasks) {
            WorkOrderVo vo = fillCommonFields(
                    baseMap,
                    processQuery,
                    sysUser,
                    hist.getProcessInstanceId(), // 流程实例 id
                    hist.getId(), // historic task id
                    hist.getName(), // 任务名称
                    hist.getCreateTime(), // createTime，用于显示/排序依据
                    hist.getProcessDefinitionId(), // 流程定义 id
                    hist.getProcessVariables() // 流程变量
            );
            if (vo == null) {
                continue; // fillCommonFields 可能在找不到 baseMap 条目时返回 null
            }

            // 添加已办独有字段：启动人信息（从历史流程实例中取）
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

        // 9. 构造返回的分页对象并返回
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
        // 构建搜索条件
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
                continue; // 没有匹配则跳过
            }
            // 当前流程信息
            workOrders.setTaskId(task.getId());
            workOrders.setCreateTime(task.getCreateTime());
            workOrders.setTaskName(task.getName());
            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            workOrders.setProcDefName(pd.getName());
            workOrders.setProcDefVersion(pd.getVersion());
            workOrders.setCategoryName(categoryLookupService.queryCategoryName(workOrders.getWorkorderId(),
                    processQuery.getCategoryType()));
            // 流程发起人信息
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
                .filter(wo -> wo.getProcInsId() != null) // 过滤掉 null key
                .collect(Collectors.toMap(
                        WorkOrderVo::getProcInsId,
                        Function.identity(),
                        (existing, replacement) -> existing // 若有重复，保留第一个
                ));
    }

    /**
     * 根据传入的 processQuery 判断需要从哪个服务中获取分类列表，然后将结果传递给 taskQuery 进行处理。
     * @param processQuery 包含业务条件的查询参数
     * @param taskQuery    需要设置分类条件的查询对象
     */
    public void processCategoryForTaskQuery(ProcessQuery processQuery, TaskQuery taskQuery) {
        // 如果 processQuery 指定获取全部APP分类
        if (Boolean.TRUE.equals(processQuery.getWorkOrderAppAll())) {
            List<String> appList = workOrderAppService.list()
                    .stream()
                    .map(WorkOrderApp::getAppId)
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                taskQuery.processCategoryIn(appList);
            }
        }
        // 如果 processQuery 指定获取全部综合分类
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
     * 查询工单列表
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
     * 新增工单
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
        long nanoTime = System.nanoTime(); // 获取当前纳秒时间戳
        int random = new Random().nextInt(9999); // 随机数，确保唯一性
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
     * 修改工单
     */
    @Override
    public Boolean updateByBo(WorkOrderBo bo) {
        WorkOrder update = BeanUtil.toBean(bo, WorkOrder.class);
        validEntityBeforeSave(update);
        return workOrderMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WorkOrder entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除工单
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return workOrderMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 工单结束更新状态
     *
     * @param processInstanceId
     * @return 是否更新成功
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    public boolean updateWorkOrderToPending(String processInstanceId, String status, String assignId) {
        // 查询工单
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrder::getProcInsId, processInstanceId);
        WorkOrder existingOrder = getOne(queryWrapper);
        if (ObjectUtil.isNull(existingOrder)) {
            log.warn("未找到对应的工单");
            return false;
        }

        // 使用 LambdaUpdateWrapper 显式设置 null（updateById 默认忽略 null 值）
        LambdaUpdateWrapper<WorkOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WorkOrder::getId, existingOrder.getId())
                .set(WorkOrder::getWorkorderStatus, status)
                .set(WorkOrder::getTaskId, null); // 显式设置为 null

        // 执行更新并返回结果
        return update(updateWrapper);
    }

    /**
     * 更新工单状态
     *
     * @param task 任务列表（需确保非空且至少包含一个任务）
     * @return 是否更新成功
     * @throws IllegalArgumentException 参数校验失败时抛出
     *
     */
    public boolean updateWorkOrderToPending(Task task, String status, String assignId) {
        // 查询工单
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrder::getProcInsId, task.getProcessInstanceId());
        WorkOrder existingOrder = getOne(queryWrapper);
        if (ObjectUtil.isNull(existingOrder)) {
            log.warn("未找到对应的工单");
            return false;
        }
        WorkOrderBo workOrderBo = new WorkOrderBo();
        // 设置指派人
        if (StringUtils.isNotBlank(assignId)) {
            workOrderBo.setAssignId(assignId);
        }
        // 更新工单状态和ID
        workOrderBo.setWorkorderStatus(status);
        if (ObjectUtil.isNotNull(task)) {
            workOrderBo.setTaskId(task.getId());
        }
        workOrderBo.setId(existingOrder.getId());

        // 执行更新并返回结果
        return updateByBo(workOrderBo);
    }

    /**
     * 查询即时工单分页列表
     */
    @Override
    public TableDataInfo<WorkOrderVo> queryImmediatePageList(WorkOrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, false);
        Page<WorkOrderVo> result = workOrderMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询即时工单列表
     */
    @Override
    public List<WorkOrderVo> queryImmediateList(WorkOrderBo bo) {
        LambdaQueryWrapper<WorkOrder> lqw = buildQueryWrapper(bo, false);
        return workOrderMapper.selectVoList(lqw);
    }

    @Override
    public TableDataInfo<Object> queryLoopPageList(WorkOrderBo bo, PageQuery pageQuery, SysUser sysUser) {
        Page<WorkOrderVo> page = pageQuery.build();
        // 使用PageQuery类的build()方法构建分页对象
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
                        (existing, replacement) -> existing // 若有重复，保留第一个
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
                        // 使用逗号拼接部门名称
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
        // 根据用户配置动态返回字段
        List<Object> result;
        if (fieldCodes == null || fieldCodes.isEmpty()) {
            // 未配置字段，返回完整的 WfTaskVo 对象
            result = new ArrayList<>(workOrderVoList);
        } else {
            // 根据配置的字段码过滤返回字段
            result = workOrderVoList.stream().map(vo -> {
                // 将 WfTaskVo 转换为 Map
                Map<String, Object> fieldAll = BeanUtil.beanToMap(vo, false, true);

                // 只保留配置的字段
                Map<String, Object> filteredFields = new LinkedHashMap<>();
                for (String code : fieldCodes) {
                    if (fieldAll.containsKey(code)) {
                        filteredFields.put(code, fieldAll.get(code));
                    }
                }
                return filteredFields;
            }).collect(Collectors.toList());
        }
        // 构建分页结果
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
     * 生成包含多个工单的PDF文件流
     *
     * @param wordOrderIds 工单ID列表
     * @return PDF文件流
     * @throws IOException IO异常
     */
    public ByteArrayOutputStream generatePdf(List<String> wordOrderIds, SysUser sysUser) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // 初始化中文字体（使用标准宋体）
        PdfFont font = PdfFontFactory.createFont(
                "STSongStd-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont boldFont = PdfFontFactory.createFont(
                "STSongStd-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        DeviceRgb headerColor = new DeviceRgb(220, 220, 220); // 表头背景色

        // 查询工单数据（MyBatis Plus查询）
        LambdaQueryWrapper<WorkOrder> lqw = new LambdaQueryWrapper<>();
        lqw.in(WorkOrder::getId, wordOrderIds);
        List<WorkOrder> workOrderList = workOrderMapper.selectList(lqw);

        // 遍历每个工单生成PDF内容
        for (WorkOrder workOrder : workOrderList) {
            // 创建工单容器
            Div workOrderDiv = new Div();
            workOrderDiv.setWidth(UnitValue.createPercentValue(100));

            // 1. 添加标题
            Paragraph title = new Paragraph(workOrder.getTitle())
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);

            // 2. 创建表头表格（工单编号 + 打印时间）
            Table headerTable = createHeaderTable(font, workOrder.getWorkorderNumber());

            // 3. 创建主内容表格
            Table mainTable = createMainTable(font, boldFont, headerColor, workOrder);

            // 将元素添加到工单容器
            workOrderDiv.add(title).add(headerTable).add(mainTable);

            // 计算工单内容所需高度
            LayoutResult layoutResult = workOrderDiv
                    .createRendererSubTree()
                    .setParent(document.getRenderer())
                    .layout(new LayoutContext(new LayoutArea(0, PageSize.A4)));
            float requiredHeight = layoutResult.getOccupiedArea().getBBox().getHeight();

            // 动态获取当前页面剩余空间（安全获取方式）
            int totalPages = pdfDoc.getNumberOfPages();
            Rectangle currentPageSize = totalPages > 0 ? pdfDoc.getPage(totalPages).getPageSize() : PageSize.A4; //
            // 默认使用A4尺寸
            float currentY = totalPages > 0
                    ? document.getRenderer().getCurrentArea().getBBox().getY()
                    : PageSize.A4.getTop(); // 初始位置设为页面顶部

            float remainingSpace = currentY - (currentPageSize.getBottom() + document.getBottomMargin() + 20);

            // 剩余空间不足时插入分页符（添加安全判断）
            if (remainingSpace < requiredHeight && totalPages > 0) {
                document.add(new AreaBreak());
            }
            // 添加工单内容到文档
            document.add(workOrderDiv);
            // 流程访问日志
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
     * 创建表头信息表格（工单编号+打印时间）
     *
     * @param font            字体
     * @param workorderNumber 工单编号
     * @return 表格对象
     */
    private Table createHeaderTable(PdfFont font, String workorderNumber) {
        Table headerTable = new Table(new float[] { 1, 3 }) // 左右列比例1:3
                .useAllAvailableWidth()
                .setMarginBottom(10);

        // 工单编号单元格（左对齐）
        Paragraph orderPara = new Paragraph("工单编号：" + workorderNumber);
        orderPara.setFont(font);
        Cell orderCell = new Cell()
                .add(orderPara)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(5)
                .setBorder(Border.NO_BORDER);

        // 打印时间单元格（右对齐）
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
     * 创建主内容表格
     *
     * @param font        普通字体
     * @param boldFont    加粗字体
     * @param headerColor 表头背景色
     * @param workOrder   工单数据
     * @return 表格对象
     */
    private Table createMainTable(
            PdfFont font, PdfFont boldFont, DeviceRgb headerColor, WorkOrder workOrder) {
        float[] columnWidths = { 150f, 150f, 150f, 150f }; // 四列等宽
        Table table = new Table(columnWidths).useAllAvailableWidth();

        // 添加各部分数据
        addBasicInfo(table, boldFont, font, headerColor, workOrder);
        addEventDescription(table, boldFont, font, headerColor,
                Optional.ofNullable(workOrder.getDescription()).orElse("-"));
        addAttachments(table, boldFont, font, headerColor, workOrder.getAttachmentUrls());
        addFormInfo(table, boldFont, font, headerColor, workOrder);
        addApprovalRecords(table, font, headerColor, workOrder);

        return table;
    }

    /**
     * 添加基础信息部分（所属系统/项目名称/工单类型等）
     *
     * @param table       主表格
     * @param boldFont    加粗字体
     * @param font        普通字体
     * @param headerColor 表头颜色
     * @param workOrder   工单数据
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
        // 所属系统
        table.addCell(createHeaderCell("所属系统", boldFont, headerColor));
        table.addCell(createValueCell(Optional.ofNullable(workOrder.getSystemId()).orElse("-"), font));

        // 项目名称
        table.addCell(createHeaderCell("项目名称", boldFont, headerColor));
        table.addCell(createValueCell(resolveLocalProjectName(workOrder.getProjectId()), font));

        // 工单类型（需要调用外部API）
        table.addCell(createHeaderCell("工单类型", boldFont, headerColor));
        String[] split = Optional.ofNullable(workOrder.getWorkorderIdExtend()).orElse("-").split(",");

        // 构建API请求URL
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

            // 正确指定泛型类型
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

        // 流程名称（固定值）
        table.addCell(createHeaderCell("流程名称", boldFont, headerColor));
        table.addCell(createValueCell("工程维修", font));
    }

    /**
     * 解析附件数量
     *
     * @param attachmentUrls JSON格式的附件地址
     * @return 附件数量
     */
    private int parseAttachmentCount(String attachmentUrls) {
        try {
            return JSONArray.parseArray(attachmentUrls).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 添加表单信息（部门/申请人/客户信息等）
     *
     * @param table       主表格
     * @param boldFont    加粗字体
     * @param font        普通字体
     * @param headerColor 表头颜色
     * @param workOrder   工单数据
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

            // 核心修改：动态处理列合并
            if (widgetList.size() == 1) {
                Map<String, Object> stringObjectMap = widgetList.get(0);
                Map<String, Object> options = (Map<String, Object>) stringObjectMap.get("options");
                if (options != null) {
                    // 创建合并3列的单元格
                    Cell combinedCell = new Cell(1, 2)
                            .add(new Paragraph(String.valueOf(options.get("label"))).setFont(font).setFontSize(10))
                            .setTextAlignment(TextAlignment.LEFT)
                            .setPadding(5);
                    table.addCell(combinedCell);
                    // 使用转换方法处理 defaultValue
                    String displayValue = convertDefaultValue(options);
                    // 补充空单元格保持表格结构
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
     * 转换 defaultValue，如果是数组则转换为对应 optionItems 中 value 对应的 label 值，否则直接返回字符串
     */
    private String convertDefaultValue(Map<String, Object> options) {
        Object defaultValue = options.get("defaultValue");
        if (defaultValue instanceof List) {
            List<?> list = (List<?>) defaultValue;
            // 尝试使用 areaOptionItems 中的 optionItems
            Map<String, Object> stringObjectMap = (Map<String, Object>) options.get("areaOptionItems");
            List<Map<String, Object>> optionItems = (List<Map<String, Object>>) stringObjectMap.get("optionItems");
            if (ObjectUtils.isNotEmpty(options)
                    && ObjectUtils.isNotEmpty(optionItems)) {
                return list.stream()
                        // 针对每个 item 在 optionItems 中查找匹配的 label
                        .map(
                                item -> optionItems.stream()
                                        .filter(optionItem -> optionItem.get("value").toString().equals(item))
                                        .findFirst()
                                        .map(optionItem -> optionItem.get("label").toString())
                                        .orElse(""))
                        .filter(label -> !label.isEmpty())
                        .collect(Collectors.joining(","));
            }
            // 如果没有 areaOptionItems，则判断是否存在 options.getOptionItems()（注意：此处类型为
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
        // 如果 defaultValue 不是 List，则直接返回字符串形式
        return defaultValue != null ? defaultValue.toString() : "";
    }

    /**
     * 添加事件描述
     *
     * @param table       主表格
     * @param boldFont    加粗字体
     * @param font        普通字体
     * @param headerColor 表头颜色
     * @param description 事件描述内容
     */
    private void addEventDescription(
            Table table, PdfFont boldFont, PdfFont font, DeviceRgb headerColor, String description) {
        Cell descHeader = createHeaderCell("事件描述", boldFont, headerColor);
        Cell descValue = createValueCell(description, font);
        table.addCell(new Cell(1, 1).add(descHeader));
        table.addCell(new Cell(1, 3).add(descValue));
    }

    /**
     * 添加附件信息
     *
     * @param table          主表格
     * @param boldFont       加粗字体
     * @param font           普通字体
     * @param headerColor    表头颜色
     * @param attachmentUrls 附件地址JSON
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
     * 添加审批记录（示例数据）
     *
     * @param table       主表格
     * @param font        字体
     * @param headerColor 表头颜色
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

            // 修正泛型类型为 List
            ApiResponse<List<ApiResponse.HistoryProcNodeList>> apiResponse = objectMapper.readValue(
                    response.body().string(),
                    new TypeReference<ApiResponse<List<ApiResponse.HistoryProcNodeList>>>() {
                    });

            List<ApiResponse.HistoryProcNodeList> historyProcNodeList = apiResponse.getData();

            // 创建逆序列表（仅用于基础信息）
            List<ApiResponse.HistoryProcNodeList> reversedNodeList = new ArrayList<>(historyProcNodeList);
            Collections.reverse(reversedNodeList);

            // 遍历逆序后的节点列表（处理基础信息）
            for (ApiResponse.HistoryProcNodeList node : reversedNodeList) {
                // 处理基础信息（从最后一个节点开始）
                Paragraph record1 = new Paragraph(
                        String.format(
                                "%s/%s/%s",
                                node.getAssigneeName(), node.getActivityName(), node.getEndTime()))
                        .setFont(font);
                table.addCell(new Cell(1, 2).add(record1));
                // 遍历原始节点列表（处理评论，保持原有顺序）
                List<ApiResponse.CommentList> commentList = node.getCommentList();
                if (ObjectUtil.isNotEmpty(commentList)) {
                    if (commentList.size() > 1) {
                        // 处理最后一条评论
                        ApiResponse.CommentList lastComment = commentList.get(commentList.size() - 1);
                        Paragraph record2 = new Paragraph(lastComment.getFullMessage()).setFont(font);
                        table.addCell(new Cell(1, 2).add(record2));

                        // 处理其他评论（保持原有顺序）
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
                        // 单条评论
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
     * 创建表头单元格
     *
     * @param text    文本内容
     * @param font    字体
     * @param bgColor 背景颜色
     * @return 单元格对象
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
     * 创建普通内容单元格
     *
     * @param text 文本内容
     * @param font 字体
     * @return 单元格对象
     */
    private Cell createValueCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font))
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(5);
    }
}
