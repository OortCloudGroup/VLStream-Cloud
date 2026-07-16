package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.AudioLinkageModeSetting;
import com.ruoyi.vlstream.mapper.VlsAudioLinkageModeSettingMapper;
import com.ruoyi.vlstream.service.IVlsAudioLinkageModeSettingService;
import org.springframework.stereotype.Service;

/** Real database service for audio linkage-mode settings. */
@Service
public class VlsAudioLinkageModeSettingServiceImpl
    extends AbstractVlsTenantCrudService<VlsAudioLinkageModeSettingMapper, AudioLinkageModeSetting>
    implements IVlsAudioLinkageModeSettingService {
}
