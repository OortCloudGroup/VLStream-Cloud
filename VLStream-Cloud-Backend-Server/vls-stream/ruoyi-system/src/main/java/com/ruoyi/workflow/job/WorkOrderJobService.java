/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.job;


import com.alibaba.excel.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.service.IWorkOrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderJobService {

    private final IWfProcessService processService;
    private final IWorkOrderService workOrderService;

    @XxlJob(value = "createWorkOrderJob")
    public  void createWorkOrderJob() {
        // 获取参数
        ObjectMapper objectMapper=new ObjectMapper();
        Map<String, Object> map;
        try {
            map= objectMapper.readValue(XxlJobHelper.getJobParam(), HashMap.class);
        } catch (JsonProcessingException e) {
            log.error("参数解析失败");
            throw new RuntimeException(e);
        }
        //启动流程
        ProcessStartBo processStartBoProperty = objectMapper.convertValue(map.get("processStartBo"), ProcessStartBo.class);
        SysUser sysUser = objectMapper.convertValue(map.get("sysUser"), SysUser.class);

        String processInstanceId = processService.startProcessByDefId(processStartBoProperty,sysUser);


        // 从map中安全获取参数，避免空指针异常
        Object workOrderJobFlagObj = map.get("workOrderJobFlag");
        if (workOrderJobFlagObj != null
            && StringUtils.isNotBlank(workOrderJobFlagObj.toString())
            && "1".equals(workOrderJobFlagObj.toString())) {
            processStartBoProperty.getWorkOrderBo().setWorkOrderJobFlag("1.2");
        }else{
            Double newValue = Double.valueOf(processStartBoProperty.getWorkOrderBo().getWorkOrderJobFlag());
            processStartBoProperty.getWorkOrderBo().setWorkOrderJobFlag(String.valueOf(newValue+0.1));
        }
        //创建工单
        String taskId = processService.getTaskId(processInstanceId,sysUser);
        processStartBoProperty.getWorkOrderBo().setTaskId(taskId);
        processStartBoProperty.getWorkOrderBo().setProcInsId(processInstanceId);
        processStartBoProperty.getWorkOrderBo().setId(null);
        WorkOrder workOrder = workOrderService.insertByBo(processStartBoProperty.getWorkOrderBo(),sysUser);
        if (workOrder != null) {
            log.info("自动创建工单成功");
        } else {
            log.info("自动工单失败");
        }
    }
}
