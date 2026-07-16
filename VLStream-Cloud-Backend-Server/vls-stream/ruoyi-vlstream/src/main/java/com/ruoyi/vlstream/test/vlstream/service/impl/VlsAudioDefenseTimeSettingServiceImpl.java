/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAudioDefenseTimeSettingMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioDefenseTimeSetting;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAudioDefenseTimeSettingService;
import org.springframework.stereotype.Service;

/**
 * 音频布防时间设置表 服务实现类
 */
@Service
public class VlsAudioDefenseTimeSettingServiceImpl extends BaseServiceImpl<VlsAudioDefenseTimeSettingMapper, AudioDefenseTimeSetting> implements IVlsAudioDefenseTimeSettingService {
}
