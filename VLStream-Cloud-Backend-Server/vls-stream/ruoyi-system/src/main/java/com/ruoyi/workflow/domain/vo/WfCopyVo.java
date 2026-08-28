/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;


/**
 * workflow object wf_copy
 *
 * @author ruoyi
 * @date 2022-05-19
 */
@Data
@ExcelIgnoreUnannotated
public class WfCopyVo {

    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    @ExcelProperty(value = "抄送主键")
    private Long copyId;

    /**
     *
     */
    @ExcelProperty(value = "抄送标题")
    private String title;

    /**
     * workflowprimary key
     */
    @ExcelProperty(value = "流程主键")
    private String processId;

    /**
     * workflow
     */
    @ExcelProperty(value = "流程名称")
    private String processName;

    /**
     * workflow primary key
     */
    @ExcelProperty(value = "流程分类主键")
    private String categoryId;

    /**
     * primary key
     */
    @ExcelProperty(value = "部署主键")
    private String deploymentId;

    /**
     * workflow instanceprimary key
     */
    @ExcelProperty(value = "流程实例主键")
    private String instanceId;

    /**
     * taskprimary key
     */
    @ExcelProperty(value = "任务主键")
    private String taskId;

    /**
     * userprimary key
     */
    @ExcelProperty(value = "用户主键")
    private Long userId;

    /**
     * Id
     */
    @ExcelProperty(value = "发起人主键")
    private Long originatorId;

    /**
     *
     */
    @ExcelProperty(value = "发起人名称")
    private String originatorName;

    /**
     * (create time)
     */
    @ExcelProperty(value = "抄送时间")
    private Date createTime;


    /**
     * workflow instancecreate time
     */
    @ExcelProperty(value = "流程实例创建时间")
    private Date proInsCreateTime;
}
