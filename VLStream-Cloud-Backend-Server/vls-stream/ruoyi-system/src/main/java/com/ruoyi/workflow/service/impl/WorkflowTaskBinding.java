package com.ruoyi.workflow.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.bo.WfTaskBo;

/** Verifies client-supplied process IDs against the task obtained from Flowable. */
final class WorkflowTaskBinding {
    private WorkflowTaskBinding() {}

    static void requireSameInstance(String actualInstanceId, String requestedInstanceId) {
        if (StringUtils.isBlank(actualInstanceId) || !actualInstanceId.equals(requestedInstanceId)) {
            throw new ServiceException("任务与流程实例不匹配");
        }
    }

    static void bindOptionalInstance(String actualInstanceId, WfTaskBo request) {
        if (StringUtils.isBlank(request.getProcInsId())) {
            if (StringUtils.isBlank(actualInstanceId)) {
                throw new ServiceException("任务缺少流程实例");
            }
            request.setProcInsId(actualInstanceId);
        } else {
            requireSameInstance(actualInstanceId, request.getProcInsId());
        }
    }
}
