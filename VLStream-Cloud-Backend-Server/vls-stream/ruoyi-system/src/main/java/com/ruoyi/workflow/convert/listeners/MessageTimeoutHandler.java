/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.convert.listeners;

import com.ruoyi.workflow.service.IWfTaskService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.el.FixedValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.common.enums.WorkOrderStatus;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.service.IWorkOrderService;
import org.springframework.stereotype.Component;

/**
 * MessageNode Process
 */
@Slf4j
@Component
public class MessageTimeoutHandler implements JavaDelegate {

    @Autowired
    @Lazy
    private IWfTaskService wfTaskService;

    @Autowired
    private IWorkOrderService workOrderService;

    private FixedValue timeoutAction;
    private FixedValue repeatCount;
    private FixedValue priority;
    private FixedValue data;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            log.info("MessageTimeoutHandler 执行: executionId={}", execution.getId());

            int action = 2; //
            if (timeoutAction != null) {
                Object val = timeoutAction.getValue(execution);
                if (val != null)
                    action = Integer.parseInt(val.toString());
            }

            int maxRepeat = 0;
            if (repeatCount != null) {
                Object val = repeatCount.getValue(execution);
                if (val != null)
                    maxRepeat = Integer.parseInt(val.toString());
            }

            // Get current (Local variable in UserTask after can , need to execution variable)
            // : BoundaryEvent after, execution is ?
            // in , UserTask finish, execution ServiceTask.
            // workflow variable
            String countVarName = "messageNode_" + execution.getCurrentActivityId() + "_retryCount";
            // : execution.getCurrentActivityId() is ServiceTask ID, is UserTask ID
            // need to key. UserTask ID ( in convert in Generate ID is ? is )
            // execution is ServiceTask execution.

            // , variable , assuming workflow in only MessageNode loop
            // "messageNode_retryCount"
            Integer currentRetry = (Integer) execution.getVariable("messageNode_retryCount");
            if (currentRetry == null)
                currentRetry = 0;

            log.info("超时处理: action={}, maxRepeat={}, currentRetry={}", action, maxRepeat, currentRetry);

            if (action == 1) { // notification
                if (currentRetry < maxRepeat) {
                    // workflow
                    execution.setVariable("messageNode_retryCount", currentRetry + 1);

                    // 【Add 】
                    execution.setVariable("messageNode_verifyCount_reset", true);

                    // Set loop
                    execution.setVariable("messageNode_loopBack", true);
                    execution.setVariable("messageNode_autoReject", false);

                    log.info("触发重复通知（第{}次），准备回退到 UserTask", currentRetry + 1);
                } else {
                    log.info("重试次数已达上限（{}/{}），转为自动通过", currentRetry, maxRepeat);
                    execution.setVariable("messageNode_loopBack", false);
                    execution.setVariable("messageNode_autoReject", false);
                }
            } else if (action == 2) { //
                log.info("触发自动通过，流程继续到下一个节点");
                execution.setVariable("messageNode_loopBack", false);
                execution.setVariable("messageNode_autoReject", false);
            } else if (action == 3) { //
                log.info("触发自动驳回，流程跳转到 EndEvent");
                execution.setVariable("messageNode_autoReject", true);
                execution.setVariable("messageNode_loopBack", false);

                // Set workflow to TERMINATED
                execution.setVariable("processStatus", "TERMINATED");
                execution.setVariable("rejectReason", "消息通知超时自动驳回");

                // 【Add 】 new work order taskID
                String workorderId = (String) execution.getVariable("workorderId");
                if (StringUtils.isNotBlank(workorderId)) {
                    try {
                        LambdaUpdateWrapper<WorkOrder> updateWrapper = new LambdaUpdateWrapper<>();
                        updateWrapper.eq(WorkOrder::getId, workorderId);
                        // Set to already (RETURNED)
                        updateWrapper.set(WorkOrder::getWorkorderStatus, WorkOrderStatus.RETURNED.getStatus());
                        // taskID
                        updateWrapper.set(WorkOrder::getTaskId, "");
                        workOrderService.update(updateWrapper);
                        log.info("MessageNode自动驳回，更新工单状态为已退回，并清除任务ID: workorderId={}", workorderId);
                    } catch (Exception e) {
                        log.error("更新工单状态失败: workorderId={}", workorderId, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("MessageTimeoutHandler 执行失败", e);
        }
    }
}
