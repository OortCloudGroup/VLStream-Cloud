/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.DeviceTagRelation;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for persistent device/tag relations. */
@Mapper
public interface VlsDeviceTagRelationMapper extends BaseMapperPlus<VlsDeviceTagRelationMapper, DeviceTagRelation, DeviceTagRelation> {
}
