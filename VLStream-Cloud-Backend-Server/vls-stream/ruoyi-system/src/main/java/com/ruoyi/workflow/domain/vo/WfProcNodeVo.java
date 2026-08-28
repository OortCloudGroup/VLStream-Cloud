/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

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
 * nodeelement object
 *
 * @author KonBAI
 * @createTime 2022/9/11 22:04
 */
@Data
@ExcelIgnoreUnannotated
public class WfProcNodeVo implements Serializable {
    /**
     * workflowID
     */
    private String procDefId;
    /**
     * ID
     */
    private String activityId;
    /**
     *
     */
    private String activityName;
    /**
     *
     */
    private String activityType;
    /**
     *
     */
    private String duration;
    /**
     * Execute Id
     */
    private String assigneeId;
    /**
     * Execute
     */
    private String assigneeName;

    /**
     * ID all Execute and whether approval

     */
    private List<assigneeInfoVo> assigneeInfoList;

    /**
     * Execute
     */
    private String candidate;
    /**
     * task
     */
    private List<Comment> commentList;
    /**
     * create time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * finish
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * info
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
     * taskid
     */
    private String taskId;
}
