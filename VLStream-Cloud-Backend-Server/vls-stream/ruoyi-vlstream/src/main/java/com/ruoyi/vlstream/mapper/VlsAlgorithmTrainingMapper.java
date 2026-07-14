/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for VLS algorithm training tasks.
 */
@Mapper
public interface VlsAlgorithmTrainingMapper extends BaseMapperPlus<VlsAlgorithmTrainingMapper, AlgorithmTraining, AlgorithmTraining> {
}
