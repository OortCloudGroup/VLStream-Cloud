/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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

        // Get Process work order
        Long todoCount = taskService.createTaskQuery()
            .active()
            .taskCandidateOrAssigned(sysUser.getUserId())
            .taskCandidateGroupIn(TaskUtils.getCandidateGroup(sysUser))
            .taskTenantId(sysUser.getTenantId())
            .count();

        // Get already Process work order
        Long finishedCount = historyService.createHistoricTaskInstanceQuery()
            .taskTenantId(sysUser.getTenantId())
            .finished() // only find already task
            .taskAssignee(sysUser.getUserId())
            .count();

        // Get work order
        Long ownCount = historyService.createHistoricProcessInstanceQuery()
            .processInstanceTenantId(sysUser.getTenantId())
            .startedBy(sysUser.getUserId())
            .count();

        // Get 0
        String thisWeekMondayStr  = DateUtils.getThisWeekMonday();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date thisWeekMonday = null;
        try {
            thisWeekMonday = sdf.parse(thisWeekMondayStr);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("解析本周一0点时间失败");
        }
        // Get 0 before
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
        // 0 before
        Long allCountLimitWeekMonday = todoCountLimitWeekMonday + finishedCountLimitWeekMonday + ownCountLimitWeekMonday;

        WfAppearanceAllCountVo wfAppearanceAllCountVo = new WfAppearanceAllCountVo();
        wfAppearanceAllCountVo.setTodoCount(todoCount);
        wfAppearanceAllCountVo.setFinishedCount(finishedCount);
        wfAppearanceAllCountVo.setOvertimeCount(null);
        wfAppearanceAllCountVo.setAllCount(todoCount + finishedCount + ownCount);
        return wfAppearanceAllCountVo;
    }

    /**
     * tokenGet userinfo
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
