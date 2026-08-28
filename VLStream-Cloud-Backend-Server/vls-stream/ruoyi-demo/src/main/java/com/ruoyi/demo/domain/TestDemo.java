/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * object test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_demo")
public class TestDemo extends BaseEntity {

    private static final long serialVersionUID = 1L;


    /**
     * primary key
     */
    @TableId(value = "id")
    private Long id;

    /**
     * department ID
     */
    private String deptId;

    /**
     * user ID
     */
    private Long userId;

    /**
     *
     */
    @OrderBy(asc = false, sort = 1)
    private Integer orderNum;

    /**
     * key
     */
    private String testKey;

    /**
     * value
     */
    private String value;

    /**
     *
     */
    @Version
    private Long version;

    /**
     * Delete
     */
    @TableLogic
    private Long delFlag;

}
