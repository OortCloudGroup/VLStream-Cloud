/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.workflow.domain.dto.WfCommentDto;
import lombok.Data;
import org.flowable.engine.task.Comment;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * task object
 *
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Data
@ExcelIgnoreUnannotated
public class WfTaskVo implements Serializable {
    /**
     * task
     */
    private String taskId;
    /**
     * task
     */
    private String taskName;
    /**
     * taskKey
     */
    private String taskDefKey;
    /**
     * taskExecute Id
     */
    private String assigneeId;
    /**
     * department name
     */
    @Deprecated
    private String deptName;
    /**
     * workflow department name
     */
    private String startDeptName;
    /**
     * taskExecute
     */
    private String assigneeName;
    /**
     * workflow Id
     */
    private String startUserId;
    /**
     * workflow
     */
    private String startUserName;
    /**
     * workflow
     */
    private String category;
    /**
     * workflow
     */
    private String categoryName;
    /**
     * workflow variableinfo
     */
    private Object procVars;
    /**
     * variableinfo
     */
    private Object taskLocalVars;
    /**
     * workflow
     */
    private String deployId;
    /**
     * workflowID
     */
    private String procDefId;
    /**
     * workflowkey
     */
    private String procDefKey;
    /**
     * workflow definition
     */
    private String procDefName;
    /**
     * workflow definition
     */
    private int procDefVersion;
    /**
     * workflow instance ID
     */
    private String procInsId;
    /**
     * history workflow instance ID
     */
    private String hisProcInsId;
    /**
     * task
     */
    private String duration;
    /**
     * task
     */
    private WfCommentDto comment;
    /**
     * task
     */
    private List<Comment> commentList;
    /**
     * Execute
     */
    private String candidate;
    /**
     * taskcreate time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * task
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /**
     * workflow
     */
    private String processStatus;

    /**
     * workflow instancecreate time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proInsCreateTime;
    /**
     * work orderid
     */
    private String workOrderId;
    /**
     * work order
     */
    private String workOrderName;
}
