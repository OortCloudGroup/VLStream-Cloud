package com.ruoyi.workorder.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 工单业务对象 work_order
 *
 * @author 雷超群
 * @date 2025-01-02
 */

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkOrderBo extends BaseEntity {
    private String id;
    /**
     * 所属系统
     */
    private String systemId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 项目名称
     */
    private String projectId;

    /**
     * 工单(流程)类型
     */
    @NotBlank(message = "工单类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String workorderId;

    /**
     * 关联的流程id
     */
    @NotBlank(message = "流程名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processKey;

    /**
     * 工单编号
     */
    private String workorderNumber;

    /**
     * 工单标题
     */
    @NotBlank(message = "工单标题不能为空", groups = { AddGroup.class, EditGroup.class })
    private String title;

    /**
     * 工单描述
     */
    private String description;

    /**
     * 工单状态
     */
    private String workorderStatus;

    /**
     * 工单紧急程度
     */
    private String priority;

    /**
     * 审批状态
     */
    private String processStatus;

    /**
     * 工单来源
     */
    private String source;

    /**
     * 是否有偿
     */
    private String compensation;

    /**
     * 评价
     */
    private String evaluate;

    /**
     * 房号
     */
    private String roomNumber;

    /**
     * 附件地址（JSON格式）
     */
    private String attachmentUrls;

    /**
     * 工单(流程)类型_用于前端回显
     */
    private String workorderIdExtend;
    /**
     * 流程实例id
     */
    private String procInsId;

    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 派单人
     */
    private String assignId;
    /**
     * 周期性工单标识
     */
    private String workOrderJobFlag;
    /**
     * 周期性工单统一序号
     */
    private String workOrderJobSerial;
    /**
     * 查询全部应用工单流程
     */
    private Boolean WorkOrderAppAll = false;
    /**
     * 查询全部综合工单流程
     */
    private Boolean WorkOrderSynthesisAll = false;
    /**
     * 访问的接口路径
     */
    private String apiPath;
    /**
     * 事件编号
     */
    private String eventNumber;
    /**
     * 流程名称
     */
    private String processName;

}
