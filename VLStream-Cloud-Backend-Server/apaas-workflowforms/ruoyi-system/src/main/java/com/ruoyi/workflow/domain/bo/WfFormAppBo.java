package com.ruoyi.workflow.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 表单应用分类业务对象 wf_form_app
 *
 * @author 雷超群
 * @date 2025-04-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormAppBo extends BaseEntity {

    /**
     * 表单分类id
     */
    private String categoryId;

    /**
     * 应用ID
     */
    @NotBlank(message = "应用ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationId;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationName;

    /**
     * 应用密钥
     */
    @NotBlank(message = "应用密钥不能为空", groups = { AddGroup.class, EditGroup.class })
    private String applicationSecret;


    /**
     * 分类父id
     */
    private String parentId;

    /**
     * 表单分类名称
     */
    private String categoryName;

    /**
     * 分类编码
     */
    private String code;

    /**
     * 0选择应用，1添加应用
     */
    private String appFlag;
    /**
     * 备注
     */
    private String remark;
    /**
     * 图标地址
     */
    private String images;
    /**
     * 0流程 1工单
     */
    private String type;
}
