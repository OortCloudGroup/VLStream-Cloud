/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.flowable.utils.TaskUtils;
import com.ruoyi.workflow.domain.vo.WfAppearanceAllCountVo;
import com.ruoyi.workflow.service.IWfAppearanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RequiredArgsConstructor
@Service
public class WfAppearanceServiceImpl extends FlowServiceFactory implements IWfAppearanceService {
    @Override
    public WfAppearanceAllCountVo getAllCount(String token) {
        SysUser sysUser = getSysUser(token);

        // 获取待处理工单数量
        Long todoCount = taskService.createTaskQuery()
            .active()
            .taskCandidateOrAssigned(sysUser.getUserId())
            .taskCandidateGroupIn(TaskUtils.getCandidateGroup(sysUser))
            .taskTenantId(sysUser.getTenantId())
            .count();

        // 获取已处理的工单数量
        Long finishedCount = historyService.createHistoricTaskInstanceQuery()
            .taskTenantId(sysUser.getTenantId())
            .finished() // 只查找已完成的任务
            .taskAssignee(sysUser.getUserId())
            .count();

        // 获取我发起的工单数量
        Long ownCount = historyService.createHistoricProcessInstanceQuery()
            .processInstanceTenantId(sysUser.getTenantId())
            .startedBy(sysUser.getUserId())
            .count();

        // 获取本周一的0点
        String thisWeekMondayStr  = DateUtils.getThisWeekMonday();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date thisWeekMonday = null;
        try {
            thisWeekMonday = sdf.parse(thisWeekMondayStr);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("解析本周一0点时间失败");
        }
        // 获取本周一0点之前的数量
        Long todoCountLimitWeekMonday = taskService.createTaskQuery()
            .active()
            .taskCandidateOrAssigned(sysUser.getUserId())
            .taskCandidateGroupIn(TaskUtils.getCandidateGroup(sysUser))
            .taskCreatedBefore(thisWeekMonday)
            .taskTenantId(sysUser.getTenantId())
            .count();
        Long finishedCountLimitWeekMonday = historyService.createHistoricTaskInstanceQuery()
            .finished()
            .taskAssignee(sysUser.getUserId())
            .taskCreatedBefore(thisWeekMonday)
            .taskTenantId(sysUser.getTenantId())
            .count();
        Long ownCountLimitWeekMonday = historyService.createHistoricProcessInstanceQuery()
            .processInstanceTenantId(sysUser.getTenantId())
            .startedBy(sysUser.getUserId())
            .startedBefore(thisWeekMonday)
            .count();
        // 本周一0点之前总数量
        Long allCountLimitWeekMonday = todoCountLimitWeekMonday + finishedCountLimitWeekMonday + ownCountLimitWeekMonday;

        WfAppearanceAllCountVo wfAppearanceAllCountVo = new WfAppearanceAllCountVo();
        wfAppearanceAllCountVo.setTodoCount(todoCount);
        wfAppearanceAllCountVo.setFinishedCount(finishedCount);
        wfAppearanceAllCountVo.setOvertimeCount(null);
        wfAppearanceAllCountVo.setAllCount(todoCount + finishedCount + ownCount);
        return wfAppearanceAllCountVo;
    }

    /**
     * 通过token获取用户信息
     * @param token
     * @return
     */
    private SysUser getSysUser(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if(user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        System.out.println(" 用户缓存信息 " + user);
        return user;
    }
}
