/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.domain.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * object test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
public class TestDemoImportVo {

    /**
     * department ID
     */
    @NotNull(message = "部门id不能为空")
    @ExcelProperty(value = "部门id")
    private String deptId;

    /**
     * user ID
     */
    @NotNull(message = "用户id不能为空")
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     *
     */
    @NotNull(message = "排序号不能为空")
    @ExcelProperty(value = "排序号")
    private Long orderNum;

    /**
     * key
     */
    @NotBlank(message = "key键不能为空")
    @ExcelProperty(value = "key键")
    private String testKey;

    /**
     * value
     */
    @NotBlank(message = "值不能为空")
    @ExcelProperty(value = "值")
    private String value;

}
