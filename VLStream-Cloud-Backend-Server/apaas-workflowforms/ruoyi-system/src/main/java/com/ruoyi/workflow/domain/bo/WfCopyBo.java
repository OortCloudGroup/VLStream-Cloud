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
 * 流程抄送业务对象 wf_copy
 *
 * @author ruoyi
 * @date 2022-05-19
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfCopyBo extends BaseEntity {

    /**
     * 抄送主键
     */
    @NotNull(message = "抄送主键不能为空", groups = { EditGroup.class })
    private Long copyId;

    /**
     * 抄送标题
     */
    @NotNull(message = "抄送标题不能为空", groups = { AddGroup.class, EditGroup.class })
    private String title;

    /**
     * 流程主键
     */
    @NotBlank(message = "流程主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processId;

    /**
     * 流程名称
     */
    @NotBlank(message = "流程名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processName;

    /**
     * 流程分类主键
     */
    @NotBlank(message = "流程分类主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String categoryId;

    /**
     * 任务主键
     */
    @NotBlank(message = "任务主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String taskId;

    /**
     * 用户主键
     */
    @NotBlank(message = "用户主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private String userId;

    /**
     * 发起人Id
     */
    @NotNull(message = "发起人主键不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long originatorId;
    /**
     * 发起人名称
     */
    @NotNull(message = "发起人名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String originatorName;

    /**
     * 流程创建时间开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartBeginTime;

    /**
     * 流程创建时间结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartEndTime;

    /**
     * 查询全部应用通用流程
     */
    private Boolean wfAppAll= false;
    /**
     * 查询全部综合通用流程
     */
    private Boolean wfSynthesisAll= false;
}
