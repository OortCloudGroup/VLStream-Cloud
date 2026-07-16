/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.ResourceType;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for VLS resource types. */
@Mapper
public interface VlsResourceTypeMapper extends BaseMapperPlus<VlsResourceTypeMapper, ResourceType, ResourceType> {
}
