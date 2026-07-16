/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.vlstream.domain.DeviceTagRelation;

import java.util.List;
import java.util.Map;

/** Service contract for persistent device/tag relations. */
public interface IVlsDeviceTagRelationService extends IService<DeviceTagRelation> {

    List<Map<String, Object>> getDeviceTags(Long deviceId);

    boolean setDeviceTags(Long deviceId, List<Long> tagIds);

    boolean addDeviceTags(Long deviceId, List<Long> tagIds);

    boolean removeDeviceTags(Long deviceId, List<Long> tagIds);

    boolean clearDeviceTags(Long deviceId);

    boolean copyDeviceTags(Long sourceId, List<Long> targetDeviceIds);

    Map<String, Object> getDeviceTagDetails(Long deviceId);
}
