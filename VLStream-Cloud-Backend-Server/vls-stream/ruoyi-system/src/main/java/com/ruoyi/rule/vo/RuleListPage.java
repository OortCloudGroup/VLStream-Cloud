/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 规则列表
 * @Date: 2024-12-20
 * @Version: V1.0
 */
@Data
public class RuleListPage {

    /**
     * 规则ID
     */
    @ExcelProperty(value = "规则ID")
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
     * 关联的规则树ID
     */
    @ExcelProperty(value = "关联的规则树ID")
    private String treeId;
    /**
     * 规则名称
     */
    @ExcelProperty(value = "规则名称")
    private String name;
    /**
     * 规则表达式，使用AviatorScript语言
     */
    @ExcelProperty(value = "规则表达式")
    private String expression;
    /**
     * 规则描述
     */
    @ExcelProperty(value = "规则描述")
    private String description;
    /**
     * 创建人
     */

    private String createBy;
    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.util.Date createTime;
    /**
     * 修改人
     */
    private String updateBy;
    /**
     * 修改时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.util.Date updateTime;
    /**
     * 删除标记，0表示未删除，1表示删除
     */

    private String delFlag;
    /**
     * 是否生效，0表示未生效，1表示删除未生效
     */

    private String status;
    /**
     * 启动状态
     */

    private String enable;

    /**
     * 表单id
     */
    private Long formId;

}
