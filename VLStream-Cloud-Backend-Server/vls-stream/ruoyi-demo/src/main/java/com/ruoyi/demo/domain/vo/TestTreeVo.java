/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;


/**
 * object test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@ExcelIgnoreUnannotated
public class TestTreeVo {

    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    private Long id;

    /**
     * id
     */
    @ExcelProperty(value = "父id")
    private String parentId;

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
     * node
     */
    @ExcelProperty(value = "树节点名")
    private String treeName;

    /**
     * create time
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;


}
