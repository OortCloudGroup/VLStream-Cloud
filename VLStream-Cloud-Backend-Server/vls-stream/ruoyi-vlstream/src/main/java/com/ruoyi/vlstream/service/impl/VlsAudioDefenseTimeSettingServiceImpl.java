/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.AudioDefenseTimeSetting;
import com.ruoyi.vlstream.mapper.VlsAudioDefenseTimeSettingMapper;
import com.ruoyi.vlstream.service.IVlsAudioDefenseTimeSettingService;
import org.springframework.stereotype.Service;

/** Real database service for audio arming schedules. */
@Service
public class VlsAudioDefenseTimeSettingServiceImpl
    extends AbstractVlsTenantCrudService<VlsAudioDefenseTimeSettingMapper, AudioDefenseTimeSetting>
    implements IVlsAudioDefenseTimeSettingService {
}
