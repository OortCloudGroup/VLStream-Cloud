/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tenant-owned platform logo configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_platform_logo")
public class SysPlatformLogo extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @JsonIgnore
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String tenantId;

    private Long ossId;

    private String description;

    private Boolean active;

    @TableLogic
    private Integer isDeleted;
}
