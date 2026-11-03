/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.job;


import com.alibaba.excel.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workflow.service.WorkflowOwnershipService;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.service.IWorkOrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderJobService {

    private final IWfProcessService processService;
    private final IWorkOrderService workOrderService;
    private final WorkflowOwnershipService workflowOwnership;

    @XxlJob(value = "createWorkOrderJob")
    @Transactional(rollbackFor = Exception.class)
    public  void createWorkOrderJob() {
        // Get parameter
        ObjectMapper objectMapper=new ObjectMapper();
        Map<String, Object> map;
        try {
            map= objectMapper.readValue(XxlJobHelper.getJobParam(), HashMap.class);
        } catch (JsonProcessingException e) {
            log.error("参数解析失败");
            throw new RuntimeException(e);
        }
        // workflow
        ProcessStartBo processStartBoProperty = objectMapper.convertValue(map.get("processStartBo"), ProcessStartBo.class);
        SysUser sysUser = objectMapper.convertValue(map.get("sysUser"), SysUser.class);

        // from map in full Get parameter, null / empty
        Object workOrderJobFlagObj = map.get("workOrderJobFlag");
        if (workOrderJobFlagObj != null
            && StringUtils.isNotBlank(workOrderJobFlagObj.toString())
            && "1".equals(workOrderJobFlagObj.toString())) {
            processStartBoProperty.getWorkOrderBo().setWorkOrderJobFlag("1.2");
        }else{
            Double newValue = Double.valueOf(processStartBoProperty.getWorkOrderBo().getWorkOrderJobFlag());
            processStartBoProperty.getWorkOrderBo().setWorkOrderJobFlag(String.valueOf(newValue+0.1));
        }
        processStartBoProperty.getWorkOrderBo().setId(null);
        processStartBoProperty.getWorkOrderBo().setProcInsId(null);
        processStartBoProperty.getWorkOrderBo().setTaskId(null);
        if (processStartBoProperty.getJob() != null) throw new IllegalStateException("周期工单不允许嵌套调度任务");
        String processInstanceId = processService.startProcessByDefId(processStartBoProperty,sysUser);
        WorkOrder workOrder = processStartBoProperty.getWorkOrder();
        if (workOrder == null) {
            String taskId = processService.getTaskId(processInstanceId,sysUser);
            processStartBoProperty.getWorkOrderBo().setTaskId(taskId);
            processStartBoProperty.getWorkOrderBo().setProcInsId(processInstanceId);
            workOrder = workOrderService.insertByBo(processStartBoProperty.getWorkOrderBo(),sysUser);
        }
        if (workOrder != null) {
            if (!workflowOwnership.recordNewPeriodicOrder(sysUser.getTenantId(), processInstanceId, workOrder)) {
                throw new IllegalStateException("周期工单实例归属无法确认");
            }
            log.info("自动创建工单成功");
        } else {
            throw new IllegalStateException("周期工单创建失败");
        }
    }
}
