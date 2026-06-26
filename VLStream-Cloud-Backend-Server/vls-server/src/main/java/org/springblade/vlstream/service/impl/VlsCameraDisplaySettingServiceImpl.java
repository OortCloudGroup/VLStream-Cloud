package org.springblade.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsCameraDisplaySettingMapper;
import org.springblade.vlstream.pojo.entity.CameraDisplaySetting;
import org.springblade.vlstream.service.IVlsCameraDisplaySettingService;
import org.springframework.stereotype.Service;

/**
 * Camera display settings table service implementation class
 */
@Service
public class VlsCameraDisplaySettingServiceImpl extends BaseServiceImpl<VlsCameraDisplaySettingMapper, CameraDisplaySetting> implements IVlsCameraDisplaySettingService {
}
