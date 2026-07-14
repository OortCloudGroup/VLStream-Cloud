/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for VLS algorithm annotation tasks.
 */
@Mapper
public interface VlsAlgorithmAnnotationMapper extends BaseMapperPlus<VlsAlgorithmAnnotationMapper, AlgorithmAnnotation, AlgorithmAnnotation> {
}
