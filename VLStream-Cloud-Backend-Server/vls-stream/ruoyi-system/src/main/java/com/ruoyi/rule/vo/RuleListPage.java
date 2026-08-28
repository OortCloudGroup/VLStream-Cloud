/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.rule.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description:
 * @Date: 2024-12-20
 * @Version: V1.0
 */
@Data
public class RuleListPage {

    /**
     * ID
     */
    @ExcelProperty(value = "规则ID")
    private String id;
    /**
     * id
     */

    private String tenantId;
    /**
     * user ID
     */

    private String userId;
    /**
     * ID
     */
    @ExcelProperty(value = "关联的规则树ID")
    private String treeId;
    /**
     *
     */
    @ExcelProperty(value = "规则名称")
    private String name;
    /**
     * , AviatorScript
     */
    @ExcelProperty(value = "规则表达式")
    private String expression;
    /**
     *
     */
    @ExcelProperty(value = "规则描述")
    private String description;
    /**
     *
     */

    private String createBy;
    /**
     * create time
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.util.Date createTime;
    /**
     * Update
     */
    private String updateBy;
    /**
     * Update
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.util.Date updateTime;
    /**
     * Delete , 0 not Delete , 1 Delete
     */

    private String delFlag;
    /**
     * whether , 0 not , 1 Delete not
     */

    private String status;
    /**
     *
     */

    private String enable;

    /**
     * formid
     */
    private Long formId;

}
