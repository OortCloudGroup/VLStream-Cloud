/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.EventManagement;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for VLS event management records.
 */
@Mapper
public interface VlsEventManagementMapper extends BaseMapperPlus<VlsEventManagementMapper, EventManagement, EventManagement> {
}
