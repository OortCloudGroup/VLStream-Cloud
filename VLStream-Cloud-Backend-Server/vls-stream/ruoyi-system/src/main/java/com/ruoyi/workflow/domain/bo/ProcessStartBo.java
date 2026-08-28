/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
     * workflow definitionid
     */
    @NotBlank(message = "流程定义id不能为null", groups = {AddGroup.class, EditGroup.class})
    private String processDefId;
    /**
     * workflow variable
     */
    private Map<String, Object> variables;
    /**
     * work orderobject
     */
    private WorkOrderBo workOrderBo;
    /**
     * loop infoparameter
     */
    private Job job;
    /**
     * Get form item
     */
    private boolean autoGetFormFlag;
    /**
     * service id
     */
    private String appId;
    /**
     * serviceevent
     */
    private String eventName;
    /**
     * work orderobject
     */
    private WorkOrder workOrder;

    /**
     * event before after
     * @return
     */
    private boolean frontFlag;


    // getter method will null
    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        return variables;
    }

    // setter method
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
