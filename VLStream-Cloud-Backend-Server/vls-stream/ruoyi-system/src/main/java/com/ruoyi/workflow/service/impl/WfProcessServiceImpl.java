/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.*;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.job.dto.CommonJobInfo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.XxlJobUtil;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.common.constant.TaskConstants;
import com.ruoyi.flowable.common.enums.FlowComment;
import com.ruoyi.flowable.common.enums.ProcessStatus;
import com.ruoyi.flowable.common.enums.WorkOrderStatus;
import com.ruoyi.flowable.core.FormConf;
import com.ruoyi.flowable.core.WFormInfo;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.flowable.flow.FlowableUtils;
import com.ruoyi.flowable.utils.*;
import com.ruoyi.system.domain.SysUserRoleView;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.workflow.domain.*;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.domain.bo.ProcessViewLogBo;
import com.ruoyi.workflow.domain.dto.WfMetaInfoDto;
import com.ruoyi.workflow.domain.vo.*;
import com.ruoyi.workflow.mapper.WfAttachmentMapper;
import com.ruoyi.workflow.mapper.WfDeployFormMapper;
import com.ruoyi.workflow.service.*;
import com.ruoyi.workflow.utils.ConvertCronUtils;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author KonBAI
 *         2022/3/24 18:57
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class WfProcessServiceImpl extends FlowServiceFactory implements IWfProcessService {

    private static final Logger logger = LoggerFactory.getLogger(WfProcessServiceImpl.class);
    private final IWfTaskService wfTaskService;
    private final IReModeJsonService reModeJsonService;
    private final UserService userService;
    private final ISysDeptService deptService;
    private final WfDeployFormMapper deployFormMapper;
    private final ISysDeptService sysDeptService;
    private final ISysUserService sysUserService;
    private final ISysRoleService sysRoleService;
    private final IWfCopyService wfCopyService;
    private final ProcessEngine processEngine;
    private final WfAttachmentMapper wfAttachmentMapper;
    private final IWorkOrderService workOrderService;
    private final IWorkOrderAppService workOrderAppService;
    private final XxlJobUtil xxlJobUtil;
    private final IWfAppService wfAppService;
    private final IWfSynthesisService wfSynthesisService;
    private final CategoryLookupService categoryLookupService;
    private final IWfUserInterfaceFieldService wfUserInterfaceFieldService;
    private final IProcessViewLogService processViewLogService;
    private final IWfFormService wfFormService;

    // Parse Replace workflow variable value method
    public static String resolveExpression(String expression, HistoricProcessInstance historicProcIns) {
        if (expression == null || expression.isEmpty() || historicProcIns == null) {
            return expression;
        }

        // , UUID
        String[] tokens = splitExpressionSafely(expression);

        StringBuilder resolvedExpression = new StringBuilder();

        for (String token : tokens) {
            if (isVariableToken(token)) {
                String resolvedValue = processBooleanParameter(token, historicProcIns);
                resolvedExpression.append(resolvedValue);
            } else {
                resolvedExpression.append(token);
            }
        }
        return resolvedExpression.toString();
    }

    // after method , UUID
    private static String[] splitExpressionSafely(String expression) {
        return expression.split("(?<![0-9a-fA-F\\-])(?<=\\W)(?=\\w)|(?<=\\w)(?=\\W)(?![0-9a-fA-F\\-])");
    }

    // Check whether is variable ( : processDefId, initiator, notifyAllSteps)
    private static boolean isVariableToken(String token) {
        return token.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") || isUUID(token);
    }

    // Process workflow variable value
    private static String processBooleanParameter(String param, HistoricProcessInstance historicProcIns) {
        Map<String, Object> processVariables = historicProcIns.getProcessVariables();
        Object o = processVariables.get(param);

        String valueStr = o != null ? o.toString() : param;

        // if ("1".equals(valueStr)) {
        // return "true";
        // } else if ("0".equals(valueStr)) {
        // return "false";
        // } else
        if (isNumeric(valueStr)) {
            return valueStr; //
        } else {
            if (isUUID(valueStr)) {
                return "'" + valueStr + "'";
            }
            return "'" + valueStr.replaceAll("'", "\\'") + "'"; // 字符串加引号
        }
    }

    // Check whether to UUID
    private static boolean isUUID(String value) {
        return value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    // Check whether to
    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    // /**
    // * workflow definition
    // *
    // * @param pageQuery parameter
    // * @return workflow definition data
    // */
    // @Override
    // public TableDataInfo<WfDefinitionVo> selectPageStartProcessList(ProcessQuery
    // processQuery, PageQuery pageQuery) {
    // Page<WfDefinitionVo> page = new Page<>();
    // Page<WfDefinitionVo> page = pageQuery.build();
    // // workflow definition dataQuery
    // ProcessDefinitionQuery processDefinitionQuery =
    // repositoryService.createProcessDefinitionQuery()
    // .latestVersion()
    // .active()
    // .orderByProcessDefinitionKey()
    // .asc();
    // // Build
    // ProcessUtils.buildProcessSearch(processDefinitionQuery, processQuery,
    // processEngine);
    // long pageTotal = processDefinitionQuery.count();
    // if (pageTotal <= 0) {
    // return TableDataInfo.build();
    // }
    // int offset = (int) (page.getSize() * (page.getCurrent() - 1));
    // List<ProcessDefinition> definitionList = processDefinitionQuery
    // .listPage(offset, (int) page.getSize());
    // List<WfDefinitionVo> definitionVoList = new ArrayList<>();
    // for (ProcessDefinition processDefinition : definitionList) {
    // String deploymentId = processDefinition.getDeploymentId();
    // Deployment deployment =
    // repositoryService.createDeploymentQuery().deploymentId(deploymentId)
    // .singleResult();
    // WfDefinitionVo vo = new WfDefinitionVo();
    // vo.setDefinitionId(processDefinition.getId());
    // vo.setProcessKey(processDefinition.getKey());
    // vo.setProcessName(processDefinition.getName());
    // vo.setVersion(processDefinition.getVersion());
    // vo.setDeploymentId(processDefinition.getDeploymentId());
    // vo.setSuspended(processDefinition.isSuspended());
    // // workflow definition
    // vo.setCategory(deployment.getCategory());
    // vo.setDeploymentTime(deployment.getDeploymentTime());
    // // Get id
    // Model model = repositoryService.createModelQuery()
    // .modelKey(deployment.getKey())
    // .latestVersion()
    // .singleResult();
    // if(model != null) {
    // WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(),
    // WfMetaInfoDto.class);
    // if (metaInfo != null) {
    // vo.setIconId(metaInfo.getIconId());
    // }
    // }
    // definitionVoList.add(vo);
    // }
    // page.setRecords(definitionVoList);
    // page.setTotal(pageTotal);
    // return TableDataInfo.build(page);
    // }
    @Override
    public List<WfDefinitionVo> selectStartProcessList(ProcessQuery processQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // workflow definition dataQuery
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).latestVersion().active().orderByProcessDefinitionKey()
                .asc();
        // Build
        ProcessUtils.buildProcessSearch(processDefinitionQuery, processQuery, processEngine, sysUser);

        List<ProcessDefinition> definitionList = processDefinitionQuery.list();

        List<WfDefinitionVo> definitionVoList = new ArrayList<>();
        for (ProcessDefinition processDefinition : definitionList) {
            String deploymentId = processDefinition.getDeploymentId();
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
            WfDefinitionVo vo = new WfDefinitionVo();
            vo.setDefinitionId(processDefinition.getId());
            vo.setProcessKey(processDefinition.getKey());
            vo.setProcessName(processDefinition.getName());
            vo.setVersion(processDefinition.getVersion());
            vo.setDeploymentId(processDefinition.getDeploymentId());
            vo.setSuspended(processDefinition.isSuspended());
            // workflow definition
            vo.setCategory(deployment.getCategory());
            vo.setDeploymentTime(deployment.getDeploymentTime());
            definitionVoList.add(vo);
        }
        return definitionVoList;
    }

    @Override
    public TableDataInfo<WfDefinitionVo> selectPageStartProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        // Build parameter
        Page<WfDefinitionVo> page = pageQuery.build();
        // workflow definition dataQuery
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).latestVersion().active();

        // Build
        ProcessUtils.buildProcessSearch(processDefinitionQuery, processQuery, processEngine, sysUser);
        List<ProcessDefinition> result = new ArrayList<>();
        List<ProcessDefinition> filteredDef;
        long pageTotal;
        if (Boolean.TRUE.equals(processQuery.getWfAppAll())) {
            List<String> appList = wfAppService.list().stream().map(item -> item.getAppId())
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                for (String appId : appList) {
                    List<ProcessDefinition> list = processDefinitionQuery.processDefinitionCategory(appId).list();
                    if (ObjectUtil.isNotEmpty(list)) {
                        result.addAll(list);
                    }
                }
            }
            pageTotal = result.size();
            filteredDef = result;
        } else if (Boolean.TRUE.equals(processQuery.getWfSynthesisAll())) {
            List<String> wfSynthesisList = wfSynthesisService.list().stream().map(item -> item.getSynthesisId())
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
                for (String synthesisId : wfSynthesisList) {
                    List<ProcessDefinition> list = processDefinitionQuery.processDefinitionCategory(synthesisId).list();
                    if (ObjectUtil.isNotEmpty(list)) {
                        result.addAll(list);
                    }
                }
            }
            pageTotal = result.size();
            filteredDef = result;
        } else {
            pageTotal = processDefinitionQuery.count();
            if (pageTotal <= 0) {
                return TableDataInfo.build();
            }
            filteredDef = processDefinitionQuery.list();
        }

        // API Query , Get all workflow definition
        // whether
        // META_INFO_ field
        // Stream API
        if (StringUtils.isNotBlank(processQuery.getShowMobile())) {
            filteredDef = filteredDef.stream().filter(processDefinition -> {
                // Query new model
                Model latestModel = repositoryService.createModelQuery().modelTenantId(sysUser.getTenantId())
                        .modelKey(processDefinition.getKey()).latestVersion().singleResult();

                // Get META_INFO_ field Parse
                if ((latestModel != null && StringUtils.isNotBlank(latestModel.getMetaInfo()))) {
                    JSONObject metaInfoObject = JSON.parseObject(latestModel.getMetaInfo());
                    // Check showMobile whether is empty
                    return StringUtils.isEmpty(metaInfoObject.getString("showMobile"))
                            || (StringUtils.isNotBlank(metaInfoObject.getString("showMobile"))
                                    && metaInfoObject.getString("showMobile").equals(processQuery.getShowMobile()));
                }
                return false; // if latestModel, workflow definition
            }).collect(Collectors.toList()); // to
        }

        // Convert workflow definition to Vo info
        List<WfDefinitionVo> definitionVoList = filteredDef.stream().map(def -> {
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(def.getDeploymentId())
                    .singleResult();
            WfDefinitionVo vo = new WfDefinitionVo();
            vo.setDefinitionId(def.getId());
            vo.setProcessKey(def.getKey());
            vo.setProcessName(def.getName());
            vo.setVersion(def.getVersion());
            vo.setDeploymentId(def.getDeploymentId());
            vo.setSuspended(def.isSuspended());
            vo.setCategory(deployment.getCategory());
            vo.setDeploymentTime(deployment.getDeploymentTime());
            vo.setCategoryName(categoryLookupService.queryCategoryName(def.getCategory(),
                    processQuery.getCategoryType()));

            // Get info
            Model model = repositoryService.createModelQuery().modelKey(def.getKey()).latestVersion().singleResult();
            if (model != null) {
                WfMetaInfoDto metaInfo = JsonUtils.parseObject(model.getMetaInfo(), WfMetaInfoDto.class);
                if (metaInfo != null) {
                    vo.setIconId(metaInfo.getIconId());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        //
        Collections.sort(definitionVoList, Comparator.comparing(WfDefinitionVo::getDeploymentTime).reversed());

        //
        int offset = (int) (page.getSize() * (page.getCurrent() - 1));
        int toIndex;
        if (Integer.MAX_VALUE < offset + page.getSize()) {
            // , if and can int value , to
            toIndex = definitionVoList.size();
        } else {
            // finish
            toIndex = (int) Math.min(offset + page.getSize(), definitionVoList.size());
        }
        List<WfDefinitionVo> pagedDefinitionVos = definitionVoList.subList(offset, toIndex);
        page.setRecords(pagedDefinitionVos);
        page.setTotal(pageTotal);

        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<Object> selectPageOwnProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        List<String> fieldCodes = JSON.parseArray(wfUserInterfaceFieldService.getFieldCodes(sysUser.getUserId(),
                processQuery.getApiPath()), String.class);

        // Page<WfTaskVo> page = new Page<>();
        // PageQuery build() method Build object
        Page<Object> page = pageQuery.build();
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).startedBy(sysUser.getUserId())
                .orderByProcessInstanceStartTime().desc();
        // Build
        ProcessUtils.buildProcessSearch(historicProcessInstanceQuery, processQuery, processEngine, sysUser);

        // workflow
        if (StringUtils.isNotBlank(processQuery.getState())) {
            historicProcessInstanceQuery.variableValueEquals(ProcessConstants.PROCESS_STATUS_KEY,
                    processQuery.getState());
        }

        List<HistoricProcessInstance> result = new ArrayList<>();
        long pageTotal;
        if (Boolean.TRUE.equals(processQuery.getWfAppAll())) {
            List<String> appList = wfAppService.list().stream().map(item -> item.getAppId())
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
        } else if (Boolean.TRUE.equals(processQuery.getWfSynthesisAll())) {
            List<String> wfSynthesisList = wfSynthesisService.list().stream().map(item -> item.getSynthesisId())
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
        } else {
            pageTotal = historicProcessInstanceQuery.count();
            if (pageTotal <= 0) {
                return TableDataInfo.build();
            }
            result = historicProcessInstanceQuery.list();
        }

        result.sort(Comparator.comparing(HistoricProcessInstance::getStartTime).reversed());
        Integer offset = (int) (page.getSize() * (page.getCurrent() - 1));
        Integer toIndex;
        if (Integer.MAX_VALUE < offset + page.getSize()) {
            // , if and can int value , to
            toIndex = Math.toIntExact(pageTotal);
        } else {
            // finish
            toIndex = (int) Math.min(offset + page.getSize(), pageTotal);
        }
        List<HistoricProcessInstance> historicProcessInstances = result.subList(offset, toIndex);
        int count = historicProcessInstances.size();
        if (count == 0) {
            return TableDataInfo.build();
        }
        page.setTotal(count);
        Map<String, SysUser> sysUserMap = sysUserService.list().stream().filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(
                        SysUser::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing // ,
                ));
        List<WfTaskVo> taskVoList = new ArrayList<>();
        for (HistoricProcessInstance hisIns : historicProcessInstances) {
            WfTaskVo taskVo = new WfTaskVo();
            // Get workflow
            HistoricVariableInstance processStatusVariable = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hisIns.getId()).variableName(ProcessConstants.PROCESS_STATUS_KEY).singleResult();
            String processStatus = null;
            if (ObjectUtil.isNotNull(processStatusVariable)) {
                processStatus = Convert.toStr(processStatusVariable.getValue());
            }
            // old workflow
            if (processStatus == null) {
                processStatus = ObjectUtil.isNull(hisIns.getEndTime()) ? ProcessStatus.RUNNING.getStatus()
                        : ProcessStatus.COMPLETED.getStatus();
            }
            // current Execute
            taskVo.setAssigneeName(taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(hisIns.getId()).list().stream().map(task -> {
                        return Optional.ofNullable(sysUserMap.get(task.getAssignee())).map(SysUser::getUserName)
                                .orElse("");
                    }).collect(Collectors.joining(",")));
            taskVo.setProcessStatus(processStatus);
            taskVo.setCreateTime(hisIns.getStartTime());
            taskVo.setFinishTime(hisIns.getEndTime());
            taskVo.setProcInsId(hisIns.getId());

            String userId = hisIns.getStartUserId();
            SysUser startSysUser = sysUserMap.get(userId);
            taskVo.setStartUserId(userId);
            taskVo.setStartUserName(startSysUser.getUserName());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode rootNode = objectMapper.readTree(startSysUser.getDeptInfo());
                List<String> deptNames = new ArrayList<>();
                if (rootNode.isArray()) {
                    for (JsonNode node : rootNode) {
                        String deptName = node.get("dept_name").asText();
                        deptNames.add(deptName);
                    }
                    // department name
                    taskVo.setStartDeptName(String.join(",", deptNames));
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("部门信息解析失败", e);
            }

            //
            if (Objects.nonNull(hisIns.getEndTime())) {
                taskVo.setDuration(DateUtils.getDatePoor(hisIns.getEndTime(), hisIns.getStartTime()));
            } else {
                taskVo.setDuration(DateUtils.getDatePoor(DateUtils.getNowDate(), hisIns.getStartTime()));
            }
            // workflow instanceinfo
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(hisIns.getDeploymentId())
                    .singleResult();
            taskVo.setDeployId(hisIns.getDeploymentId());
            taskVo.setProcDefId(hisIns.getProcessDefinitionId());
            taskVo.setProcDefName(hisIns.getProcessDefinitionName());
            taskVo.setProcDefVersion(hisIns.getProcessDefinitionVersion());
            taskVo.setCategory(deployment.getCategory());
            taskVo.setCategoryName(categoryLookupService.queryCategoryName(deployment.getCategory(),
                    processQuery.getCategoryType()));
            // current workflow
            List<Task> taskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(hisIns.getId()).includeIdentityLinks().list();
            if (CollUtil.isNotEmpty(taskList)) {
                taskVo.setTaskName(taskList.stream().map(Task::getName).filter(StringUtils::isNotEmpty).distinct()
                        .collect(Collectors.joining(",")));
            }
            if (taskVo.getTaskName() == null) {
                taskVo.setTaskName("审批已完成");
            }
            taskVoList.add(taskVo);
        }
        // userconfiguration field
        List<Object> result2;
        if (fieldCodes == null || fieldCodes.isEmpty()) {
            // not configurationfield, WfTaskVo object
            result2 = new ArrayList<>(taskVoList);
        } else {
            // configuration field field
            result2 = taskVoList.stream().map(vo -> {
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
        resultPage.setRecords(result2);
        return TableDataInfo.build(resultPage);
    }

    @Override
    public TableDataInfo<WfTaskVo> selectPageAllProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            String token) {
        SysUser user = getSysUser(token);
        // Page<WfTaskVo> page = new Page<>();
        // PageQuery build() method Build object
        Page<WfTaskVo> page = pageQuery.build();
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(user.getTenantId())
                // .startedBy(user.getUserId())
                // .startedBy(TaskUtils.getUserId())
                .orderByProcessInstanceStartTime().desc();
        // Build
        ProcessUtils.buildProcessSearch(historicProcessInstanceQuery, processQuery, processEngine, user);
        // workflow
        // if (StringUtils.isNotBlank(processQuery.getState())) {
        // historicProcessInstanceQuery.variableValueEquals(ProcessConstants.PROCESS_STATUS_KEY,
        // processQuery
        // .getState());
        // }
        List<HistoricProcessInstance> historicProcessInstances = historicProcessInstanceQuery.listPage(
                (int) ((page.getCurrent() - 1) * page.getSize()),
                (int) page.getSize());
        page.setTotal(historicProcessInstanceQuery.count());
        List<WfTaskVo> taskVoList = new ArrayList<>();
        for (HistoricProcessInstance hisIns : historicProcessInstances) {
            WfTaskVo taskVo = new WfTaskVo();
            // Get workflow
            HistoricVariableInstance processStatusVariable = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hisIns.getId()).variableName(ProcessConstants.PROCESS_STATUS_KEY).singleResult();
            String processStatus = null;
            if (ObjectUtil.isNotNull(processStatusVariable)) {
                processStatus = Convert.toStr(processStatusVariable.getValue());
            }
            // old workflow
            if (processStatus == null) {
                processStatus = ObjectUtil.isNull(hisIns.getEndTime()) ? ProcessStatus.RUNNING.getStatus()
                        : ProcessStatus.COMPLETED.getStatus();
            }
            taskVo.setProcessStatus(processStatus);
            taskVo.setCreateTime(hisIns.getStartTime());
            taskVo.setFinishTime(hisIns.getEndTime());
            taskVo.setProcInsId(hisIns.getId());
            //
            if (Objects.nonNull(hisIns.getEndTime())) {
                taskVo.setDuration(DateUtils.getDatePoor(hisIns.getEndTime(), hisIns.getStartTime()));
            } else {
                taskVo.setDuration(DateUtils.getDatePoor(DateUtils.getNowDate(), hisIns.getStartTime()));
            }
            // workflow instanceinfo
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentTenantId(user.getTenantId())
                    .deploymentId(hisIns.getDeploymentId()).singleResult();
            taskVo.setDeployId(hisIns.getDeploymentId());
            taskVo.setProcDefId(hisIns.getProcessDefinitionId());
            taskVo.setProcDefName(hisIns.getProcessDefinitionName());
            taskVo.setProcDefVersion(hisIns.getProcessDefinitionVersion());
            taskVo.setCategory(deployment.getCategory());
            // current workflow
            List<Task> taskList = taskService.createTaskQuery().taskTenantId(user.getTenantId())
                    .processInstanceId(hisIns.getId()).includeIdentityLinks().list();
            if (CollUtil.isNotEmpty(taskList)) {
                taskVo.setTaskName(taskList.stream().map(Task::getName).filter(StringUtils::isNotEmpty).distinct()
                        .collect(Collectors.joining(",")));
            }
            taskVoList.add(taskVo);
        }
        page.setRecords(taskVoList);
        return TableDataInfo.build(page);
    }

    /**
     * tokenGet userinfo
     *
     * @param token
     * @return
     */
    private SysUser getSysUser(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if (user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        System.out.println(" 用户缓存信息 " + user);
        return user;
    }

    @Override
    public List<WfTaskVo> selectOwnProcessList(ProcessQuery processQuery, boolean allFlag, String token) {
        SysUser sysUser = getSysUser(token);
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).startedBy(TaskUtils.getUserId())
                .orderByProcessInstanceStartTime().desc();
        if (allFlag) {
            historicProcessInstanceQuery.startedBy(sysUser.getUserId());
        }
        log.info("成功");
        // Build
        ProcessUtils.buildProcessSearch(historicProcessInstanceQuery, processQuery, processEngine, sysUser);
        List<HistoricProcessInstance> historicProcessInstances = historicProcessInstanceQuery
                .processInstanceTenantId(sysUser.getTenantId()).list();
        List<WfTaskVo> taskVoList = new ArrayList<>();
        for (HistoricProcessInstance hisIns : historicProcessInstances) {
            WfTaskVo taskVo = new WfTaskVo();

            taskVo.setCreateTime(hisIns.getStartTime());
            taskVo.setFinishTime(hisIns.getEndTime());
            taskVo.setProcInsId(hisIns.getId());

            //
            if (Objects.nonNull(hisIns.getEndTime())) {
                taskVo.setDuration(DateUtils.getDatePoor(hisIns.getEndTime(), hisIns.getStartTime()));
            } else {
                taskVo.setDuration(DateUtils.getDatePoor(DateUtils.getNowDate(), hisIns.getStartTime()));
            }
            // workflow instanceinfo
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentTenantId(sysUser.getTenantId())
                    .deploymentId(hisIns.getDeploymentId()).singleResult();
            taskVo.setDeployId(hisIns.getDeploymentId());
            taskVo.setProcDefId(hisIns.getProcessDefinitionId());
            taskVo.setProcDefName(hisIns.getProcessDefinitionName());
            taskVo.setProcDefVersion(hisIns.getProcessDefinitionVersion());
            taskVo.setCategory(deployment.getCategory());
            // current workflow
            List<Task> taskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(hisIns.getId()).includeIdentityLinks().list();
            if (CollUtil.isNotEmpty(taskList)) {
                taskVo.setTaskName(taskList.stream().map(Task::getName).filter(StringUtils::isNotEmpty)
                        .collect(Collectors.joining(",")));
            }
            taskVoList.add(taskVo);
        }
        return taskVoList;
    }

    /**
     * processQuery Check need to from service in Get , after taskQuery Process .
     *
     * @param processQuery Query parameter
     * @param taskQuery need to Set Query object
     */
    public void processCategoryForTaskQuery(ProcessQuery processQuery, TaskQuery taskQuery) {
        // if processQuery Get full APP
        if (Boolean.TRUE.equals(processQuery.getWfAppAll())) {
            List<String> appList = wfAppService.list()
                    .stream()
                    .map(WfApp::getAppId)
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                taskQuery.processCategoryIn(appList);
            }
        }
        // if processQuery Get full
        else if (Boolean.TRUE.equals(processQuery.getWfSynthesisAll())) {
            List<String> wfSynthesisList = wfSynthesisService.list()
                    .stream()
                    .map(WfSynthesis::getSynthesisId)
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
                taskQuery.processCategoryIn(wfSynthesisList);
            }
        }
    }

    @Override
    public TableDataInfo<WfTaskVo> selectPageTodoProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            SysUser sysUser) {
        Page<WfTaskVo> page = new Page<>();
        TaskQuery taskQuery = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).active()
                .includeProcessVariables()
                .taskAssignee(String.valueOf(sysUser.getUserId())).orderByTaskCreateTime().desc();
        // Build
        ProcessUtils.buildProcessSearch(taskQuery, processQuery, processEngine, sysUser);

        processCategoryForTaskQuery(processQuery, taskQuery);

        long pageTotal = taskQuery.count();
        if (pageTotal <= 0) {
            return TableDataInfo.build();
        }

        page.setTotal(pageTotal);
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<Task> taskList = taskQuery.listPage(offset, pageQuery.getPageSize());
        List<WfTaskVo> flowList = new ArrayList<>();
        for (Task task : taskList) {
            WfTaskVo flowTask = new WfTaskVo();
            // current workflowinfo
            flowTask.setTaskId(task.getId());
            flowTask.setTaskDefKey(task.getTaskDefinitionKey());
            flowTask.setCreateTime(task.getCreateTime());
            flowTask.setProcDefId(task.getProcessDefinitionId());
            flowTask.setTaskName(task.getName());
            // workflow definitioninfo
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setProcInsId(task.getProcessInstanceId());
            flowTask.setCategory(pd.getCategory());

            // workflow info
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String userId = historicProcessInstance.getStartUserId();
            String userName = userService.selectUserNameById(userId);
            flowTask.setStartUserId(userId);
            flowTask.setStartUserName(userName);
            flowTask.setProInsCreateTime(historicProcessInstance.getStartTime());

            // workflow variable
            flowTask.setProcVars(task.getProcessVariables());
            flowTask.setProcessStatus(String.valueOf(task.getProcessVariables().get("processStatus")));
            flowList.add(flowTask);
        }
        page.setRecords(flowList);
        return TableDataInfo.build(page);
    }

    @Override
    public List<WfTaskVo> selectTodoProcessList(ProcessQuery processQuery, String token) {
        SysUser sysUser = getSysUser(token);
        TaskQuery taskQuery = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).active()
                .includeProcessVariables().taskCandidateOrAssigned(sysUser.getUserId())
                .taskCandidateGroupIn(TaskUtils.getCandidateGroup(sysUser)).orderByTaskCreateTime().desc();
        // Build
        return getWfTaskVos(processQuery, sysUser, taskQuery);
    }

    @NotNull
    private List<WfTaskVo> getWfTaskVos(ProcessQuery processQuery, SysUser sysUser, TaskQuery taskQuery) {
        ProcessUtils.buildProcessSearch(taskQuery, processQuery, processEngine, sysUser);
        List<Task> taskList = taskQuery.list();
        List<WfTaskVo> taskVoList = new ArrayList<>();
        for (Task task : taskList) {
            WfTaskVo taskVo = new WfTaskVo();
            // current workflowinfo
            taskVo.setTaskId(task.getId());
            taskVo.setTaskDefKey(task.getTaskDefinitionKey());
            taskVo.setCreateTime(task.getCreateTime());
            taskVo.setProcDefId(task.getProcessDefinitionId());
            taskVo.setTaskName(task.getName());
            // workflow definitioninfo
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            taskVo.setDeployId(pd.getDeploymentId());
            taskVo.setProcDefName(pd.getName());
            taskVo.setProcDefVersion(pd.getVersion());
            taskVo.setProcInsId(task.getProcessInstanceId());

            // workflow info
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String userId = historicProcessInstance.getStartUserId();
            String userName = userService.selectUserNameById(userId);
            taskVo.setStartUserId(userId);
            taskVo.setStartUserName(userName);

            taskVoList.add(taskVo);
        }
        return taskVoList;
    }

    @Override
    public TableDataInfo<WfTaskVo> selectPageClaimProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            SysUser sysUser) {
        Page<WfTaskVo> page = new Page<>();
        TaskQuery taskQuery = null;
        try {
            taskQuery = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).active()
                    .includeProcessVariables().taskCandidateUser(sysUser.getUserId())
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

        page.setTotal(taskQuery.count());
        int offset = pageQuery.getPageSize() * (pageQuery.getPageNum() - 1);
        List<Task> taskList = taskQuery.listPage(offset, pageQuery.getPageSize());
        List<WfTaskVo> flowList = new ArrayList<>();
        for (Task task : taskList) {
            WfTaskVo flowTask = new WfTaskVo();
            // current workflowinfo
            flowTask.setTaskId(task.getId());
            flowTask.setTaskDefKey(task.getTaskDefinitionKey());
            flowTask.setCreateTime(task.getCreateTime());
            flowTask.setProcDefId(task.getProcessDefinitionId());
            flowTask.setTaskName(task.getName());
            // workflow definitioninfo
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setProcInsId(task.getProcessInstanceId());

            // workflow info
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String userId = historicProcessInstance.getStartUserId();
            String userName = userService.selectUserNameById(userId);
            flowTask.setStartUserId(userId);
            flowTask.setStartUserName(userName);
            flowList.add(flowTask);
            // if (processQuery.getWorkOrderAppAll()) {
            // LambdaQueryWrapper<WorkOrder> query = new LambdaQueryWrapper<>();
            // query.eq(WorkOrder::getProcInstId, task.getProcessInstanceId());
            // WorkOrder workOrderServiceOne = workOrderService.getOne(query);
            // if (ObjectUtil.isNotNull(workOrderServiceOne)) {
            // flowTask.setWorkOrderId(workOrderServiceOne.getId());
            // flowTask.setWorkOrderName(workOrderServiceOne.getTitle());
            // }
            // }
        }
        page.setRecords(flowList);
        return TableDataInfo.build(page);
    }

    // /**
    // * DefinitionKey workflow
    // *
    // * @param procDefKey workflow definitionKey
    // * @param variables parameter
    // */
    // @Override
    // @Transactional(rollbackFor = Exception.class)
    // public void startProcessByDefKey(String procDefKey, Map<String, Object>
    // variables) {
    // try {
    //
    // ProcessDefinition processDefinition =
    // repositoryService.createProcessDefinitionQuery()
    // .processDefinitionKey(procDefKey).latestVersion().singleResult();
    // startProcess(processDefinition, variables);
    // } catch (Exception e) {
    // e.printStackTrace();
    // throw new ServiceException("workflow ");
    // }
    // }

    @Override
    public List<WfTaskVo> selectClaimProcessList(ProcessQuery processQuery, SysUser sysUser) {
        TaskQuery taskQuery = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).active()
                .includeProcessVariables().taskCandidateUser(sysUser.getUserId())
                .taskCandidateGroupIn(TaskUtils.getCandidateGroup(sysUser)).orderByTaskCreateTime().desc();
        // Build
        return getWfTaskVos(processQuery, sysUser, taskQuery);
    }

    @Override
    public TableDataInfo<WfTaskVo> selectPageFinishedProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            SysUser sysUser) {
        Page<WfTaskVo> page = new Page<>();

        // Build HistoricTaskInstanceQuery ( )
        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables()
                .finished()
                .taskAssignee(String.valueOf(sysUser.getUserId()))
                .orderByHistoricTaskInstanceEndTime().desc();

        // Build ( )
        ProcessUtils.buildProcessSearch(taskInstanceQuery, processQuery, processEngine, sysUser);

        if (Boolean.TRUE.equals(processQuery.getWfAppAll())) {
            List<String> appList = wfAppService.list().stream().map(WfApp::getAppId).collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(appList)) {
                taskInstanceQuery.processCategoryIn(appList);
            }
        } else if (Boolean.TRUE.equals(processQuery.getWfSynthesisAll())) {
            List<String> wfSynthesisList = wfSynthesisService.list().stream().map(WfSynthesis::getSynthesisId)
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(wfSynthesisList)) {
                taskInstanceQuery.processCategoryIn(wfSynthesisList);
            }
        }

        List<HistoricTaskInstance> allHistoricTasks = taskInstanceQuery.list();

        // processInstanceId , only createTime new

        // map each workflow instance new HistoricTaskInstance ( createTime to )
        Map<String, HistoricTaskInstance> latestByProcIns = new HashMap<>(allHistoricTasks.size());
        for (HistoricTaskInstance hti : allHistoricTasks) {
            String procInsId = hti.getProcessInstanceId();
            HistoricTaskInstance exist = latestByProcIns.get(procInsId);
            if (exist == null) {
                latestByProcIns.put(procInsId, hti);
            } else {
                Date existCreate = exist.getCreateTime();
                Date curCreate = hti.getCreateTime();
                // null full : null
                long existMillis = (existCreate == null) ? Long.MIN_VALUE : existCreate.getTime();
                long curMillis = (curCreate == null) ? Long.MIN_VALUE : curCreate.getTime();
                if (curMillis > existMillis) {
                    // current createTime new : Replace to new record
                    latestByProcIns.put(procInsId, hti);
                }
            }
        }

        // 3) to List, createTime ( in before)
        List<HistoricTaskInstance> dedupList = new ArrayList<>(latestByProcIns.values());
        dedupList.sort((a, b) -> {
            Date da = a.getCreateTime();
            Date db = b.getCreateTime();
            long ta = (da == null) ? Long.MIN_VALUE : da.getTime();
            long tb = (db == null) ? Long.MIN_VALUE : db.getTime();
            // : in before
            return Long.compare(tb, ta);
        });

        // -------------------------
        // 4) pageQuery
        // -------------------------
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

        // -------------------------
        // 5) after Convert to WfTaskVo ( )
        // -------------------------
        List<WfTaskVo> hisTaskList = new ArrayList<>();
        for (HistoricTaskInstance histTask : pageHistoricTasks) {
            WfTaskVo flowTask = new WfTaskVo();
            flowTask.setCreateTime(histTask.getCreateTime());
            flowTask.setFinishTime(histTask.getEndTime());
            flowTask.setDuration(DateUtil.formatBetween(histTask.getDurationInMillis(), BetweenFormatter.Level.SECOND));
            flowTask.setProcDefId(histTask.getProcessDefinitionId());
            flowTask.setTaskDefKey(histTask.getTaskDefinitionKey());
            flowTask.setTaskName(histTask.getName());

            // workflow definitioninfo
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId())
                    .processDefinitionId(histTask.getProcessDefinitionId())
                    .singleResult();
            if (pd != null) {
                flowTask.setDeployId(pd.getDeploymentId());
                flowTask.setProcDefName(pd.getName());
                flowTask.setProcDefVersion(pd.getVersion());
                flowTask.setCategory(pd.getCategory());
            }

            flowTask.setProcInsId(histTask.getProcessInstanceId());
            flowTask.setHisProcInsId(histTask.getProcessInstanceId());

            // workflow info
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId())
                    .processInstanceId(histTask.getProcessInstanceId())
                    .singleResult();
            if (historicProcessInstance != null) {
                String userId = historicProcessInstance.getStartUserId();
                String userName = userService.selectUserNameById(userId);
                flowTask.setStartUserId(userId);
                flowTask.setStartUserName(userName);
                flowTask.setProInsCreateTime(historicProcessInstance.getStartTime());
            }

            // workflow variable
            flowTask.setProcVars(histTask.getProcessVariables());
            Object ps = (histTask.getProcessVariables() == null) ? null
                    : histTask.getProcessVariables().get("processStatus");
            flowTask.setProcessStatus(ps == null ? null : String.valueOf(ps));

            hisTaskList.add(flowTask);
        }

        // -------------------------
        // 6) Set object
        // -------------------------
        page.setTotal(total);
        page.setRecords(hisTaskList);
        return TableDataInfo.build(page);
    }

    @Override
    public List<WfTaskVo> selectFinishedProcessList(ProcessQuery processQuery, String token) {
        SysUser sysUser = getSysUser(token);
        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables().finished().taskAssignee(TaskUtils.getUserId())
                .orderByHistoricTaskInstanceEndTime().desc();
        // Build
        ProcessUtils.buildProcessSearch(taskInstanceQuery, processQuery, processEngine, sysUser);
        List<HistoricTaskInstance> historicTaskInstanceList = taskInstanceQuery.list();
        List<WfTaskVo> hisTaskList = new ArrayList<>();
        for (HistoricTaskInstance histTask : historicTaskInstanceList) {
            WfTaskVo flowTask = new WfTaskVo();
            // current workflowinfo
            flowTask.setTaskId(histTask.getId());
            // approver info
            flowTask.setCreateTime(histTask.getCreateTime());
            flowTask.setFinishTime(histTask.getEndTime());
            flowTask.setDuration(DateUtil.formatBetween(histTask.getDurationInMillis(), BetweenFormatter.Level.SECOND));
            flowTask.setProcDefId(histTask.getProcessDefinitionId());
            flowTask.setTaskDefKey(histTask.getTaskDefinitionKey());
            flowTask.setTaskName(histTask.getName());

            // workflow definitioninfo
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId())
                    .processDefinitionId(histTask.getProcessDefinitionId()).singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setProcInsId(histTask.getProcessInstanceId());
            flowTask.setHisProcInsId(histTask.getProcessInstanceId());

            // workflow info
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(histTask.getProcessInstanceId())
                    .singleResult();
            String userId = historicProcessInstance.getStartUserId();
            String userName = userService.selectUserNameById(userId);
            flowTask.setStartUserId(userId);
            flowTask.setStartUserName(userName);

            // workflow variable
            flowTask.setProcVars(histTask.getProcessVariables());

            hisTaskList.add(flowTask);
        }
        return hisTaskList;
    }

    @Override
    public Object selectFormContent(String definitionId, String deployId, String procInsId) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        BpmnModel bpmnModel = repositoryService.getBpmnModel(definitionId);

        if (ObjectUtil.isNull(bpmnModel)) {
            throw new RuntimeException("获取流程设计失败！");
        }

        StartEvent startEvent = ModelUtils.getStartEvent(bpmnModel);
        WfDeployForm deployForm = deployFormMapper.selectOne(new LambdaQueryWrapper<WfDeployForm>()
                .eq(WfDeployForm::getDeployId, deployId)
                .eq(WfDeployForm::getFormKey, startEvent.getFormKey())
                .eq(WfDeployForm::getNodeKey, startEvent.getId()));

        if (ObjectUtil.isNull(deployForm) || StringUtils.isEmpty(deployForm.getContent())) {
            throw new RuntimeException("流程表单内容缺失！");
        }

        // Check form Parse
        FormConf formConf = JsonUtils.parseObject(deployForm.getContent(), FormConf.class);
        boolean isOldForm = (ObjectUtil.isNotNull(formConf) && StringUtils.isNotEmpty(formConf.getFormRef()));

        WFormInfo wFormInfo = isOldForm ? null : JsonUtils.parseObject(deployForm.getContent(), WFormInfo.class);

        if (!isOldForm && (ObjectUtil.isNull(wFormInfo) || ObjectUtil.isNull(wFormInfo.getFormConfig()))) {
            throw new RuntimeException("获取流程表单失败！");
        }

        // in workflow instance ID, fill formdata
        if (ObjectUtil.isNotEmpty(procInsId)) {
            HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(procInsId)
                    .includeProcessVariables().singleResult();
            if (historicProcIns != null) {
                Map<String, Object> processVariables = historicProcIns.getProcessVariables();
                if (isOldForm) {
                    ProcessFormUtils.fillFormData(formConf, processVariables);
                } else {
                    WProcessFormUtils.fillFormData(wFormInfo, processVariables);
                }
            }
        }

        return isOldForm ? formConf : wFormInfo;
    }

    /**
     * workflow definition ID workflow instance
     *
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcessByDefId(ProcessStartBo processStartBo, SysUser sysUser) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId())
                    .processDefinitionId(processStartBo.getProcessDefId()).latestVersion().singleResult();
            // null / empty value
            if (processDefinition == null) {
                throw new ServiceException("未找到指定的流程定义，流程定义ID：" + processStartBo.getProcessDefId());
            }

            return startProcess(processDefinition, processStartBo, sysUser);
        } catch (ServiceException e) {
            // new already
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("流程启动错误: " + e.getMessage());
        }
    }

    // /**
    // * workflow instance
    // */
    // private void startProcess(ProcessDefinition procDef, Map<String, Object>
    // variables) {
    // if (ObjectUtil.isNotNull(procDef) && procDef.isSuspended()) {
    // throw new ServiceException("workflow already , workflow");
    // }
    // // Set workflow Id workflow in
    // String userIdStr = TaskUtils.getUserId();
    // identityService.setAuthenticatedUserId(userIdStr);
    // variables.put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR, userIdStr);
    // // Set workflow to in
    // variables.put(ProcessConstants.PROCESS_STATUS_KEY,
    // ProcessStatus.RUNNING.getStatus());
    // // workflow instance
    // ProcessInstance processInstance =
    // runtimeService.startProcessInstanceByKeyAndTenantId(procDef.getId(),,
    // variables);
    // ProcessInstanceBuilder builder =
    // runtimeService.createProcessInstanceBuilder();
    // // usertask to , task
    // wfTaskService.startFirstTask(processInstance, variables);
    // }

    // Parse Replace workflow variable value method
    // public static String resolveExpression(String expression,
    // HistoricProcessInstance historicProcIns) {
    // if (expression == null || expression.isEmpty() || historicProcIns == null) {
    // return expression;
    // }
    //
    // // , UUID
    // String[] tokens = splitExpressionSafely(expression);
    //
    // StringBuilder resolvedExpression = new StringBuilder();
    //
    // for (String token : tokens) {
    // if (isVariableToken(token)) {
    // String resolvedValue = processBooleanParameter(token, historicProcIns);
    // resolvedExpression.append(resolvedValue);
    // } else {
    // resolvedExpression.append(token);
    // }
    // }
    //
    // return resolvedExpression.toString();
    // }
    //
    // // after method , UUID
    // private static String[] splitExpressionSafely(String expression) {
    // return
    // expression.split("(?<![0-9a-fA-F\\-])(?<=\\W)(?=\\w)|(?<=\\w)(?=\\W)(?![0-9a-fA-F\\-])");
    // }
    //
    // // Check whether is variable ( : processDefId, initiator, notifyAllSteps)
    // private static boolean isVariableToken(String token) {
    // return token.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
    // || isUUID(token);
    // }
    //
    // // Process workflow variable value
    // private static String processBooleanParameter(String param,
    // HistoricProcessInstance historicProcIns) {
    // Map<String, Object> processVariables = historicProcIns.getProcessVariables();
    // Object o = processVariables.get(param);
    //
    // String valueStr = o != null ? o.toString() : param;
    //
    // if ("1".equals(valueStr)) {
    // return "true";
    // } else if ("0".equals(valueStr)) {
    // return "false";
    // } else {
    // if (isUUID(valueStr)) {
    // return "'" + valueStr + "'";
    // }
    // return "'" + valueStr.replaceAll("'", "\\'") + "'";
    // }
    // }
    //
    // // Check whether to UUID
    // private static boolean isUUID(String value) {
    // return
    // value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    // }

    @Override
    public String getTaskId(String procInsId, SysUser sysUser) {
        try {
            return taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).processInstanceId(procInsId)
                    .orderByTaskCreateTime().desc().list().stream().map(task -> task.getId())
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            logger.debug("最后一个节点没有下一个任务");
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessByIds(String[] instanceIds) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        // RedisUtilsn.getCacheObject();
        List<String> ids = Arrays.asList(instanceIds);
        // Validate workflowwhether finish
        long activeInsCount = runtimeService.createProcessInstanceQuery().processInstanceTenantId(sysUser.getTenantId())
                .processInstanceIds(new HashSet<>(ids)).active().count();
        if (activeInsCount > 0) {
            throw new ServiceException("不允许删除进行中的流程实例");
        }
        // Delete history workflow instance
        historyService.bulkDeleteHistoricProcessInstances(ids);
    }

    /**
     * xml
     *
     * @param processDefId workflow definition ID
     */
    @Override
    public String queryBpmnXmlById(String processDefId) {
        InputStream inputStream = repositoryService.getProcessModel(processDefId);
        try {
            return IoUtil.readUtf8(inputStream);
        } catch (IORuntimeException exception) {
            throw new RuntimeException("加载xml文件异常");
        }
    }

    @Override
    public String queryBpmnJsonById(String processDefId, SysUser sysUser) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefId).processDefinitionTenantId(sysUser.getTenantId()).singleResult();
        if (processDefinition == null) {
            throw new ServiceException("流程定义不存在");
        }

        // : deploymentId Query Model
        String deploymentId = processDefinition.getDeploymentId();
        Model model = repositoryService.createModelQuery().deploymentId(deploymentId).singleResult();

        // : if model to null, modelKey and tenantId Query new model
        if (model == null) {

            model = repositoryService.createModelQuery()
                    .modelTenantId(sysUser.getTenantId())
                    .modelKey(processDefinition.getKey())
                    .latestVersion()
                    .singleResult();

            if (model != null) {
                log.info("通过modelKey成功获取到Model，modelId={}, modelKey={}",
                        model.getId(), model.getKey());
            } else {
                // if ,
                throw new ServiceException("流程定义未关联流程模型，processDefId=" + processDefId
                        + ", processKey=" + processDefinition.getKey());
            }
        }

        ReModelJsonVo reModelJsonVo = reModeJsonService.queryById(model.getId());
        return reModelJsonVo.getJsonContent();
    }

    /**
     * workflow info
     *
     * @param procInsId workflow instance ID
     * @param taskIds taskID
     * @param sysUser
     * @param includeApproverIds whether need to all approverID
     * @return
     */
    @Override
    public WfDetailVo queryProcessDetail(String procInsId, String taskIds, SysUser sysUser,
            Boolean includeApproverIds) {
        WfDetailVo detailVo = new WfDetailVo();
        // Get workflow instance
        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(procInsId).includeProcessVariables()
                .singleResult();
        String taskId = org.apache.commons.lang3.StringUtils.substringBefore(taskIds, ",");
        if (StringUtils.isNotBlank(taskId)) {
            List<HistoricTaskInstance> task = historyService.createHistoricTaskInstanceQuery()
                    .taskId(taskId)
                    .includeIdentityLinks()
                    .includeProcessVariables()
                    .list();
            if (!ObjectUtil.isNotEmpty(task)) {
                log.error("没有可办理的任务！");
            }
            detailVo.setTaskFormData(currTaskFormData(historicProcIns.getDeploymentId(), task));
        }
        String bpmnJson = queryBpmnJsonById(historicProcIns.getProcessDefinitionId(), sysUser);
        // Get Bpmnmodelinfo
        InputStream inputStream = repositoryService.getProcessModel(historicProcIns.getProcessDefinitionId());
        String bpmnXmlStr = StrUtil.utf8Str(IoUtil.readBytes(inputStream, false));
        BpmnModel bpmnModel = ModelUtils.getBpmnModel(bpmnXmlStr);
        detailVo.setBpmnXml(bpmnXmlStr);
        detailVo.setBpmnJson(bpmnJson);
        detailVo.setHistoryProcNodeList(historyProcNodeList(historicProcIns, sysUser));
        detailVo.setProcessFormList(processFormList(bpmnModel, historicProcIns));
        detailVo.setFlowViewer(getFlowViewer(bpmnModel, procInsId));
        detailVo.setWfBasicInfoVo(getProcessBasicInfo(historicProcIns, sysUser));
        // Get current node all extension properties
        if (StringUtils.isNotBlank(taskId) && historicProcIns.getEndTime() == null) {
            Map<String, String> extensionMap = new HashMap<>();
            Map<String, String> buttonsMap = new HashMap<>();
            Task task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId()).taskId(taskId).singleResult();
            if (ObjectUtil.isNotNull(task)) {
                // Get task
                FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
                if (flowElement instanceof UserTask) {
                    UserTask userTask = (UserTask) flowElement;
                    // Get usertask element
                    List<ExtensionElement> extensionElements = userTask.getExtensionElements().get("properties");
                    if (extensionElements != null && !extensionElements.isEmpty()) {
                        for (ExtensionElement extensionElement : extensionElements) {
                            // <flowable:properties> element
                            // Get sub element <flowable:property>
                            List<ExtensionElement> propertyElements = extensionElement.getChildElements()
                                    .get("property");
                            // <flowable:property> element
                            if (propertyElements != null && !propertyElements.isEmpty()) {
                                for (ExtensionElement propertyElement : propertyElements) {
                                    // Get property and value
                                    String propertyName = propertyElement.getAttributeValue(null, "name");
                                    String propertyValue = propertyElement.getAttributeValue(null, "value");
                                    extensionMap.put(propertyName, propertyValue);
                                }
                            }
                        }
                    }
                }
                if (flowElement instanceof UserTask) {
                    UserTask userTask = (UserTask) flowElement;
                    // Get usertask element
                    List<ExtensionElement> extensionElements = userTask.getExtensionElements().get("propertiesBtn");
                    if (extensionElements != null && !extensionElements.isEmpty()) {
                        for (ExtensionElement extensionElement : extensionElements) {
                            // <flowable:properties> element
                            // Get sub element <flowable:property>
                            List<ExtensionElement> propertyElements = extensionElement.getChildElements()
                                    .get("property");
                            // <flowable:property> element
                            if (propertyElements != null && !propertyElements.isEmpty()) {
                                String propertyName = null;
                                String propertyValue = null;
                                for (ExtensionElement propertyElement : propertyElements) {
                                    // Get property and value
                                    propertyName = propertyElement.getAttributeValue(null, "name");
                                    propertyValue = propertyElement.getAttributeValue(null, "value");
                                    buttonsMap.put(propertyName, propertyValue);
                                }
                                // only current node assignee button
                                if (StringUtils.isNotBlank(task.getAssignee())
                                        && !task.getAssignee().equals(sysUser.getUserId())) {
                                    propertyValue = propertyValue.replaceAll("0", "");
                                }
                                // Process nodebutton
                                WorkOrder one = workOrderService
                                        .getOne(new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getProcInsId,
                                                procInsId));
                                if (ObjectUtil.isNotNull(one)) {
                                    propertyValue = (StringUtils.isNotBlank(propertyValue)) ? propertyValue : "";
                                    if (one.getWorkorderStatus().equals(WorkOrderStatus.PENDING_ORDERS.getStatus())) {
                                        // Process null value
                                        propertyValue = propertyValue.replaceAll("[70]", "");
                                    } else {
                                        propertyValue = (StringUtils.isNotBlank(propertyValue))
                                                ? propertyValue.replaceAll("6", "")
                                                : "";
                                    }
                                    // if
                                    // (one.getWorkorderStatus().equals(WorkOrderStatus.PENDING_DISPATCH.getStatus()))
                                    // {
                                    // propertyValue = propertyValue.replaceAll("7", "");
                                    // }
                                    buttonsMap.put(propertyName, propertyValue);
                                }
                            }
                        }
                    }
                }
                detailVo.setExtensionMap(extensionMap);
                detailVo.setButtonsMap(buttonsMap);
            }
        }
        // if need to all approverID
        if (Boolean.TRUE.equals(includeApproverIds)) {
            List<String> approverIds = new ArrayList<>();
            // Get workflow all history usertask
            List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(procInsId)
                    .activityType(BpmnXMLConstants.ELEMENT_TASK_USER)
                    .list();
            for (HistoricActivityInstance activityInstance : activityInstances) {
                // Get task approver
                if (StringUtils.isNotBlank(activityInstance.getAssignee())) {
                    if (!approverIds.contains(activityInstance.getAssignee())) {
                        approverIds.add(activityInstance.getAssignee());
                    }
                }
                // Get task candidate user
                List<HistoricIdentityLink> identityLinks = historyService
                        .getHistoricIdentityLinksForTask(activityInstance.getTaskId());
                for (HistoricIdentityLink identityLink : identityLinks) {
                    if ("candidate".equals(identityLink.getType())
                            && StringUtils.isNotBlank(identityLink.getUserId())) {
                        if (!approverIds.contains(identityLink.getUserId())) {
                            approverIds.add(identityLink.getUserId());
                        }
                    }
                }
            }
            // Get current task candidate user
            List<Task> currentTasks = taskService.createTaskQuery()
                    .processInstanceId(procInsId)
                    .taskTenantId(sysUser.getTenantId())
                    .list();
            for (Task currentTask : currentTasks) {
                // current task approver
                if (StringUtils.isNotBlank(currentTask.getAssignee())) {
                    if (!approverIds.contains(currentTask.getAssignee())) {
                        approverIds.add(currentTask.getAssignee());
                    }
                }
                // current task candidate user
                List<IdentityLink> taskIdentityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                for (IdentityLink identityLink : taskIdentityLinks) {
                    if ("candidate".equals(identityLink.getType())
                            && StringUtils.isNotBlank(identityLink.getUserId())) {
                        if (!approverIds.contains(identityLink.getUserId())) {
                            approverIds.add(identityLink.getUserId());
                        }
                    }
                }
            }
            detailVo.setApproverIds(approverIds);
        }
        // workflow log
        ProcessViewLogBo processViewLog = new ProcessViewLogBo();
        processViewLog.setProcessInstanceId(historicProcIns.getId());
        processViewLog.setProcessKey(historicProcIns.getProcessDefinitionKey());
        processViewLog.setOperationType("查看");
        processViewLog.setProcessStatus(
                (String) historicProcIns.getProcessVariables().get(ProcessConstants.PROCESS_STATUS_KEY));
        processViewLog.setViewTime(new Date());
        processViewLogService.insertByBo(processViewLog, sysUser);
        return detailVo;
    }

    private WfBasicInfoVo getProcessBasicInfo(HistoricProcessInstance historicProcIns, SysUser sysUser) {
        WfBasicInfoVo basicInfo = new WfBasicInfoVo();
        if (historicProcIns != null) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionTenantId(sysUser.getTenantId())
                    .processDefinitionId(historicProcIns.getProcessDefinitionId()).singleResult();
            List<Task> taskList = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(historicProcIns.getId()).orderByTaskCreateTime().desc().list();
            taskList.stream().forEach(task -> {
                basicInfo.setProcessCategory(processDefinition.getCategory());
                basicInfo.setProcessName(processDefinition.getName());
                basicInfo.setProcessId(historicProcIns.getId());
                basicInfo.setSubmissionTime(historicProcIns.getStartTime());
                if (ObjectUtil.isNotNull(task)) {
                    basicInfo.setTaskDefId(task.getTaskDefinitionKey());
                }
            });
        }
        return basicInfo;
    }

    /**
     * workflow instance
     */
    private String startProcess(ProcessDefinition procDef, ProcessStartBo processStartBo, SysUser sysUser) {
        if (ObjectUtil.isNotNull(procDef) && procDef.isSuspended()) {
            throw new ServiceException("流程已被挂起，请先激活流程");
        }

        // null / empty value
        if (procDef == null) {
            throw new ServiceException("流程定义不能为空");
        }

        if (processStartBo == null) {
            throw new ServiceException("流程启动参数不能为空");
        }

        if (sysUser == null) {
            throw new ServiceException("用户信息不能为空");
        }

        // String userIdStr = TaskUtils.getUserId();
        identityService.setAuthenticatedUserId(String.valueOf(sysUser.getUserId()));
        // Set workflow id
        processStartBo.getVariables().put(BpmnXMLConstants.ATTRIBUTE_EVENT_START_INITIATOR,
                String.valueOf(sysUser.getUserId()));
        // Set workflow to in
        processStartBo.getVariables().put(ProcessConstants.PROCESS_STATUS_KEY, ProcessStatus.RUNNING.getStatus());

        // null / empty
        if (processStartBo.getVariables() == null) {
            processStartBo.setVariables(new HashMap<>());
        }

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String applicationId = httpRequest.getHeader("Appid");
        processStartBo.getVariables().put("appid", applicationId);
        // if Get formfield
        if (processStartBo.isAutoGetFormFlag()) {
            log.info("开始处理表单字段映射，processDefId={}", procDef.getId());
            Map<String, Object> mappedVariables = processFormVariables(procDef, processStartBo.getVariables(), sysUser);
            processStartBo.setVariables(mappedVariables);
            log.info("表单字段映射完成");
        }
        // workflow instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId(procDef.getKey(),
                processStartBo.getVariables(), sysUser.getTenantId());

        // null / empty value
        if (processInstance == null) {
            throw new ServiceException("流程实例创建失败");
        }

        runtimeService.setProcessInstanceName(processInstance.getId(), procDef.getName());

        // ProcessInstanceBuilder
        // builderce=runtimeService.createProcessInstanceBuilder().processDefinitionId
        // (procDef.getId()).
        // tenantId(sysUser.getTenantId()).variables(variables);
        // page approver, page approver
        // Get workflow definition ID
        String processDefinitionId = procDef.getId();
        // RepositoryService Get BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        // null / empty value
        if (bpmnModel == null) {
            throw new ServiceException("未能获取到流程模型信息");
        }

        Task currentTask = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                .processInstanceId(processInstance.getId()).active().singleResult();

        // null / empty value
        if (currentTask == null) {
            throw new ServiceException("未能获取到当前任务信息");
        }

        currentTask.getId();
        // Get workflow definition main workflow object
        Process process = bpmnModel.getMainProcess();

        // null / empty value
        if (process == null) {
            throw new ServiceException("未能获取到主流程信息");
        }

        // Get Customproperty(http://flowable.org/bpmn to null / empty userGet Customproperty)
        String notifyAllStepsStr = process.getAttributeValue("http://flowable.org/bpmn", "notifyAllSteps");
        // Convert to value ( to "true" to true, to false)
        Boolean notifyAllSteps = "true".equalsIgnoreCase(notifyAllStepsStr);
        // workflow variable in ,
        runtimeService.setVariable(currentTask.getExecutionId(), "notifyAllSteps", notifyAllSteps);
        String nextUserIds = (String) processStartBo.getVariables().get("nextUserIds");
        if (StringUtils.isNotBlank(nextUserIds)) {
            // Parse nextUserIds , assuming is user ID
            String[] userIdArray = nextUserIds.split(",");
            List<String> candidateUserIds = Arrays.asList(userIdArray);
            // Get new node
            List<Task> task = taskService.createTaskQuery().taskTenantId(sysUser.getTenantId())
                    .processInstanceId(processInstance.getId()).active().list();
            // Check current task whether to workflow
            if (task != null && !task.isEmpty() && task.get(0) != null) {
                // new user
                for (String userId : candidateUserIds) {
                    taskService.addCandidateUser(task.get(0).getId(), userId);
                }
            }
            // wfTaskService.sendMessage(notifyAllSteps, nextUserIds);
            // wfTaskService.buildAndSendUnifiedMessage(task.get(0), nextUserIds, true,
            // sysUser);
        }
        HistoricProcessInstance historicProcIns = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(currentTask.getProcessInstanceId())
                .includeProcessVariables().singleResult();
        // in workflow method in
        for (FlowElement flowElement : process.getFlowElements()) {
            if (flowElement instanceof SequenceFlow) {
                SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                Map<String, List<ExtensionElement>> extensionElements = sequenceFlow.getExtensionElements();
                if (extensionElements != null && extensionElements.containsKey("expression")) {
                    ExtensionElement expressionElement = extensionElements.get("expression").get(0);
                    String expression = expressionElement.getElementText();
                    try {
                        // Parse ( )
                        //
                        AviatorEvaluatorInstance engine = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);
                        // Execute
                        engine.setOption(Options.TRACE_EVAL, true);
                        String s = resolveExpression(expression, historicProcIns);
                        log.info("表达式：{}", s);
                        Expression exp = engine.compile(s);
                        boolean execute = (Boolean) exp.execute(exp.newEnv());
                        // workflow variable
                        runtimeService.setVariable(processInstance.getId(), sequenceFlow.getId() + "expression",
                                execute);
                    } catch (Exception e) {
                        throw new ServiceException("条件表达式存在非法的值: " + e.getMessage());
                    }
                }
            }
        }
        // usertask to , task
        wfTaskService.startFirstTask(processInstance, processStartBo.getVariables(), sysUser);
        if (ObjectUtil.isNotNull(processStartBo.getJob())) {
            // work order
            // processStartBo.getWorkOrderBo().setWorkorderId(currentTask.getCategory());
            // BeanUtil.toBean(processStartBo.getWorkOrderBo(), WorkOrderBo.class);
            processStartBo.getWorkOrderBo().setTaskId(getTaskId(processInstance.getId(), sysUser));
            long nanoTime = System.nanoTime(); // Get current
            int random = new Random().nextInt(9999); // ,
            String id = Long.toHexString(nanoTime) + Integer.toHexString(random);
            processStartBo.getWorkOrderBo().setWorkOrderJobSerial(id.substring(0, Math.min(10, id.length())));
            processStartBo.getWorkOrderBo().setWorkOrderJobFlag("1");
            processStartBo.getWorkOrderBo().setProcessKey(processInstance.getProcessDefinitionKey());
            workOrderService.insertByBo(processStartBo.getWorkOrderBo(), sysUser);
            String Cycle = ConvertCronUtils.convertToCron(processStartBo.getJob());
            // null / empty loopparameter
            processStartBo.setJob(null);
            Map<String, Object> jobProperties = new HashMap();
            jobProperties.put("processStartBo", processStartBo);
            jobProperties.put("sysUser", sysUser);
            jobProperties.put("workOrderJobFlag", 1);
            CommonJobInfo commonJobInfo = new CommonJobInfo();
            commonJobInfo.setJobDesc(procDef.getName());
            commonJobInfo.setAuthor(sysUser.getUserName());
            commonJobInfo.setScheduleType("CRON");
            commonJobInfo.setScheduleConf(Cycle);
            commonJobInfo.setCronGenDisplay(Cycle);
            commonJobInfo.setScheduleConfCRON(Cycle);
            commonJobInfo.setGlueType("BEAN");
            commonJobInfo.setAddTime(new Date());
            commonJobInfo.setUpdateTime(new Date());
            commonJobInfo.setExecutorHandler("createWorkOrderJob");
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String variablesString = objectMapper.writeValueAsString(jobProperties);
                commonJobInfo.setExecutorParam(variablesString);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            commonJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
            commonJobInfo.setMisfireStrategy("DO_NOTHING");
            commonJobInfo.setExecutorRouteStrategy("FIRST");
            commonJobInfo.setExecutorTimeout(600);// 10
            commonJobInfo.setExecutorFailRetryCount(3);
            commonJobInfo.setGlueRemark("GLUE代码初始化");
            xxlJobUtil.addAndStart(commonJobInfo);
        }
        if (ObjectUtil.isNotNull(processStartBo.getWorkOrderBo())) {
            processStartBo.getWorkOrderBo().setProcInsId(processInstance.getId());
            processStartBo.getWorkOrderBo().setProcessKey(processInstance.getProcessDefinitionKey());
            processStartBo.getWorkOrderBo().setTaskId(getTaskId(processInstance.getId(), sysUser));
            WorkOrder workOrder = null;
            if (processStartBo.isFrontFlag()) {
                processStartBo.getWorkOrderBo().setWorkorderStatus(WorkOrderStatus.PROCESSING.getStatus());
            }
            if (StringUtils.isNotBlank(processStartBo.getAppId())) {
                String workOrderAppId = workOrderAppService
                        .getOne(new LambdaQueryWrapper<WorkOrderApp>().eq(WorkOrderApp::getApplicationId,
                                processStartBo.getAppId()))
                        .getAppId();
                if (StringUtils.isEmpty(workOrderAppId)) {
                    throw new ServiceException("应用不存在");
                }
                processStartBo.getWorkOrderBo().setWorkorderId(workOrderAppId);
                processStartBo.getWorkOrderBo().setWorkorderIdExtend("1," + workOrderAppId);
                processStartBo.getWorkOrderBo().setTitle(processStartBo.getEventName());
                workOrder = workOrderService.insertByBo(processStartBo.getWorkOrderBo(), sysUser);
                runtimeService.setVariable(processInstance.getId(), "workorderId", workOrderAppId);
            }
            if (StringUtils.isBlank(processStartBo.getEventName())) {
                workOrder = workOrderService.insertByBo(processStartBo.getWorkOrderBo(), sysUser);
            }
            processStartBo.setWorkOrder(workOrder);
        }
        // workflow instanceid
        return processInstance.getId();
    }

    /**
     * task
     *
     * @param processDefinitionId workflow definitionid
     * @param firstTriggerTime
     */
    // public void scheduleDailyProcess(SysUser sysUser,String processDefinitionId,
    // Date firstTriggerTime,String cycle) {
    // ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl)
    // processEngineConfiguration;
    // log.info("🛠️ start task, Process : {}", CustomTimerHandler.TYPE);
    // managementService.executeCommand(commandContext -> {
    // // Get taskservice
    // TimerJobService timerJobService =
    // config.getJobServiceConfiguration().getTimerJobService();
    // // task
    // TimerJobEntity timerJob = timerJobService.createTimerJob();
    // timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
    //// timerJob.setExclusive(true);
    //
    // timerJob.setDuedate(firstTriggerTime);
    // timerJob.setJobHandlerType(CustomTimerHandler.TYPE);
    // ObjectMapper mapper = new ObjectMapper();
    //// String sysUserString = null;
    //// try {
    //// mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //// sysUserString = mapper.writeValueAsString(sysUser);
    //// } catch (JsonProcessingException e) {
    //// throw new RuntimeException(e);
    //// }
    // timerJob.setJobHandlerConfiguration("");
    // timerJob.setTenantId(sysUser.getTenantId());
    // timerJob.setProcessDefinitionId(processDefinitionId);
    //// timerJob.setRepeat(cycle);
    // // task
    // timerJobService.scheduleTimerJob(timerJob);
    // Job job =
    // managementService.createDeadLetterJobQuery().jobId(timerJob.getId()).singleResult();
    // if(ObjectUtil.isNotNull(job)){
    // managementService.moveDeadLetterJobToExecutableJob(timerJob.getId(), 3);
    // }else {
    // System.out.println("task in in , can already Execute in . 1");
    // }
    // return null;
    // });
    // }

    /**
     * Get workflow variable
     *
     * @param taskId taskID
     * @return workflow variable
     */
    private Map<String, Object> getProcessVariables(String taskId) {
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables().finished().taskId(taskId).singleResult();
        if (Objects.nonNull(historicTaskInstance)) {
            return historicTaskInstance.getProcessVariables();
        }
        return taskService.getVariables(taskId);
    }

    /**
     * Get current taskworkflowforminfo
     *
     * @param deployId workflow ID
     * @param tasks current task
     * @return each task formdata (FormConf WFormInfo)
     */
    private List<Object> currTaskFormData(String deployId, List<HistoricTaskInstance> tasks) {
        return tasks.stream()
                .map(task -> {
                    // 1. deployId + formKey + nodeKey find workflowformconfiguration
                    WfDeployFormVo deployFormVo = deployFormMapper.selectVoOne(
                            new LambdaQueryWrapper<WfDeployForm>()
                                    .eq(WfDeployForm::getDeployId, deployId)
                                    .eq(WfDeployForm::getFormKey, task.getFormKey())
                                    .eq(WfDeployForm::getNodeKey, task.getTaskDefinitionKey()));

                    // 2. if configuration, node
                    if (deployFormVo == null) {
                        return null;
                    }

                    // 3. Parse to FormConf, whether is " form" ( formRef Check )
                    FormConf formConf = JsonUtils.parseObject(deployFormVo.getContent(), FormConf.class);
                    boolean isOldForm = (formConf != null && StringUtils.isNotEmpty(formConf.getFormRef()));

                    // 4. if is form, Parse to new formobject WFormInfo
                    WFormInfo wFormInfo = isOldForm ? null
                            : JsonUtils.parseObject(deployFormVo.getContent(),
                                    WFormInfo.class);

                    // 5. if new formParse afterdata ,
                    if (!isOldForm && (wFormInfo == null || wFormInfo.getFormConfig() == null)) {
                        throw new RuntimeException(
                                "获取流程表单失败，请检测当前节点表单是否设计完成！");
                    }

                    // 6. fill formdata, and button
                    if (isOldForm) {
                        ProcessFormUtils.fillFormData(formConf, task.getTaskLocalVariables());
                        formConf.setFormBtns(false);
                        return formConf;
                    } else {
                        WProcessFormUtils.fillFormData(wFormInfo, task.getTaskLocalVariables());
                        wFormInfo.getFormConfig().setFormBtns(false);
                        return wFormInfo;
                    }
                })
                // 7. map null
                .filter(Objects::nonNull)
                // 8. List<Object>
                .collect(Collectors.toList());
    }

    /**
     * Get history workflowforminfo
     */
    public List<Object> processFormList(BpmnModel bpmnModel, HistoricProcessInstance historicProcIns) {
        List<Object> procFormList = new ArrayList<>();

        List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(historicProcIns.getId()).finished()
                .activityTypes(CollUtil.newHashSet(BpmnXMLConstants.ELEMENT_EVENT_START,
                        BpmnXMLConstants.ELEMENT_TASK_USER))
                .orderByHistoricActivityInstanceStartTime().asc().list();
        List<String> processFormKeys = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : activityInstanceList) {
            try {
                // Get current nodeworkflowelementinfo
                FlowElement flowElement = ModelUtils.getFlowElementById(bpmnModel, activityInstance.getActivityId());
                // if flowElement to null, node
                if (flowElement == null) {
                    log.warn("流程节点元素不存在，activityId: {}, processInstanceId: {}",
                            activityInstance.getActivityId(), historicProcIns.getId());
                    continue;
                }

                // Get current nodeformKey
                String formKey = ModelUtils.getFormKey(flowElement);
                if (formKey == null || StringUtils.isEmpty(formKey)) {
                    continue;
                }

                boolean localScope = Convert.toBool(ModelUtils.getElementAttributeValue(flowElement,
                        ProcessConstants.PROCESS_FORM_LOCAL_SCOPE), false);
                Map<String, Object> variables;

                if (localScope) {
                    // localScope, need to Query tasknodeparameter
                    // : START_EVENT instance can taskId
                    String taskId = activityInstance.getTaskId();
                    if (StringUtils.isBlank(taskId)) {
                        // if is START_EVENT taskId, workflow variable
                        log.debug("活动实例没有taskId，使用流程变量，activityId: {}, activityType: {}",
                                activityInstance.getActivityId(), activityInstance.getActivityType());
                        variables = historicProcIns.getProcessVariables() != null
                                ? historicProcIns.getProcessVariables()
                                : new HashMap<>();
                    } else {
                        // Query tasknodeparameter, Convert Map
                        List<HistoricVariableInstance> variableInstances = historyService
                                .createHistoricVariableInstanceQuery()
                                .processInstanceId(historicProcIns.getId())
                                .taskId(taskId)
                                .list();
                        if (CollUtil.isNotEmpty(variableInstances)) {
                            variables = variableInstances.stream()
                                    .collect(Collectors.toMap(
                                            HistoricVariableInstance::getVariableName,
                                            HistoricVariableInstance::getValue,
                                            (existing, replacement) -> existing));
                        } else {
                            // if Query taskvariable, workflow variable
                            variables = historicProcIns.getProcessVariables() != null
                                    ? historicProcIns.getProcessVariables()
                                    : new HashMap<>();
                        }
                    }
                } else {
                    if (processFormKeys.contains(formKey)) {
                        continue;
                    }
                    variables = historicProcIns.getProcessVariables() != null ? historicProcIns.getProcessVariables()
                            : new HashMap<>();
                    processFormKeys.add(formKey);
                }

                // non-nodeform Query can , only Get info
                List<WfDeployFormVo> formInfoList = deployFormMapper.selectVoList(new LambdaQueryWrapper<WfDeployForm>()
                        .eq(WfDeployForm::getDeployId, historicProcIns.getDeploymentId())
                        .eq(WfDeployForm::getFormKey, formKey)
                        .eq(localScope, WfDeployForm::getNodeKey, flowElement.getId()));

                // @update by Brath: null / empty collection NULL null / empty
                WfDeployFormVo formInfo = formInfoList.stream().findFirst().orElse(null);

                if (ObjectUtil.isNotNull(formInfo)) {
                    // form whether is empty
                    if (StringUtils.isEmpty(formInfo.getContent())) {
                        log.warn("表单内容为空，formKey: {}, nodeKey: {}, deployId: {}",
                                formKey, flowElement.getId(), historicProcIns.getDeploymentId());
                        continue;
                    }

                    // old data formInfo.getFormName() to null
                    String formName = Optional.ofNullable(formInfo.getFormName()).orElse(StringUtils.EMPTY);
                    String elementName = flowElement.getName() != null ? flowElement.getName() : "";
                    String title = localScope ? formName.concat("(" + elementName + ")") : formName;

                    // Check form Parse
                    FormConf formConf = null;
                    try {
                        formConf = JsonUtils.parseObject(formInfo.getContent(), FormConf.class);
                    } catch (Exception e) {
                        log.warn("解析FormConf失败，formKey: {}, error: {}", formKey, e.getMessage());
                    }

                    boolean isOldForm = (ObjectUtil.isNotNull(formConf)
                            && StringUtils.isNotEmpty(formConf.getFormRef()));

                    WFormInfo wFormInfo = null;
                    if (!isOldForm) {
                        try {
                            wFormInfo = JsonUtils.parseObject(formInfo.getContent(), WFormInfo.class);
                        } catch (Exception e) {
                            log.warn("解析WFormInfo失败，formKey: {}, error: {}", formKey, e.getMessage());
                        }
                    }

                    if (!isOldForm && (ObjectUtil.isNull(wFormInfo) || ObjectUtil.isNull(wFormInfo.getFormConfig()))) {
                        log.error("获取流程表单失败，formKey: {}, deployId: {}, content: {}",
                                formKey, historicProcIns.getDeploymentId(),
                                formInfo.getContent() != null && formInfo.getContent().length() > 100
                                        ? formInfo.getContent().substring(0, 100) + "..."
                                        : formInfo.getContent());
                        continue; // to continue is throw, form Get
                    }

                    if (isOldForm) {
                        if (formConf != null) {
                            formConf.setTitle(title);
                            formConf.setDisabled(true);
                            formConf.setFormBtns(false);
                            if (variables != null) {
                                ProcessFormUtils.fillFormData(formConf, variables);
                            }
                            procFormList.add(formConf);
                        } else {
                            log.warn("formConf为null，无法添加到表单列表，formKey: {}", formKey);
                        }
                    } else {
                        if (wFormInfo != null) {
                            if (wFormInfo.getFormConfig() != null) {
                                wFormInfo.getFormConfig().setTitle(title);
                                wFormInfo.getFormConfig().setDisabled(true);
                                wFormInfo.getFormConfig().setFormBtns(false);
                            }
                            if (variables != null) {
                                WProcessFormUtils.fillFormData(wFormInfo, variables);
                            }
                            procFormList.add(wFormInfo);
                        } else {
                            log.warn("wFormInfo为null，无法添加到表单列表，formKey: {}", formKey);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("处理流程表单时发生异常，activityId: {}, processInstanceId: {}, error: {}",
                        activityInstance.getActivityId(), historicProcIns.getId(), e.getMessage(), e);
                // Process node, in workflow
            }
        }
        return procFormList;
    }

    @Deprecated
    private void buildStartFormData(HistoricProcessInstance historicProcIns, Process process, String deployId,
            List<FormConf> procFormList) {
        procFormList = procFormList == null ? new ArrayList<>() : procFormList;
        HistoricActivityInstance startInstance = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(historicProcIns.getId()).activityId(historicProcIns.getStartActivityId())
                .singleResult();
        StartEvent startEvent = (StartEvent) process.getFlowElement(startInstance.getActivityId());
        WfDeployFormVo startFormInfo = deployFormMapper
                .selectVoOne(new LambdaQueryWrapper<WfDeployForm>().eq(WfDeployForm::getDeployId,
                        deployId).eq(WfDeployForm::getFormKey, startEvent.getFormKey()).eq(WfDeployForm::getNodeKey,
                                startEvent.getId()));
        if (ObjectUtil.isNotNull(startFormInfo)) {
            FormConf formConf = JsonUtils.parseObject(startFormInfo.getContent(), FormConf.class);
            if (null != formConf) {
                formConf.setTitle(startEvent.getName());
                formConf.setDisabled(true);
                formConf.setFormBtns(false);
                ProcessFormUtils.fillFormData(formConf, historicProcIns.getProcessVariables());
                procFormList.add(formConf);
            }
        }
    }

    @Deprecated
    private void buildUserTaskFormData(String procInsId, String deployId, Process process,
            List<FormConf> procFormList) {
        procFormList = procFormList == null ? new ArrayList<>() : procFormList;
        List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInsId).finished().activityType(BpmnXMLConstants.ELEMENT_TASK_USER)
                .orderByHistoricActivityInstanceStartTime().asc().list();
        for (HistoricActivityInstance instanceItem : activityInstanceList) {
            UserTask userTask = (UserTask) process.getFlowElement(instanceItem.getActivityId(), true);
            String formKey = userTask.getFormKey();
            if (formKey == null) {
                continue;
            }
            // Query tasknodeparameter, Convert Map
            Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(procInsId).taskId(instanceItem.getTaskId()).list().stream().collect(Collectors
                            .toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));
            WfDeployFormVo deployFormVo = deployFormMapper.selectVoOne(new LambdaQueryWrapper<WfDeployForm>()
                    .eq(WfDeployForm::getDeployId,
                            deployId)
                    .eq(WfDeployForm::getFormKey, formKey).eq(WfDeployForm::getNodeKey, userTask.getId()));
            if (ObjectUtil.isNotNull(deployFormVo)) {
                FormConf formConf = JsonUtils.parseObject(deployFormVo.getContent(), FormConf.class);
                if (null != formConf) {
                    formConf.setTitle(userTask.getName());
                    formConf.setDisabled(true);
                    formConf.setFormBtns(false);
                    ProcessFormUtils.fillFormData(formConf, variables);
                    procFormList.add(formConf);
                }
            }
        }
    }

    /**
     * Get history taskinfo
     */
    public List<WfProcNodeVo> historyProcNodeList(HistoricProcessInstance historicProcIns, SysUser sysUser) {
        String procInsId = historicProcIns.getId();
        List<HistoricActivityInstance> historicActivityInstanceList = historyService
                .createHistoricActivityInstanceQuery().processInstanceId(procInsId)
                .activityTypes(CollUtil.newHashSet(BpmnXMLConstants.ELEMENT_EVENT_START,
                        BpmnXMLConstants.ELEMENT_EVENT_END, BpmnXMLConstants.ELEMENT_TASK_USER))
                .orderByHistoricActivityInstanceStartTime().desc().orderByHistoricActivityInstanceEndTime().desc()
                .list();

        List<Comment> commentList = taskService.getProcessInstanceComments(procInsId);

        List<WfProcNodeVo> elementVoList = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
            WfProcNodeVo elementVo = new WfProcNodeVo();
            elementVo.setProcDefId(activityInstance.getProcessDefinitionId());
            elementVo.setActivityId(activityInstance.getActivityId());
            elementVo.setActivityName(activityInstance.getActivityName());
            elementVo.setActivityType(activityInstance.getActivityType());
            elementVo.setCreateTime(activityInstance.getStartTime());
            elementVo.setEndTime(activityInstance.getEndTime());
            elementVo.setTransactionOrder(activityInstance.getTransactionOrder());
            elementVo.setExecutionId(activityInstance.getExecutionId());
            if (ObjectUtil.isNotNull(activityInstance.getDurationInMillis())) {
                elementVo.setDuration(DateUtil.formatBetween(activityInstance.getDurationInMillis(),
                        BetweenFormatter.Level.SECOND));
            }

            if (BpmnXMLConstants.ELEMENT_EVENT_START.equals(activityInstance.getActivityType())) {
                if (ObjectUtil.isNotNull(historicProcIns)) {
                    String userId = historicProcIns.getStartUserId();
                    String userName = userService.selectUserNameById(userId);
                    if (userName != null) {
                        elementVo.setAssigneeId(userId);
                        elementVo.setAssigneeName(userName);
                    }
                }
            } else if (BpmnXMLConstants.ELEMENT_TASK_USER.equals(activityInstance.getActivityType())) {
                if (StringUtils.isNotBlank(activityInstance.getAssignee())) {
                    String userId = activityInstance.getAssignee();
                    String userName = userService.selectUserNameById(userId);
                    elementVo.setAssigneeId(userId);
                    elementVo.setAssigneeName(userName);
                }
                // approver
                List<HistoricIdentityLink> linksForTask = historyService
                        .getHistoricIdentityLinksForTask(activityInstance.getTaskId());
                StringBuilder stringBuilder = new StringBuilder();
                for (HistoricIdentityLink identityLink : linksForTask) {
                    if ("candidate".equals(identityLink.getType())) {
                        if (StringUtils.isNotBlank(identityLink.getUserId())) {
                            String userId = identityLink.getUserId();
                            String userName = userService.selectUserNameById(userId);
                            stringBuilder.append(userName).append(",");
                        }
                        if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                            if (identityLink.getGroupId().startsWith(TaskConstants.ROLE_GROUP_PREFIX)) {
                                String roleId = StringUtils.stripStart(identityLink.getGroupId(),
                                        TaskConstants.ROLE_GROUP_PREFIX);
                                SysUserRoleView role = sysRoleService.selectRoleByCondition(sysUser.getUserId(),
                                        roleId);
                                stringBuilder.append(role.getRoleName()).append(",");
                            } else if (identityLink.getGroupId().startsWith(TaskConstants.DEPT_GROUP_PREFIX)) {
                                String deptId = String.format(StringUtils.stripStart(identityLink.getGroupId(),
                                        TaskConstants.DEPT_GROUP_PREFIX));
                                SysDeptView dept = deptService.selectDeptById(deptId);
                                stringBuilder.append(dept.getDeptName()).append(",");
                            }
                        }
                    }
                }
                if (StringUtils.isNotBlank(stringBuilder)) {
                    elementVo.setCandidate(stringBuilder.substring(0, stringBuilder.length() - 1));
                }
                // Get
                if (CollUtil.isNotEmpty(commentList)) {
                    List<Comment> comments = new ArrayList<>();
                    for (Comment comment : commentList) {

                        if (StringUtils.isNotBlank(comment.getTaskId())
                                && comment.getTaskId().equals(activityInstance.getTaskId())) {
                            comments.add(comment);
                        }
                    }
                    elementVo.setCommentList(comments);
                }
                // Get info
                List<SysUser> copyUsers = new ArrayList<>();
                String taskId = activityInstance.getTaskId();
                List<String> userIds = wfCopyService.selectCopyUserIdByTaskId(taskId);
                if (!CollectionUtils.isEmpty(userIds)) {
                    for (String userId : userIds) {
                        SysUser sysUser2 = sysUserService.selectUserById(userId);
                        copyUsers.add(sysUser2);
                    }
                    elementVo.setWfCopyUser(copyUsers);
                }
            }
            elementVoList.add(elementVo);
        }
        // Query all
        Map<String, WfProcNodeVo> uniqueNodes = new HashMap<>();
        int i = 0;
        for (WfProcNodeVo node : elementVoList) {
            String activityId = node.getActivityId();
            WfProcNodeVo existingNode = uniqueNodes.get(activityId);

            if (existingNode == null || existingNode.getExecutionId().equals(node.getExecutionId())) {
                // if current ID in uniqueNodes in , new node
                WfProcNodeVo result = new WfProcNodeVo();
                BeanUtil.copyProperties(node, result);
                result.setActivityId(activityId);
                result.setAssigneeInfoList(new ArrayList<>());

                assigneeInfoVo assigneeInfoVo = new assigneeInfoVo();
                assigneeInfoVo.setAssigneeName(node.getAssigneeName());
                if (node.getEndTime() != null) {
                    assigneeInfoVo.setCompleteTime(node.getEndTime());
                }
                result.getAssigneeInfoList().add(assigneeInfoVo);

                if (node.getCommentList() != null && !node.getCommentList().isEmpty()) {
                    // if CommentList to null is empty, Set assigneeName and assigneeId
                    result.setAssigneeId(node.getAssigneeId());
                    result.setAssigneeName(node.getAssigneeName());
                }
                if (uniqueNodes.get(activityId) != null) {
                    uniqueNodes.put(activityId + ':' + i++, result);
                    // + node.getCreateTime(), result);
                } else {
                    uniqueNodes.put(activityId, result);
                }
            } else {
                // if current ID already in uniqueNodes in , assigneeName
                // existingNode.getAssigneeNameList().add(node.getAssigneeName());
                assigneeInfoVo assigneeInfoVo = new assigneeInfoVo();
                assigneeInfoVo.setAssigneeName(node.getAssigneeName());
                if (node.getEndTime() != null) {
                    assigneeInfoVo.setCompleteTime(node.getEndTime());
                }
                existingNode.getAssigneeInfoList().add(assigneeInfoVo);

                if (node.getCommentList() != null && !node.getCommentList().isEmpty()) {
                    // if CommentList to null is empty, new assigneeName and assigneeId
                    existingNode.setAssigneeId(node.getAssigneeId());
                    existingNode.setAssigneeName(node.getAssigneeName());
                }
            }
        }
        List<WfProcNodeVo> deduplicatedList = new ArrayList<>(uniqueNodes.values());
        // Comparator
        Comparator<WfProcNodeVo> comparator = Comparator.comparing((WfProcNodeVo node) -> node.getCreateTime())
                .reversed() // createTime
                .thenComparing(node -> node.getActivityType(),
                        Comparator.reverseOrder()); // createTime , activityType
        // ( userTask , reverseOrder)
        // Stream sorted method
        deduplicatedList = deduplicatedList.stream().sorted(comparator).peek(node -> {
            if (node.getAssigneeInfoList() == null || node.getAssigneeInfoList().isEmpty()) {
                List<assigneeInfoVo> assigneeNamesSingleton = new ArrayList<>();
                assigneeInfoVo assigneeInfoVo = new assigneeInfoVo();
                assigneeInfoVo.setAssigneeName(node.getAssigneeName());
                if (node.getEndTime() != null) {
                    assigneeInfoVo.setCompleteTime(node.getEndTime());
                }
                assigneeNamesSingleton.add(assigneeInfoVo);
                node.setAssigneeInfoList(assigneeNamesSingleton);
            }
            if (!CollectionUtils.isEmpty(node.getCommentList())
                    && FlowComment.STOP.getType().equals(node.getCommentList().get(0).getType())) {
                node.setActivityName("发起人");
                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceTenantId(sysUser.getTenantId()).processInstanceId(procInsId).singleResult();
                String nickName = sysUserService.selectUserById(historicProcessInstance.getStartUserId()).getUserName();
                node.setAssigneeName(nickName);
                node.getAssigneeInfoList().get(0).setAssigneeName(nickName);
            }
        }).collect(Collectors.toList());
        clearParallelMultiInstanceAssigneeName(deduplicatedList, getBpmnModel(historicProcIns));
        return deduplicatedList;
    }

    /**
     * instancenode layer assignee, assigneeInfoList .
     */
    private void clearParallelMultiInstanceAssigneeName(List<WfProcNodeVo> nodeList, BpmnModel bpmnModel) {
        if (CollectionUtils.isEmpty(nodeList) || bpmnModel == null) {
            return;
        }
        for (WfProcNodeVo node : nodeList) {
            if (isParallelMultiInstanceUserTask(bpmnModel, node.getActivityId())) {
                node.setAssigneeName(null);
            }
        }
    }

    /**
     * Check nodewhether to will .
     */
    private boolean isParallelMultiInstanceUserTask(BpmnModel bpmnModel, String activityId) {
        if (StringUtils.isBlank(activityId)) {
            return false;
        }
        FlowElement flowElement = bpmnModel.getFlowElement(activityId);
        if (!(flowElement instanceof UserTask)) {
            return false;
        }
        MultiInstanceLoopCharacteristics loopCharacteristics = ((UserTask) flowElement).getLoopCharacteristics();
        return loopCharacteristics != null && !loopCharacteristics.isSequential();
    }

    /**
     * Get workflowmodel, Get failed history nodedata .
     */
    private BpmnModel getBpmnModel(HistoricProcessInstance historicProcIns) {
        if (historicProcIns == null || StringUtils.isBlank(historicProcIns.getProcessDefinitionId())) {
            return null;
        }
        try {
            return repositoryService.getBpmnModel(historicProcIns.getProcessDefinitionId());
        } catch (Exception e) {
            log.warn("获取流程模型失败，processDefinitionId: {}", historicProcIns.getProcessDefinitionId(), e);
            return null;
        }
    }
    // /**
    // * Get history taskinfo
    // */
    // private List<WfProcNodeVo> historyProcNodeList(HistoricProcessInstance
    // historicProcIns) {
    // String procInsId = historicProcIns.getId();
    // List<HistoricActivityInstance> historicActivityInstanceList = historyService
    // .createHistoricActivityInstanceQuery()
    // .processInstanceId(procInsId)
    // .activityTypes(CollUtil.newHashSet(BpmnXMLConstants.ELEMENT_EVENT_START,
    // BpmnXMLConstants
    // .ELEMENT_EVENT_END, BpmnXMLConstants.ELEMENT_TASK_USER))
    // .orderByHistoricActivityInstanceStartTime().desc()
    // .orderByHistoricActivityInstanceEndTime().desc()
    // .list();
    //
    // List<Comment> commentList =
    // taskService.getProcessInstanceComments(procInsId);
    //
    // List<WfProcNodeVo> elementVoList = new ArrayList<>();
    // for (HistoricActivityInstance activityInstance :
    // historicActivityInstanceList) {
    // WfProcNodeVo elementVo = new WfProcNodeVo();
    // elementVo.setProcDefId(activityInstance.getProcessDefinitionId());
    // elementVo.setActivityId(activityInstance.getActivityId());
    // elementVo.setActivityName(activityInstance.getActivityName());
    // elementVo.setActivityType(activityInstance.getActivityType());
    // elementVo.setCreateTime(activityInstance.getStartTime());
    // elementVo.setEndTime(activityInstance.getEndTime());
    // if (ObjectUtil.isNotNull(activityInstance.getDurationInMillis())) {
    // elementVo.setDuration(DateUtil.formatBetween(activityInstance.getDurationInMillis(),
    // BetweenFormatter.Level.SECOND));
    // }
    //
    // if
    // (BpmnXMLConstants.ELEMENT_EVENT_START.equals(activityInstance.getActivityType()))
    // {
    // if (ObjectUtil.isNotNull(historicProcIns)) {
    // String userId = historicProcIns.getStartUserId();
    // String nickName = userService.selectNickNameById(userId);
    // if (nickName != null) {
    // elementVo.setAssigneeId(userId);
    // elementVo.setAssigneeName(nickName);
    // }
    // }
    // } else if
    // (BpmnXMLConstants.ELEMENT_TASK_USER.equals(activityInstance.getActivityType()))
    // {
    // if (StringUtils.isNotBlank(activityInstance.getAssignee())) {
    // String userId = activityInstance.getAssignee();
    // String nickName = userService.selectNickNameById(userId);
    // elementVo.setAssigneeId(userId);
    // elementVo.setAssigneeName(nickName);
    // }
    // // approver
    // List<HistoricIdentityLink> linksForTask =
    // historyService.getHistoricIdentityLinksForTask
    // (activityInstance.getTaskId());
    // StringBuilder stringBuilder = new StringBuilder();
    // for (HistoricIdentityLink identityLink : linksForTask) {
    // if ("candidate".equals(identityLink.getType())) {
    // if (StringUtils.isNotBlank(identityLink.getUserId())) {
    // String userId = identityLink.getUserId();
    // String nickName = userService.selectNickNameById(userId);
    // stringBuilder.append(nickName).append(",");
    // }
    // if (StringUtils.isNotBlank(identityLink.getGroupId())) {
    // if (identityLink.getGroupId().startsWith(TaskConstants.ROLE_GROUP_PREFIX)) {
    // Long roleId =
    // Long.parseLong(StringUtils.stripStart(identityLink.getGroupId(),
    // TaskConstants.ROLE_GROUP_PREFIX));
    // SysRole role = roleService.selectRoleById(roleId);
    // stringBuilder.append(role.getRoleName()).append(",");
    // } else if
    // (identityLink.getGroupId().startsWith(TaskConstants.DEPT_GROUP_PREFIX)) {
    // Long deptId =
    // Long.parseLong(StringUtils.stripStart(identityLink.getGroupId(),
    // TaskConstants.DEPT_GROUP_PREFIX));
    // SysDept dept = deptService.selectDeptById(deptId);
    // stringBuilder.append(dept.getDeptName()).append(",");
    // }
    // }
    // }
    // }
    // if (StringUtils.isNotBlank(stringBuilder)) {
    // elementVo.setCandidate(stringBuilder.substring(0, stringBuilder.length() -
    // 1));
    // }
    // // Get
    // if (CollUtil.isNotEmpty(commentList)) {
    // List<Comment> comments = new ArrayList<>();
    // for (Comment comment : commentList) {
    //
    // if (comment.getTaskId().equals(activityInstance.getTaskId())) {
    // comments.add(comment);
    // }
    // }
    // elementVo.setCommentList(comments);
    // }
    // // Get info
    // List<SysUser> copyUsers = new ArrayList<>();
    // String taskId = activityInstance.getTaskId();
    // List<String> userIds = wfCopyService.selectCopyUserIdByTaskId(taskId);
    // if(!CollectionUtils.isEmpty(userIds)) {
    // for (String userId : userIds) {
    // SysUser sysUser = sysUserService.selectUserById(userId);
    // copyUsers.add(sysUser);
    // }
    // elementVo.setWfCopyUser(copyUsers);
    // }
    // }
    // elementVoList.add(elementVo);
    // }
    // Map<String, WfProcNodeVo> uniqueNodes = new HashMap<>();
    // for (WfProcNodeVo node : elementVoList) {
    // String activityId = node.getActivityId();
    // WfProcNodeVo existingNode = uniqueNodes.get(activityId);
    //
    // if (existingNode == null) {
    // // if current ID in uniqueNodes in , new node
    // WfProcNodeVo result = new WfProcNodeVo();
    // BeanUtil.copyProperties(node, result);
    // result.setActivityId(activityId);
    // result.setAssigneeInfoList(new ArrayList<>());
    //
    // if (node.getEndTime() == null) {
    // assigneeInfoVo assigneeInfoVo = new assigneeInfoVo();
    // //todo
    // assigneeInfoVo.setAssigneeName(node.getAssigneeName());
    // result.getAssigneeInfoList().add(assigneeInfoVo);
    // }
    //
    // if (node.getCommentList() != null && !node.getCommentList().isEmpty()) {
    // // if CommentList to null is empty, Set assigneeName and assigneeId
    // result.setAssigneeId(node.getAssigneeId());
    // result.setAssigneeName(node.getAssigneeName());
    // }
    // uniqueNodes.put(activityId, result);
    // } else {
    // // if current ID already in uniqueNodes in , assigneeName
    // if (node.getEndTime() == null) {
    //// existingNode.getAssigneeNameList().add(node.getAssigneeName());
    // assigneeInfoVo assigneeInfoVo = new assigneeInfoVo();
    // // todo
    // assigneeInfoVo.setAssigneeName(node.getAssigneeName());
    // existingNode.getAssigneeInfoList().add(assigneeInfoVo);
    // }
    //
    // if (node.getCommentList() != null && !node.getCommentList().isEmpty()) {
    // // if CommentList to null is empty, new assigneeName and assigneeId
    // existingNode.setAssigneeId(node.getAssigneeId());
    // existingNode.setAssigneeName(node.getAssigneeName());
    // }
    // }
    // }
    // List<WfProcNodeVo> deduplicatedList = new ArrayList<>(uniqueNodes.values());
    // // Comparator
    // Comparator<WfProcNodeVo> comparator = Comparator.comparing((WfProcNodeVo
    // node) -> node.getCreateTime())
    // .reversed() // createTime
    // .thenComparing(node -> node.getActivityType(), Comparator.reverseOrder()); //
    // createTime , activityType ( userTask , reverseOrder)
    // // Stream sorted method
    // deduplicatedList = deduplicatedList.stream()
    // .sorted(comparator)
    // .peek(node -> {
    // if (node.getAssigneeInfoList() == null ||
    // node.getAssigneeInfoList().isEmpty()) {
    // List<assigneeInfoVo> assigneeNamesSingleton = new ArrayList<>();
    // assigneeInfoVo assigneeInfoVo = new assigneeInfoVo();
    // assigneeInfoVo.setAssigneeName(node.getAssigneeName());
    // // todo
    // assigneeNamesSingleton.add(assigneeInfoVo);
    // node.setAssigneeInfoList(assigneeNamesSingleton);
    // }
    // })
    // .collect(Collectors.toList());
    // return deduplicatedList;
    // }

    /**
     * Get workflowExecute
     *
     * @param procInsId
     * @return
     */
    private WfViewerVo getFlowViewer(BpmnModel bpmnModel, String procInsId) {
        // Build Query
        HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInsId);
        List<HistoricActivityInstance> allActivityInstanceList = query.list();
        if (CollUtil.isEmpty(allActivityInstanceList)) {
            return new WfViewerVo();
        }
        // Query all already element
        List<HistoricActivityInstance> finishedElementList = allActivityInstanceList.stream()
                .filter(item -> ObjectUtil.isNotNull(item.getEndTime())).collect(Collectors.toList());
        // all already
        Set<String> finishedSequenceFlowSet = new HashSet<>();
        // all already tasknode
        Set<String> finishedTaskSet = new HashSet<>();
        finishedElementList.forEach(item -> {
            if (BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW.equals(item.getActivityType())) {
                finishedSequenceFlowSet.add(item.getActivityId());
            } else {
                finishedTaskSet.add(item.getActivityId());
            }
        });
        // Query all not finish node
        Set<String> unfinishedTaskSet = allActivityInstanceList.stream()
                .filter(item -> ObjectUtil.isNull(item.getEndTime())).map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toSet());
        // DFS Query not elementcollection
        Set<String> rejectedSet = FlowableUtils.dfsFindRejects(bpmnModel, unfinishedTaskSet, finishedSequenceFlowSet,
                finishedTaskSet);
        return new WfViewerVo(finishedTaskSet, finishedSequenceFlowSet, unfinishedTaskSet, rejectedSet);
    }

    /**
     * workflow idGet all usernoderelated userinfo
     *
     * @param processDefKey
     * @return
     */
    @Override
    public List<WfUserTaskInfoVo> getAllUserInfo(String processDefKey) {
        SysUser sysUser = getSysUser(AuthorizationInterceptor.getToken());
        // Query new workflow definition
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionKey(processDefKey).latestVersion()
                .singleResult();

        // Get workflow definition BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

        // Get workflow definition in all FlowElement
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();

        // all FlowElement, UserTask
        List<WfUserTaskInfoVo> wfUserTaskInfoVos = new ArrayList<>();
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask) {
                UserTask userTask = (UserTask) flowElement;
                // if user, Query user
                List<String> candidateUsers = userTask.getCandidateUsers();
                List<SysUser> candidateUsersInfo = new ArrayList<>();
                if (candidateUsers != null && !candidateUsers.isEmpty()) {
                    for (String candidateUser : candidateUsers) {
                        SysUser sysUserById = sysUserService.selectUserById(candidateUser);
                        candidateUsersInfo.add(sysUserById);
                    }
                } else if (userTask.getAssignee() != null) {
                    String userId = userTask.getAssignee();
                    if (userId != null) {
                        SysUser sysUserById = sysUserService.selectUserById(userId);
                        if (sysUserById != null) {
                            candidateUsersInfo.add(sysUserById);
                        }
                    }
                }
                // if candidate group, Query candidate group
                List<String> candidateGroups = userTask.getCandidateGroups();
                List<SysDeptView> candidateDeptGroupsInfo = new ArrayList<>();
                List<SysUserRoleView> candidateRoleGroupsInfo = new ArrayList<>();
                if (candidateGroups != null && !candidateGroups.isEmpty()) {
                    String dataType = userTask.getAttributeValue("http://flowable.org/bpmn", "dataType");
                    if ("DEPTS".equals(dataType)) {
                        for (String candidateGroup : candidateGroups) {
                            SysDeptView sysDeptView = sysDeptService.selectDeptById(candidateGroup.substring(4));
                            candidateDeptGroupsInfo.add(sysDeptView);
                        }
                    } else if ("ROLES".equals(dataType)) {
                        for (String candidateGroup : candidateGroups) {
                            SysUserRoleView sysRole = sysRoleService.selectRoleByCondition(sysUser.getUserId(),
                                    candidateGroup.substring(4));
                            candidateRoleGroupsInfo.add(sysRole);
                        }
                    }

                }
                // Check will is
                MultiInstanceLoopCharacteristics loopCharacteristics = userTask.getLoopCharacteristics();
                WfUserTaskInfoVo wfUserTaskInfoVo = new WfUserTaskInfoVo();
                if (loopCharacteristics != null) {
                    wfUserTaskInfoVo.setMultiInstanceType(loopCharacteristics.isSequential() == true ? "会签" : "或签");
                }
                wfUserTaskInfoVo.setUserTaskId(userTask.getId());
                wfUserTaskInfoVo.setUserTaskName(userTask.getName());
                wfUserTaskInfoVo.setCandidateUsers(candidateUsersInfo);

                wfUserTaskInfoVo.setCandidateDeptGroups(candidateDeptGroupsInfo);
                wfUserTaskInfoVo.setCandidateRoleGroups(candidateRoleGroupsInfo);
                if (!candidateUsersInfo.isEmpty() || !candidateDeptGroupsInfo.isEmpty()
                        || !candidateRoleGroupsInfo.isEmpty()) {
                    wfUserTaskInfoVos.add(wfUserTaskInfoVo);
                }
            }
        }
        return wfUserTaskInfoVos;
    }

    @Override
    public List<WfAttachment> getPDF(String procInstId) {
        // instanceidGet all info
        List<WfAttachment> wfAttachments = wfAttachmentMapper
                .selectList(new LambdaQueryWrapper<WfAttachment>().eq(WfAttachment::getProcInsId,
                        procInstId));
        return wfAttachments;
    }

    @Override
    public WfLastHisTaskInfoVo getLatestHisTaskInfo(String procInstId) {
        List<HistoricTaskInstance> latestCompletedTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(procInstId).finished().orderByHistoricTaskInstanceEndTime().desc().list();
        if (!CollectionUtils.isEmpty(latestCompletedTasks)) {
            HistoricTaskInstance latestCompletedTask = latestCompletedTasks.get(0);
            List<Comment> commentList = taskService.getProcessInstanceComments(procInstId);
            // need to Get property
            WfLastHisTaskInfoVo wfLastHisTaskInfoVo = new WfLastHisTaskInfoVo();
            wfLastHisTaskInfoVo.setTaskId(latestCompletedTask.getId());
            wfLastHisTaskInfoVo.setTaskName(latestCompletedTask.getName());
            wfLastHisTaskInfoVo.setAssigneeId(latestCompletedTask.getAssignee());
            wfLastHisTaskInfoVo
                    .setAssigneeName(sysUserService.selectUserById(latestCompletedTask.getAssignee()).getUserName());
            for (Comment comment : commentList) {
                if (StringUtils.isNotBlank(comment.getTaskId())
                        && comment.getTaskId().equals(latestCompletedTask.getId())) {
                    wfLastHisTaskInfoVo.setCommentMsg(comment.getFullMessage());
                    wfLastHisTaskInfoVo.setType(FlowComment.getRemarkByType(comment.getType()));
                }
            }
            return wfLastHisTaskInfoVo;
        } else {
            throw new RuntimeException("没有找到已完成的任务");
        }
    }

    /**
     * workflow , Query workflow definitionid and new id
     */
    @Override
    public WfDefAndDepVo getDefIdAndDepIdByProcKey(String processKey, String token) {
        SysUser sysUser = getSysUser(token);
        // workflow definition dataQuery
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(sysUser.getTenantId()).processDefinitionKey(processKey).latestVersion()
                .active().singleResult();
        if (processDefinition != null) {
            WfDefAndDepVo wfDefAndDepVo = new WfDefAndDepVo();
            wfDefAndDepVo.setDefinitionId(processDefinition.getId());
            wfDefAndDepVo.setDeploymentId(processDefinition.getDeploymentId());
            return wfDefAndDepVo;
        }
        return null;
    }

    /**
     * Process formvariable
     * autoGetFormFlag=true , variables in customName to formfield id
     *
     * @param procDef workflow definition
     * @param variables variablecollection
     * @param sysUser current user
     * @return Process after variablecollection
     */
    private Map<String, Object> processFormVariables(ProcessDefinition procDef,
            Map<String, Object> variables,
            SysUser sysUser) {
        // 1. Get form ID
        String formId = getFormIdFromModel(procDef, sysUser);
        if (StringUtils.isBlank(formId)) {
            throw new ServiceException("流程未绑定表单，请检查流程模型配置");
        }

        // 2. Query form
        WfFormVo formVo = wfFormService.queryById(formId);
        if (formVo == null || StringUtils.isBlank(formVo.getContent())) {
            throw new ServiceException("未找到表单或表单内容为空，formId=" + formId);
        }

        // 3. Build customName -> id
        Map<String, String> customNameToIdMap = buildCustomNameToIdMap(formVo.getContent());
        if (customNameToIdMap.isEmpty()) {
            log.warn("表单中未找到任何 customName 映射，formId={}", formId);
            return variables;
        }

        // 4. Convert variables
        Map<String, Object> newVariables = new HashMap<>();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // if in , new id; key
            String newKey = customNameToIdMap.getOrDefault(key, key);
            newVariables.put(newKey, value);

            // record log
            if (!key.equals(newKey)) {
                log.info("表单字段映射: {} -> {}", key, newKey);
            }
        }

        return newVariables;
    }

    /**
     * from workflow definition Model in Get form ID
     *
     * @param procDef workflow definition
     * @param sysUser current user
     * @return form ID, can to null
     */
    private String getFormIdFromModel(ProcessDefinition procDef, SysUser sysUser) {
        try {
            // queryBpmnJsonById method
            String deploymentId = procDef.getDeploymentId();
            Model model = repositoryService.createModelQuery()
                    .deploymentId(deploymentId)
                    .singleResult();

            // if deploymentId , modelKey Query new
            if (model == null) {
                model = repositoryService.createModelQuery()
                        .modelTenantId(sysUser.getTenantId())
                        .modelKey(procDef.getKey())
                        .latestVersion()
                        .singleResult();

                if (model != null) {
                    log.info("通过modelKey获取到Model，modelId={}, modelKey={}",
                            model.getId(), model.getKey());
                }
            }

            if (model != null && StringUtils.isNotBlank(model.getMetaInfo())) {
                WfMetaInfoDto metaInfo = JsonUtils.parseObject(
                        model.getMetaInfo(),
                        WfMetaInfoDto.class);
                if (metaInfo != null) {
                    return metaInfo.getFormId();
                }
            }
        } catch (Exception e) {
            log.error("获取表单ID失败, processDefId={}", procDef.getId(), e);
        }
        return null;
    }

    /**
     * from form content in Build customName -> id
     * if in customName,
     *
     * @param content form JSON
     * @return customName id
     */
    private Map<String, String> buildCustomNameToIdMap(String content) {
        Map<String, String> map = new HashMap<>();
        try {
            JSONObject jsonObject = JSON.parseObject(content);
            JSONArray widgetList = jsonObject.getJSONArray("widgetList");

            if (widgetList != null) {
                for (int i = 0; i < widgetList.size(); i++) {
                    JSONObject widget = widgetList.getJSONObject(i);
                    String customName = widget.getString("customName");
                    String id = widget.getString("id");

                    if (StringUtils.isNotBlank(customName) && StringUtils.isNotBlank(id)) {
                        // , if already in
                        if (!map.containsKey(customName)) {
                            map.put(customName, id);
                        } else {
                            log.warn("发现重复的customName: {}, 已使用第一个映射", customName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析表单内容失败", e);
        }
        return map;
    }
}
