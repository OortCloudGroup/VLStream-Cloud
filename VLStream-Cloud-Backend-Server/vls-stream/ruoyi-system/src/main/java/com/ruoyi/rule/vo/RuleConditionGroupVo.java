package com.ruoyi.rule.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * 规则条件组，示条件的集合，是逻辑运算的中间单位，每个条件组隶属于一个规则列。视图对象 rule_condition_group
 *
 * @author ruoyi
 * @date 2024-12-20
 */
@Data
@ExcelIgnoreUnannotated
public class RuleConditionGroupVo {

    private static final long serialVersionUID = 1L;

    /**
     * 条件组ID
     */
    @ExcelProperty(value = "条件组ID")
    private String id;

    /**
     * 租户id
     */
    @ExcelProperty(value = "租户id")
    private String tenantId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * 关联的规则列表ID
     */
    @ExcelProperty(value = "关联的规则列表ID")
    private String ruleListId;

    /**
     * 条件组名称
     */
    @ExcelProperty(value = "条件组名称")
    private String name;

    /**
     * 条件组描述
     */
    @ExcelProperty(value = "条件组描述")
    private String description;

    /**
     * 或且
     */
    @ExcelProperty(value = "或or且")
    private String andOr;
}
