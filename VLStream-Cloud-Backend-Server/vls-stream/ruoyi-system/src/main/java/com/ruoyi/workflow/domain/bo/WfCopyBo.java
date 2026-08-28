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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * workflow object wf_copy
 *
 * @author ruoyi
 * @date 2022-05-19
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfCopyBo extends BaseEntity {

    /**
     * primary key
     */
    @NotNull(message = "抄送主键不能为空", groups = { EditGroup.class })
    private Long copyId;

    /**
     *
     */
    @NotNull(message = "抄送标题不能为空", groups = { AddGroup.class, EditGroup.class })
    private String title;

    /**
     * workflowprimary key
     */
    @NotBlank(message = "流程主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processId;

    /**
     * workflow
     */
    @NotBlank(message = "流程名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processName;

    /**
     * workflow primary key
     */
    @NotBlank(message = "流程分类主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String categoryId;

    /**
     * taskprimary key
     */
    @NotBlank(message = "任务主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String taskId;

    /**
     * userprimary key
     */
    @NotBlank(message = "用户主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String userId;

    /**
     * Id
     */
    @NotNull(message = "发起人主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long originatorId;
    /**
     *
     */
    @NotNull(message = "发起人名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String originatorName;

    /**
     * workflowcreate timestart
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartBeginTime;

    /**
     * workflowcreate timefinish
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartEndTime;

    /**
     * Query full workflow
     */
    private Boolean wfAppAll= false;
    /**
     * Query full workflow
     */
    private Boolean wfSynthesisAll= false;
}
