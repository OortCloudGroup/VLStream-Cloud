package org.springblade.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsAudioLinkageModeSettingMapper;
import org.springblade.vlstream.pojo.entity.AudioLinkageModeSetting;
import org.springblade.vlstream.service.IVlsAudioLinkageModeSettingService;
import org.springframework.stereotype.Service;

/**
 * Audio linkage mode settings table service implementation class
 */
@Service
public class VlsAudioLinkageModeSettingServiceImpl extends BaseServiceImpl<VlsAudioLinkageModeSettingMapper, AudioLinkageModeSetting> implements IVlsAudioLinkageModeSettingService {
}
