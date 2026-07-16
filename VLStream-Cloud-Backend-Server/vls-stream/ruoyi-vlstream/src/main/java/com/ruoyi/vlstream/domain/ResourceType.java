/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Resource-provider type configuration. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_resource_type")
public class ResourceType extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private String typeCode;
    private String typeName;
    private Integer isActive;
    private Integer sortOrder;
    private String description;
}
