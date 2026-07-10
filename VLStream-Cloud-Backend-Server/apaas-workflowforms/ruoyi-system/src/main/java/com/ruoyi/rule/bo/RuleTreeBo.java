package com.ruoyi.rule.bo;

import com.ruoyi.common.core.domain.TreeEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 规则树业务对象 rule_tree
 *
 * @author 雷超群
 * @date 2024-12-18
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleTreeBo extends TreeEntity<RuleTreeBo> {

    private String id;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 规则树名称
     */
    @NotBlank(message = "规则树名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 规则树描述
     */
    private String description;
    /**
     * 0流程 1工单
     */
    private String type;

}
