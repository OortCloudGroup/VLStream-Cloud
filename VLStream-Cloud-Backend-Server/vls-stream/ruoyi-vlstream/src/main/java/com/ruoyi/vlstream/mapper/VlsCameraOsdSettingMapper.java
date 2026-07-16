/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.CameraOsdSetting;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for persisted camera OSD settings. */
@Mapper
public interface VlsCameraOsdSettingMapper extends BaseMapperPlus<VlsCameraOsdSettingMapper, CameraOsdSetting, CameraOsdSetting> {
}
