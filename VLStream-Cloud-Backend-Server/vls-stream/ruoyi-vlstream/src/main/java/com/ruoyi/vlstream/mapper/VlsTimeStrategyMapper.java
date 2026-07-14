/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.TimeStrategy;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for VLS time strategy records.
 */
@Mapper
public interface VlsTimeStrategyMapper extends BaseMapperPlus<VlsTimeStrategyMapper, TimeStrategy, TimeStrategy> {
}
