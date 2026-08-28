/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class TreeEntity<T> extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * menu name
     */
    @TableField(exist = false)
    private String parentName;

    /**
     * menu ID
     */
    private String parentId;

    /**
     * menucode
     */
    @TableField(exist = false) // field
    private String oortPdcode;

    /**
     * sub department
     */
    @TableField(exist = false)
    private List<T> children = new ArrayList<>();

}
