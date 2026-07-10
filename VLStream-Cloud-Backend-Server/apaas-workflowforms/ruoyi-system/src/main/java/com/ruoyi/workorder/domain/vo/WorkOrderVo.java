package com.ruoyi.workorder.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

import java.util.Date;

/**
 * 工单视图对象 work_order
 *
 * @author 雷超群
 * @date 2025-01-02
 */
@Data
@ExcelIgnoreUnannotated
public class WorkOrderVo {

    private static final long serialVersionUID = 1L;

    /**
     * 工单主键ID
     */
    @ExcelProperty(value = "工单主键ID")
    private String id;

    /**
     * 租户id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * 所属系统
     */
    @ExcelProperty(value = "所属系统")
    private String systemId;

    /**
     * 项目名称
     */
    @ExcelProperty(value = "项目名称")
    private String projectId;

    /**
     * 工单(流程)类型
     */
    @ExcelProperty(value = "工单(流程)类型")
    private String workorderId;

    /**
     * 关联的流程key
     */
    @ExcelProperty(value = "关联的流程key")
    private String processKey;

    /**
     * 工单编号
     */
    @ExcelProperty(value = "工单编号")
    private String workorderNumber;

    /**
     * 工单标题
     */
    @ExcelProperty(value = "工单标题")
    private String title;

    /**
     * 工单描述
     */
    @ExcelProperty(value = "工单描述")
    private String description;

    /**
     * 工单状态
     */
    @ExcelProperty(value = "工单状态")
    private String workorderStatus;

    /**
     * 工单紧急程度
     */
    @ExcelProperty(value = "工单紧急程度")
    private String priority;

    /**
     * 审批状态
     */
    @ExcelProperty(value = "审批状态")
    private String processStatus;

    /**
     * 工单来源
     */
    @ExcelProperty(value = "工单来源")
    private String source;

    // /**
    // * 是否有偿
    // */
    // @ExcelProperty(value = "是否有偿")
    // private String compensation;
    //
    // /**
    // * 评价
    // */
    // @ExcelProperty(value = "评价")
    // private String evaluate;

    // /**
    // * 房号
    // */
    // @ExcelProperty(value = "房号")
    // private String roomNumber;

    /**
     * 创建人
     */
    @ExcelProperty(value = "创建人")
    private String createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @ExcelProperty(value = "修改时间")
    private Date updateTime;

    /**
     * 附件地址（JSON格式）
     */
    @ExcelProperty(value = "附件地址", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "JSON格式")
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
     * 流程实例id
     */
    private String procInstId;

    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 派单人
     */
    private String assignId;
    /**
     * 流程版本
     */
    @ExcelProperty(value = "流程版本")
    private int procDefVersion;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 流程定义名称
     */
    private String procDefName;
    /**
     * 工单类型
     */
    private String categoryName;
    /**
     * 流程发起人Id
     */
    private String startUserId;
    /**
     * 流程发起人名称
     */
    private String startUserName;
    /**
     * 周期性工单标识
     */
    private String workOrderJobFlag;
    /**
     * 周期性工单统一序号
     */
    private String workOrderJobSerial;
    /**
     * 流程结束时间
     */
    private String endTime;
    // /**
    // * 图标id
    // */
    // private String iconId;

    /**
     * 发起人部门名称
     */
    private String deptName;
    /**
     * 当前处理人
     */
    private String currentAssignName;
    /**
     * 审批节点
     */
    private String currentActivityName;
    /**
     * 处理时长
     */
    private String processingTime;
    /**
     * 事件编号
     */
    private String eventNumber;
    /**
     * 流程名称
     */
    private String processName;
}
