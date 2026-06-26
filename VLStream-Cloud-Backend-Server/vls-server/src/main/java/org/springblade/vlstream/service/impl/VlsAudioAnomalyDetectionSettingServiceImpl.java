package org.springblade.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsAudioAnomalyDetectionSettingMapper;
import org.springblade.vlstream.pojo.entity.AudioAnomalyDetectionSetting;
import org.springblade.vlstream.service.IVlsAudioAnomalyDetectionSettingService;
import org.springframework.stereotype.Service;

/**
 * Audio exception detection settings table service implementation class
 */
@Service
public class VlsAudioAnomalyDetectionSettingServiceImpl extends BaseServiceImpl<VlsAudioAnomalyDetectionSettingMapper, AudioAnomalyDetectionSetting> implements IVlsAudioAnomalyDetectionSettingService {
}
