package com.ruoyi.rule.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规则树对象 rule_tree
 *
 * @author 雷超群
 * @date 2024-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_tree")
public class RuleTree extends TreeEntity {
    private static final long serialVersionUID = 1L;

    /** 规则树ID */
    @TableId(value = "id",type = IdType.ASSIGN_ID)
    private String id;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 租户ID
     */
    private String tenantId;
    /** 规则树名称 */
    @ExcelProperty("规则树名称")
    private String name;

    /** 规则树描述 */
    @ExcelProperty("规则树描述")
    private String description;

    /** 删除标记，0表示未删除，1表示删除 */
    private String delFlag;
    /**
     * 0流程 1工单
     */
    private String type;
}
