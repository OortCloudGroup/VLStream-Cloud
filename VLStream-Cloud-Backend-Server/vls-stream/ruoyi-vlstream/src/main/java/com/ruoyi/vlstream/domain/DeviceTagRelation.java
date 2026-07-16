/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Persistent many-to-many relation between devices and tags. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_device_tag_relation")
public class DeviceTagRelation extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tagId;
}
