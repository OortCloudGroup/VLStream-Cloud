/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.SceneGovernance;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for VLS scene governance records.
 */
@Mapper
public interface VlsSceneGovernanceMapper extends BaseMapperPlus<VlsSceneGovernanceMapper, SceneGovernance, SceneGovernance> {
}
