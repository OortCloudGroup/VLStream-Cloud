/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.workflow.domain.Job;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.bo.WorkOrderBo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessStartBo extends BaseEntity {
    /**
     * 流程定义id
     */
    @NotBlank(message = "流程定义id不能为null", groups = {AddGroup.class, EditGroup.class})
    private String processDefId;
    /**
     * 流程变量
     */
    private Map<String, Object> variables;
    /**
     * 工单对象
     */
    private WorkOrderBo workOrderBo;
    /**
     * 循环定时信息参数
     */
    private Job job;
    /**
     * 自动获取表单项标识
     */
    private boolean autoGetFormFlag;
    /**
     * 摄像机服务传入的应用id
     */
    private String appId;
    /**
     * 摄像机服务事件名称
     */
    private String eventName;
    /**
     * 工单对象
     */
    private WorkOrder workOrder;

    /**
     * 事件管理前后端调用
     * @return
     */
    private boolean frontFlag;


    // 添加 getter 方法确保不会返回 null
    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        return variables;
    }

    // 添加 setter 方法
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
