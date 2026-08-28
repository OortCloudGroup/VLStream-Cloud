/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;


/**
 * object test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@ExcelIgnoreUnannotated
public class TestDemoVo {

    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * department ID
     */
    @ExcelProperty(value = "部门id")
    private String deptId;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     *
     */
    @ExcelProperty(value = "排序号")
    private Integer orderNum;

    /**
     * key
     */
    @ExcelProperty(value = "key键")
    private String testKey;

    /**
     * value
     */
    @ExcelProperty(value = "值")
    private String value;

    /**
     * create time
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     *
     */
    @ExcelProperty(value = "创建人")
    private String createBy;

    /**
     * update time
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * new
     */
    @ExcelProperty(value = "更新人")
    private String updateBy;


}
