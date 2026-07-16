/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.AudioAnomalyDetectionSetting;
import com.ruoyi.vlstream.mapper.VlsAudioAnomalyDetectionSettingMapper;
import com.ruoyi.vlstream.service.IVlsAudioAnomalyDetectionSettingService;
import org.springframework.stereotype.Service;

/** Real database service for audio anomaly-detection settings. */
@Service
public class VlsAudioAnomalyDetectionSettingServiceImpl
    extends AbstractVlsTenantCrudService<VlsAudioAnomalyDetectionSettingMapper, AudioAnomalyDetectionSetting>
    implements IVlsAudioAnomalyDetectionSettingService {
}
