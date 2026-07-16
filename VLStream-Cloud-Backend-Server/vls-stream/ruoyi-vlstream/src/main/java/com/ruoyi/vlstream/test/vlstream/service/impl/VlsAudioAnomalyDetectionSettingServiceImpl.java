/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAudioAnomalyDetectionSettingMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioAnomalyDetectionSetting;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAudioAnomalyDetectionSettingService;
import org.springframework.stereotype.Service;

/**
 * 音频异常侦测设置表 服务实现类
 */
@Service
public class VlsAudioAnomalyDetectionSettingServiceImpl extends BaseServiceImpl<VlsAudioAnomalyDetectionSettingMapper, AudioAnomalyDetectionSetting> implements IVlsAudioAnomalyDetectionSettingService {
}
