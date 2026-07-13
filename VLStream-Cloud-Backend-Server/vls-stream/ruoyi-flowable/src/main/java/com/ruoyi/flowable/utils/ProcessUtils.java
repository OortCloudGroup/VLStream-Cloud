/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.utils;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import org.flowable.common.engine.api.query.Query;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程工具类
 *
 * @author konbai
 * @since 2022/12/11 03:35
 */
public class ProcessUtils {


    public static void buildProcessSearch(Query<?, ?> query, ProcessQuery process, ProcessEngine processEngine, SysUser sysUser) {
        if (query instanceof ProcessDefinitionQuery) {
            buildProcessDefinitionSearch((ProcessDefinitionQuery) query, process);
        } else if (query instanceof TaskQuery) {
            buildTaskSearch((TaskQuery) query, process, processEngine,sysUser);
        } else if (query instanceof HistoricTaskInstanceQuery) {
            buildHistoricTaskInstanceSearch((HistoricTaskInstanceQuery) query, process, processEngine,sysUser);
        } else if (query instanceof HistoricProcessInstanceQuery) {
            buildHistoricProcessInstanceSearch((HistoricProcessInstanceQuery) query, process);
        }
    }

    /**
     * 构建流程定义搜索
     */
    public static void buildProcessDefinitionSearch(ProcessDefinitionQuery query, ProcessQuery process) {
        // 流程标识
        if (StringUtils.isNotBlank(process.getProcessKey())) {
            query.processDefinitionKeyLike("%" + process.getProcessKey() + "%");
        }
        // 流程名称
        if (StringUtils.isNotBlank(process.getProcessName())) {
            query.processDefinitionNameLike("%" + process.getProcessName() + "%");
        }
        // 流程分类
        if (StringUtils.isNotBlank(process.getCategory())) {
            query.processDefinitionCategory(process.getCategory());
        }

        // 流程状态
        if (StringUtils.isNotBlank(process.getState())) {
            if (SuspensionState.ACTIVE.toString().equals(process.getState())) {
                query.active();
            } else if (SuspensionState.SUSPENDED.toString().equals(process.getState())) {
                query.suspended();
            }
        }
    }

    /**
     * 构建任务搜索
     */
    public static void buildTaskSearch(TaskQuery query, ProcessQuery process, ProcessEngine processEngine,SysUser sysUser) {
        Map<String, Object> params = process.getParams();
        if (StringUtils.isNotBlank(process.getProcessKey())) {
            query.processDefinitionKeyLike("%" + process.getProcessKey() + "%");
        }
        if (StringUtils.isNotBlank(process.getProcessName())) {
            query.processDefinitionNameLike("%" + process.getProcessName() + "%");
        }
        if (StringUtils.isNotBlank(process.getCategory())) {
            List<String> strings = new ArrayList<>();
            strings.add(process.getCategory());
            query.processCategoryIn(strings);
        }
        if (params.get("beginTime") != null && params.get("endTime") != null) {
            query.taskCreatedAfter(DateUtils.parseDate(params.get("beginTime")));
            query.taskCreatedBefore(DateUtils.parseDate(params.get("endTime")));
        }
        // 筛选提交时间
        if (ObjectUtil.isNotNull(process.getProStartBeginTime()) && ObjectUtil.isNotNull(process.getProStartEndTime())) {
            Date instanceBeginTime = process.getProStartBeginTime();
            Date instanceEndTime = process.getProStartEndTime();
//            Date instanceBeginTime = DateUtils.parseDate(process.getProStartBeginTime());
//            Date instanceEndTime = DateUtils.parseDate(process.getProStartEndTime());
            // 使用ProcessInstanceQuery筛选出符合条件的流程实例ID
            RuntimeService runtimeService = processEngine.getRuntimeService();
            ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .startedAfter(instanceBeginTime)
                .startedBefore(instanceEndTime);
            List<String> instanceIds = processInstanceQuery.list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
            if(CollectionUtils.isEmpty(instanceIds)) {
                instanceIds.add(String.valueOf(UUID.randomUUID()));
            }
            query.processInstanceIdIn(instanceIds);
        }
    }

    private static void buildHistoricTaskInstanceSearch(HistoricTaskInstanceQuery query, ProcessQuery process, ProcessEngine processEngine,SysUser sysUser) {
        Map<String, Object> params = process.getParams();
        if (StringUtils.isNotBlank(process.getProcessKey())) {
            query.processDefinitionKeyLike("%" + process.getProcessKey() + "%");
        }
        if (StringUtils.isNotBlank(process.getProcessName())) {
            query.processDefinitionNameLike("%" + process.getProcessName() + "%");
        }
        if (StringUtils.isNotBlank(process.getCategory())) {
            List<String> strings = new ArrayList<>();
            strings.add(process.getCategory());
            query.processCategoryIn(strings);
        }
        if (params.get("beginTime") != null && params.get("endTime") != null) {
            query.taskCompletedAfter(DateUtils.parseDate(params.get("beginTime")));
            query.taskCompletedBefore(DateUtils.parseDate(params.get("endTime")));
        }
        if (StringUtils.isNotBlank(process.getState())) {
            query.processVariableValueEquals(ProcessConstants.PROCESS_STATUS_KEY, process.getState());
        }
        // 筛选提交时间
        if (ObjectUtil.isNotNull(process.getProStartBeginTime()) && ObjectUtil.isNotNull(process.getProStartEndTime())) {
            Date instanceBeginTime = process.getProStartBeginTime();
            Date instanceEndTime = process.getProStartEndTime();
            RuntimeService runtimeService = processEngine.getRuntimeService();
            ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .startedAfter(instanceBeginTime)
                .startedBefore(instanceEndTime);
            List<String> instanceIds = processInstanceQuery.list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
            if(CollectionUtils.isEmpty(instanceIds)) {
                instanceIds.add(String.valueOf(UUID.randomUUID()));
            }
            query.processInstanceIdIn(instanceIds);
        }
    }

    /**
     * 构建历史流程实例搜索
     */
    public static void buildHistoricProcessInstanceSearch(HistoricProcessInstanceQuery query, ProcessQuery process) {
        Map<String, Object> params = process.getParams();
        // 流程标识
        if (StringUtils.isNotBlank(process.getProcessKey())) {
            query.processDefinitionKey(process.getProcessKey());
        }
        // 流程名称
        if (StringUtils.isNotBlank(process.getProcessName())) {
            query.processDefinitionName(process.getProcessName());
        }
        // 流程名称
        if (StringUtils.isNotBlank(process.getCategory())) {
            query.processDefinitionCategory(process.getCategory());
        }
        if (process.getProStartBeginTime() != null && process.getProStartBeginTime() != null) {
            query.startedAfter(process.getProStartBeginTime());
            query.startedBefore(process.getProStartBeginTime());
        }
    }

}
