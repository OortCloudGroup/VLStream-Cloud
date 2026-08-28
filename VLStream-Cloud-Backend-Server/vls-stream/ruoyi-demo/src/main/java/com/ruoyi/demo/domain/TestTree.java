/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * object test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_tree")
public class TestTree extends TreeEntity<TestTree> {

    private static final long serialVersionUID = 1L;


    /**
     * primary key
     */
    @TableId(value = "id")
    private Long id;

    /**
     * department ID
     */
    private String  deptId;

    /**
     * user ID
     */
    private Long userId;

    /**
     * node
     */
    private String treeName;

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
