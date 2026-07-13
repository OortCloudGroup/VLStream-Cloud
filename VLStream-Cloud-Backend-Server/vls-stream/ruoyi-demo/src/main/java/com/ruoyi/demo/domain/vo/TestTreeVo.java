/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;


/**
 * 测试树表视图对象 test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@ExcelIgnoreUnannotated
public class TestTreeVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 父id
     */
    @ExcelProperty(value = "父id")
    private String parentId;

    /**
     * 部门id
     */
    @ExcelProperty(value = "部门id")
    private String deptId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 树节点名
     */
    @ExcelProperty(value = "树节点名")
    private String treeName;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;


}
