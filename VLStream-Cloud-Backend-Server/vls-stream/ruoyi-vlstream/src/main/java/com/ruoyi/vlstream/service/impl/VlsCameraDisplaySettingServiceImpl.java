/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.CameraDisplaySetting;
import com.ruoyi.vlstream.mapper.VlsCameraDisplaySettingMapper;
import com.ruoyi.vlstream.service.IVlsCameraDisplaySettingService;
import org.springframework.stereotype.Service;

/** Real database service for camera display settings. */
@Service
public class VlsCameraDisplaySettingServiceImpl
    extends AbstractVlsTenantCrudService<VlsCameraDisplaySettingMapper, CameraDisplaySetting>
    implements IVlsCameraDisplaySettingService {
}
