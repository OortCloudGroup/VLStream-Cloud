package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.CameraOsdSetting;
import com.ruoyi.vlstream.mapper.VlsCameraOsdSettingMapper;
import com.ruoyi.vlstream.service.IVlsCameraOsdSettingService;
import org.springframework.stereotype.Service;

/** Real database service for camera OSD settings. */
@Service
public class VlsCameraOsdSettingServiceImpl
    extends AbstractVlsTenantCrudService<VlsCameraOsdSettingMapper, CameraOsdSetting>
    implements IVlsCameraOsdSettingService {
}
