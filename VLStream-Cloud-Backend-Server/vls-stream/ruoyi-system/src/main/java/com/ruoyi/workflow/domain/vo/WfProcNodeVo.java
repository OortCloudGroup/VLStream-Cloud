package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.entity.SysUser;
import lombok.Data;
import org.flowable.engine.task.Comment;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工作流节点元素视图对象
 *
 * @author KonBAI
 * @createTime 2022/9/11 22:04
 */
@Data
@ExcelIgnoreUnannotated
public class WfProcNodeVo implements Serializable {
    /**
     * 流程ID
     */
    private String procDefId;
    /**
     * 活动ID
     */
    private String activityId;
    /**
     * 活动名称
     */
    private String activityName;
    /**
     * 活动类型
     */
    private String activityType;
    /**
     * 活动耗时
     */
    private String duration;
    /**
     * 执行人Id
     */
    private String assigneeId;
    /**
     * 执行人名称
     */
    private String assigneeName;

    /**
     * 存储同一活动ID下所有执行人名称和是否审批的列表

     */
    private List<assigneeInfoVo> assigneeInfoList;

    /**
     * 候选执行人
     */
    private String candidate;
    /**
     * 任务意见
     */
    private List<Comment> commentList;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 抄送人信息
     */
    private List<SysUser> wfCopyUser;
    /**
     * TRANSACTION_ORDER_
     */
    private Integer transactionOrder;
    /**
     * EXECUTION_ID_
     */
    private String executionId;
    /**
     * 任务id
     */
    private String taskId;
}
