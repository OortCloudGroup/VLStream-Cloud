package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.CopyGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.workflow.convert.ProcessModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 流程模型对象
 *
 * @author KonBAI
 * @createTime 2022/6/21 9:16
 */
@Data
public class WfModelBo {
    /**
     * 模型主键
     */
    @NotNull(message = "模型主键不能为空", groups = {EditGroup.class})
    private String modelId;
    /**
     * 模型名称
     */
    @NotNull(message = "模型名称不能为空", groups = {AddGroup.class, EditGroup.class, CopyGroup.class})
    private String modelName;
    /**
     * 模型Key
     */
    @NotNull(message = "模型Key不能为空", groups = {AddGroup.class, EditGroup.class, CopyGroup.class})
    private String modelKey;
    /**
     * 通用流程分类
     */
    private String wfCategory;
    /**
     * 查询全部应用通用流程
     */
    private Boolean wfAppAll = false;
    /**
     * 查询全部综合通用流程
     */
    private Boolean wfSynthesisAll = false;
    /**
     * 查询全部应用工单流程
     */
    private Boolean WorkOrderAppAll = false;
    /**
     * 查询全部综合工单流程
     */
    private Boolean WorkOrderSynthesisAll = false;
    /**
     * 工单流程分类
     */
    private String WorkOrderCategory;

    /**
     * 描述
     */
    private String description;
    /**
     * 表单类型（0流式布局 1签批卡片布局）
     */
    private Integer formType;

    /**
     * 部署id
     */
    private String deploymentId;

    /**
     * 流程xml
     */
    private String bpmnXml;
    /**
     * 表单主键
     */
//    @NotNull(message = "发起表单不能为空", groups = {CopyGroup.class })
    private String formId;
    /**
     * 是否保存为新版本
     */
    private Boolean newVersion;
    /**
     * 图标id
     */
    private String iconId;

    /**
     * 手机端是否显示 0（显示） 1（不显示）
     */
    private String showMobile = "0";

    /**
     * 被复制的模型的id
     */
    @NotBlank(message = "被复制的流程主键不能为空", groups = {CopyGroup.class})
    private String copyModelId;

    /**
     * 是否全程消息推送
     */
    private Boolean notifyAllSteps;

    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 应用id
     */
    private String applicationId;

    /**
     * 流程模型数据
     */
    private ProcessModel processModel;

//    /**
//     * 开始节点的表单key
//     */
//
//    private String formKey;


    /**
     * 表单所属分类，同时标识是否需要同时创建表单
     */
    private String  categoryId;

    /**
     * 标识工单还是流程  0流程 1工单
     */
    private String type;
}
