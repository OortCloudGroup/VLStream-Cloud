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

/** Compute resource specification configuration. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_resource_spec")
public class ResourceSpec extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceTypeId;
    private String specName;
    private String cpuModel;
    private Integer vcpu;
    private Integer memoryGb;
    private String gpuDesc;
    private Integer systemDiskGb;
    private Integer dataDiskGb;
    private Integer isActive;
    private Integer sortOrder;
    private String remark;
}
