/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * VLS device service contract used by compatibility controllers.
 */
public interface IVlsDeviceInfoService {

    BladePage<DeviceInfo> getDevicePage(long current, long size, String keyword, String deviceName,
                                        String status, String tag);

    DeviceInfo getDevice(Long id);

    DeviceInfo createDevice(DeviceInfo deviceInfo);

    DeviceInfo updateDevice(Long id, DeviceInfo deviceInfo);

    boolean deleteDevice(Long id);

    boolean deleteDevices(List<Long> ids);

    Map<String, Object> getDeviceStatistics();

    List<Map<String, Object>> getDeviceTree();

    Map<String, Object> testDeviceConnection(Long id);

    Map<String, Object> refreshDeviceStatus(Long id);

    Map<String, Object> batchRefreshDevices(List<Long> ids);

    List<VlsDeviceInfoMapper.TypeStatistics> getTypeStatistics();

    List<String> getAllTags();

    Map<String, Object> ptzControl(Long id, String command, Map<String, Object> params);

    Map<String, Object> getVideoStreamInfo(Long id);

    List<DeviceInfo> exportDevices(String deviceIds);

    Map<String, Object> importDevices(MultipartFile file);

    Map<String, Object> dispatchAlgorithms(Long algorithmId, String deviceIds);
}
