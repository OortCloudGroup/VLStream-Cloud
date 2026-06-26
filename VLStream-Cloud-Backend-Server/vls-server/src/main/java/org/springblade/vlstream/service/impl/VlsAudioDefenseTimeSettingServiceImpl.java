package org.springblade.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsAudioDefenseTimeSettingMapper;
import org.springblade.vlstream.pojo.entity.AudioDefenseTimeSetting;
import org.springblade.vlstream.service.IVlsAudioDefenseTimeSettingService;
import org.springframework.stereotype.Service;

/**
 * Audio arming schedule setting table service implementation class
 */
@Service
public class VlsAudioDefenseTimeSettingServiceImpl extends BaseServiceImpl<VlsAudioDefenseTimeSettingMapper, AudioDefenseTimeSetting> implements IVlsAudioDefenseTimeSettingService {
}
