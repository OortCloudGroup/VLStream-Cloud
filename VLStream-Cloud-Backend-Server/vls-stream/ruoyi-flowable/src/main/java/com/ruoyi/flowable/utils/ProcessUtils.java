/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.flowable.utils;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
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
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * workflow
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
     * Build workflow definition
     */
    public static void buildProcessDefinitionSearch(ProcessDefinitionQuery query, ProcessQuery process) {
        // workflow
        if (StringUtils.isNotBlank(process.getProcessKey())) {
            query.processDefinitionKeyLike("%" + process.getProcessKey() + "%");
        }
        // workflow
        if (StringUtils.isNotBlank(process.getProcessName())) {
            query.processDefinitionNameLike("%" + process.getProcessName() + "%");
        }
        // workflow
        if (StringUtils.isNotBlank(process.getCategory())) {
            query.processDefinitionCategory(process.getCategory());
        }

        // workflow
        if (StringUtils.isNotBlank(process.getState())) {
            if (SuspensionState.ACTIVE.toString().equals(process.getState())) {
                query.active();
            } else if (SuspensionState.SUSPENDED.toString().equals(process.getState())) {
                query.suspended();
            }
        }
    }

    /**
     * Build task
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
        //
        if (ObjectUtil.isNotNull(process.getProStartBeginTime()) && ObjectUtil.isNotNull(process.getProStartEndTime())) {
            Date instanceBeginTime = process.getProStartBeginTime();
            Date instanceEndTime = process.getProStartEndTime();
//            Date instanceBeginTime = DateUtils.parseDate(process.getProStartBeginTime());
//            Date instanceEndTime = DateUtils.parseDate(process.getProStartEndTime());
            // ProcessInstanceQuery workflow instance ID
            RuntimeService runtimeService = processEngine.getRuntimeService();
            ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .startedAfter(instanceBeginTime)
                .startedBefore(instanceEndTime);
            List<String> instanceIds = processInstanceQuery.listPage(0, 10001).stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
            if (!(query instanceof TaskQueryImpl)) {
                throw new ServiceException("无法安全合并流程筛选条件");
            }
            List<String> scoped = intersectInstanceIds(((TaskQueryImpl) query).getProcessInstanceIds(), instanceIds);
            if (scoped.isEmpty()) {
                // Both predicates address the same column and cannot simultaneously match.
                query.processInstanceId("0").processInstanceIdIn(Collections.singletonList("1"));
            } else {
                query.processInstanceIdIn(scoped);
            }
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
        //
        if (ObjectUtil.isNotNull(process.getProStartBeginTime()) && ObjectUtil.isNotNull(process.getProStartEndTime())) {
            Date instanceBeginTime = process.getProStartBeginTime();
            Date instanceEndTime = process.getProStartEndTime();
            RuntimeService runtimeService = processEngine.getRuntimeService();
            ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
                .processInstanceTenantId(sysUser.getTenantId())
                .startedAfter(instanceBeginTime)
                .startedBefore(instanceEndTime);
            List<String> instanceIds = processInstanceQuery.listPage(0, 10001).stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
            if (!(query instanceof HistoricTaskInstanceQueryImpl)) {
                throw new ServiceException("无法安全合并流程筛选条件");
            }
            List<String> scoped = intersectInstanceIds(((HistoricTaskInstanceQueryImpl) query).getProcessInstanceIds(), instanceIds);
            if (scoped.isEmpty()) {
                query.processInstanceId("0").processInstanceIdIn(Collections.singletonList("1"));
            } else {
                query.processInstanceIdIn(scoped);
            }
        }
    }

    static List<String> intersectInstanceIds(Collection<String> authorized, List<String> timed) {
        if (timed.size() > 10000 || (authorized != null && authorized.size() > 10000)) {
            throw new ServiceException("流程筛选结果超过临时安全上限，请缩小查询范围");
        }
        if (authorized == null) return timed;
        Set<String> allowed = new HashSet<>(authorized);
        return timed.stream().filter(allowed::contains).collect(Collectors.toList());
    }

    /**
     * Build history workflow instance
     */
    public static void buildHistoricProcessInstanceSearch(HistoricProcessInstanceQuery query, ProcessQuery process) {
        Map<String, Object> params = process.getParams();
        // workflow
        if (StringUtils.isNotBlank(process.getProcessKey())) {
            query.processDefinitionKey(process.getProcessKey());
        }
        // workflow
        if (StringUtils.isNotBlank(process.getProcessName())) {
            query.processDefinitionName(process.getProcessName());
        }
        // workflow
        if (StringUtils.isNotBlank(process.getCategory())) {
            query.processDefinitionCategory(process.getCategory());
        }
        if (process.getProStartBeginTime() != null && process.getProStartBeginTime() != null) {
            query.startedAfter(process.getProStartBeginTime());
            query.startedBefore(process.getProStartBeginTime());
        }
    }

}
