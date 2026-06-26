package org.springblade.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsCameraOsdSettingMapper;
import org.springblade.vlstream.pojo.entity.CameraOsdSetting;
import org.springblade.vlstream.service.IVlsCameraOsdSettingService;
import org.springframework.stereotype.Service;

/**
 * Camera OSD settings table service implementation class
 */
@Service
public class VlsCameraOsdSettingServiceImpl extends BaseServiceImpl<VlsCameraOsdSettingMapper, CameraOsdSetting> implements IVlsCameraOsdSettingService {
}
