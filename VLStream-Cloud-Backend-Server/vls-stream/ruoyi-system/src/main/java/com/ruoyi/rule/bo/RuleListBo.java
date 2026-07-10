package com.ruoyi.rule.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 规则列业务对象 rule_list
 *
 * @author ruoyi
 * @date 2024-12-18
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleListBo extends BaseEntity {

    private String id;

    /**
     * 关联的规则树ID
     */
    @NotBlank(message = "关联的规则树ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String treeId;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 规则表达式，使用AviatorScript语言
     */
    private String expression;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 规则状态，ACTIVE表示生效，INACTIVE表示无效
     */
    @NotBlank(message = "规则状态，ACTIVE表示生效，INACTIVE表示无效不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 表单id
     */
    private String formId;
    /**
     * 0流程 1工单
     */
    private String type;
}
