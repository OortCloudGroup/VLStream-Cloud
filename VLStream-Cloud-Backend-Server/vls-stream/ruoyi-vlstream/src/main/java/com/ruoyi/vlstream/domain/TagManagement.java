/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VLS tag management entity mapped to vls_tag_management.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_tag_management")
public class TagManagement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String tagName;

    private String categoryType;

    private Integer level;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private Integer sortOrder;

    private String tagColor;

    private String tagIcon;

    private String description;

    private Integer isActive;

    private Integer usageCount;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String tagType;

    @TableField(exist = false)
    private Integer position;
}
