/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * MessageNode 超时处理器
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

            int action = 2; // 默认自动通过
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

            // 获取当前重试次数 (Local 变量在 UserTask 销毁后可能丢失，需要用 execution 变量)
            // 注意：BoundaryEvent 触发后，execution 还是同一个吗？
            // 对于中断型，UserTask 结束，execution 进入 ServiceTask。
            // 我们使用流程变量来持久化重试次数
            String countVarName = "messageNode_" + execution.getCurrentActivityId() + "_retryCount";
            // 注意：execution.getCurrentActivityId() 是 ServiceTask 的 ID，不是 UserTask 的 ID
            // 我们需要一个稳定的 key。可以使用 UserTask 的 ID (但在 convert 中生成的 ID 是固定的吗？是的)
            // 这里的 execution 是 ServiceTask 的 execution。

            // 简单起见，使用一个特定的变量名，假设流程中只有一个活跃的 MessageNode 重试循环
            // 或者使用 "messageNode_retryCount"
            Integer currentRetry = (Integer) execution.getVariable("messageNode_retryCount");
            if (currentRetry == null)
                currentRetry = 0;

            log.info("超时处理: action={}, maxRepeat={}, currentRetry={}", action, maxRepeat, currentRetry);

            if (action == 1) { // 重复通知
                if (currentRetry < maxRepeat) {
                    // 增加流程级别的重试计数
                    execution.setVariable("messageNode_retryCount", currentRetry + 1);

                    // 【新增】重置验证次数标记
                    execution.setVariable("messageNode_verifyCount_reset", true);

                    // 设置循环标志
                    execution.setVariable("messageNode_loopBack", true);
                    execution.setVariable("messageNode_autoReject", false);

                    log.info("触发重复通知（第{}次），准备回退到 UserTask", currentRetry + 1);
                } else {
                    log.info("重试次数已达上限（{}/{}），转为自动通过", currentRetry, maxRepeat);
                    execution.setVariable("messageNode_loopBack", false);
                    execution.setVariable("messageNode_autoReject", false);
                }
            } else if (action == 2) { // 自动通过
                log.info("触发自动通过，流程继续到下一个节点");
                execution.setVariable("messageNode_loopBack", false);
                execution.setVariable("messageNode_autoReject", false);
            } else if (action == 3) { // 自动驳回
                log.info("触发自动驳回，流程跳转到 EndEvent");
                execution.setVariable("messageNode_autoReject", true);
                execution.setVariable("messageNode_loopBack", false);

                // 设置流程状态为 TERMINATED
                execution.setVariable("processStatus", "TERMINATED");
                execution.setVariable("rejectReason", "消息通知超时自动驳回");

                // 【新增】更新工单状态并清除任务ID
                String workorderId = (String) execution.getVariable("workorderId");
                if (StringUtils.isNotBlank(workorderId)) {
                    try {
                        LambdaUpdateWrapper<WorkOrder> updateWrapper = new LambdaUpdateWrapper<>();
                        updateWrapper.eq(WorkOrder::getId, workorderId);
                        // 设置状态为已退回 (RETURNED)
                        updateWrapper.set(WorkOrder::getWorkorderStatus, WorkOrderStatus.RETURNED.getStatus());
                        // 清除任务ID
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
