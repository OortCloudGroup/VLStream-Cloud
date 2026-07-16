/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AudioAnomalyDetectionSetting;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for persisted audio anomaly-detection settings. */
@Mapper
public interface VlsAudioAnomalyDetectionSettingMapper extends BaseMapperPlus<VlsAudioAnomalyDetectionSettingMapper, AudioAnomalyDetectionSetting, AudioAnomalyDetectionSetting> {
}
