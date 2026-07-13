/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.rule.domain;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @Description: 规则列表
 * @Date: 2024-12-20
 * @Version: V1.0
 */
@Data
@TableName("rule_list")
public class RuleList implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ExcelIgnore
    private String id;
    /**
     * 租户id
     */
    @ExcelIgnore
    private String tenantId;
    /**
     * 用户id
     */
    @ExcelIgnore
    private String userId;
    /**
     * 关联的规则树ID
     */
    @ExcelProperty("分类ID（不能为空）")
    private String treeId;
    /**
     * 规则名称
     */
    @ExcelProperty("规则名称")
    private String name;
    /**
     * 规则表达式，使用AviatorScript语言
     */
    @ExcelProperty("规则表达式")
    private String expression;
    /**
     * 规则描述
     */
    @ExcelProperty("规则描述")
    private String description;
    /**
     * 创建人
     */
    @ExcelIgnore
    private String createBy;
    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ExcelIgnore
    private java.util.Date createTime;
    /**
     * 修改人
     */
    @ExcelIgnore
    private String updateBy;
    /**
     * 修改时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.util.Date updateTime;
    /**
     * 删除标记，0表示未删除，1表示删除
     */
    @ExcelIgnore

    private String delFlag;
    /**
     * 是否生效，0表示未生效，1表示生效
     */
    @ExcelProperty("是否生效,0表示未生效，1表示生效")
    private String status;
    /**
     * 启动状态
     */
    @ExcelProperty("启动状态")
    private String enable;
    /**
     * 表单id
     */
    @ExcelIgnore
    private String  formId;
    /**
     * 0流程 1工单
     */
    private String type;
}
