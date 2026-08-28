/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * @Description:
 * @Date: 2024-12-20
 * @Version: V1.0
 */
@Data
@TableName("rule_list")
public class RuleList implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ExcelIgnore
    private String id;
    /**
     * id
     */
    @ExcelIgnore
    private String tenantId;
    /**
     * user ID
     */
    @ExcelIgnore
    private String userId;
    /**
     * ID
     */
    @ExcelProperty("分类ID（不能为空）")
    private String treeId;
    /**
     *
     */
    @ExcelProperty("规则名称")
    private String name;
    /**
     * , AviatorScript
     */
    @ExcelProperty("规则表达式")
    private String expression;
    /**
     *
     */
    @ExcelProperty("规则描述")
    private String description;
    /**
     *
     */
    @ExcelIgnore
    private String createBy;
    /**
     * create time
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ExcelIgnore
    private java.util.Date createTime;
    /**
     * Update
     */
    @ExcelIgnore
    private String updateBy;
    /**
     * Update
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.util.Date updateTime;
    /**
     * Delete , 0 not Delete , 1 Delete
     */
    @ExcelIgnore

    private String delFlag;
    /**
     * whether , 0 not , 1
     */
    @ExcelProperty("是否生效,0表示未生效，1表示生效")
    private String status;
    /**
     *
     */
    @ExcelProperty("启动状态")
    private String enable;
    /**
     * formid
     */
    @ExcelIgnore
    private String  formId;
    /**
     * 0workflow 1work order
     */
    private String type;
}
