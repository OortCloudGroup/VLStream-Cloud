/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AnalysisRequest;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for intelligent-analysis requests. */
@Mapper
public interface VlsAnalysisRequestMapper extends BaseMapperPlus<VlsAnalysisRequestMapper, AnalysisRequest, AnalysisRequest> {
}
