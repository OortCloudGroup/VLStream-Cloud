/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.service.VlsAlgorithmDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.Socket;
import java.net.InetSocketAddress;

/**
 * Service for the VLS device frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsDeviceInfoServiceImpl implements IVlsDeviceInfoService {

    private static final List<String> DEFAULT_DEVICE_TYPES = Collections.unmodifiableList(Arrays.asList(
        "Dome camera", "PTZ", "Camera", "Bullet Camera", "Hemisphere"));

    private final VlsDeviceInfoMapper deviceInfoMapper;
    private final VlsAlgorithmDispatchService algorithmDispatchService;

    @Override
    public BladePage<DeviceInfo> getDevicePage(long current, long size, String keyword, String deviceName,
                                               String status, String tag) {
        Page<DeviceInfo> page = new Page<DeviceInfo>(current, size);
        LambdaQueryWrapper<DeviceInfo> queryWrapper = new LambdaQueryWrapper<DeviceInfo>();
        String search = firstText(keyword, deviceName);

        if (StringUtils.hasText(search)) {
            final String trimmed = search.trim();
            queryWrapper.and(wrapper -> wrapper
                .like(DeviceInfo::getDeviceName, trimmed)
                .or()
                .like(DeviceInfo::getDeviceId, trimmed));
        }
        if (StringUtils.hasText(status)) {
            Integer statusValue = toInteger(status);
            if (statusValue != null) {
                queryWrapper.eq(DeviceInfo::getStatus, statusValue);
            }
        }
        if (StringUtils.hasText(tag)) {
            queryWrapper.eq(DeviceInfo::getDeviceType, tag.trim());
        }

        queryWrapper.orderByDesc(DeviceInfo::getId);
        Page<DeviceInfo> result = deviceInfoMapper.selectPage(page, queryWrapper);
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public DeviceInfo getDevice(Long id) {
        return deviceInfoMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceInfo createDevice(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        if (!StringUtils.hasText(deviceInfo.getDeviceId())) {
            deviceInfo.setDeviceId("DEV" + System.currentTimeMillis());
        } else if (deviceInfoMapper.countByDeviceId(deviceInfo.getDeviceId()) > 0) {
            throw new IllegalArgumentException("设备编号已存在");
        }
        normalizeDefaults(deviceInfo);
        deviceInfoMapper.insert(deviceInfo);
        return deviceInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceInfo updateDevice(Long id, DeviceInfo deviceInfo) {
        DeviceInfo existing = deviceInfoMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        copyNonNull(deviceInfo, existing);
        existing.setId(id);
        deviceInfoMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean deleteDevice(Long id) {
        return deviceInfoMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteDevices(List<Long> ids) {
        return ids != null && !ids.isEmpty() && deviceInfoMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public Map<String, Object> getDeviceStatistics() {
        List<DeviceInfo> devices = deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>());
        long online = 0L;
        long offline = 0L;
        long fault = 0L;
        for (DeviceInfo device : devices) {
            Integer status = device.getStatus();
            if (Integer.valueOf(0).equals(status)) {
                online++;
            } else if (Integer.valueOf(2).equals(status)) {
                fault++;
            } else {
                offline++;
            }
        }

        Map<String, Object> statistics = new LinkedHashMap<String, Object>();
        statistics.put("total", (long) devices.size());
        statistics.put("totalCount", (long) devices.size());
        statistics.put("online", online);
        statistics.put("onlineCount", online);
        statistics.put("offline", offline);
        statistics.put("offlineCount", offline);
        statistics.put("fault", fault);
        statistics.put("faultCount", fault);
        statistics.put("typeStatistics", getTypeStatistics());
        statistics.put("brandStatistics", Collections.emptyList());
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getDeviceTree() {
        List<String> types = new ArrayList<String>(DEFAULT_DEVICE_TYPES);
        for (String type : getAllTags()) {
            if (!types.contains(type)) {
                types.add(type);
            }
        }

        List<Map<String, Object>> tree = new ArrayList<Map<String, Object>>();
        for (String type : types) {
            List<DeviceInfo> devices = devicesByType(type);
            Map<String, Object> typeNode = new LinkedHashMap<String, Object>();
            typeNode.put("id", "type_" + type);
            typeNode.put("label", type + " (" + devices.size() + ")");
            typeNode.put("type", "device_type");
            typeNode.put("deviceType", type);
            typeNode.put("deviceCount", devices.size());
            typeNode.put("children", deviceNodes(devices));
            tree.add(typeNode);
        }
        return tree;
    }

    @Override
    public Map<String, Object> testDeviceConnection(Long id) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }
        return probeDevice(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshDeviceStatus(Long id) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }
        Map<String, Object> probe = probeDevice(device);
        boolean reachable = Boolean.TRUE.equals(probe.get("success"));
        device.setStatus(reachable ? 0 : 1);
        deviceInfoMapper.updateById(device);
        result.putAll(probe);
        result.put("status", device.getStatus());
        result.put("message", reachable ? "设备连接探测成功，状态已更新为在线"
            : "设备连接探测失败，状态已更新为离线: " + probe.get("message"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchRefreshDevices(List<Long> ids) {
        int successCount = 0;
        if (ids != null) {
            for (Long id : ids) {
                Map<String, Object> result = refreshDeviceStatus(id);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    successCount++;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", ids == null ? 0 : ids.size());
        result.put("success", successCount);
        result.put("fail", ids == null ? 0 : ids.size() - successCount);
        result.put("operationSuccess", ids != null && !ids.isEmpty() && successCount == ids.size());
        if (ids == null || ids.isEmpty()) {
            result.put("message", "设备 ID 列表不能为空");
        } else if (successCount != ids.size()) {
            result.put("message", "部分或全部设备连接探测失败");
        }
        return result;
    }

    @Override
    public List<VlsDeviceInfoMapper.TypeStatistics> getTypeStatistics() {
        return deviceInfoMapper.getTypeStatistics();
    }

    @Override
    public List<String> getAllTags() {
        return deviceInfoMapper.getAllTags();
    }

    @Override
    public Map<String, Object> ptzControl(Long id, String command, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }
        result.put("success", false);
        result.put("message", "当前后端未配置真实 ONVIF/PTZ 控制通道，未向设备下发命令");
        result.put("command", command);
        result.put("params", params == null ? Collections.emptyMap() : params);
        return result;
    }

    @Override
    public Map<String, Object> getVideoStreamInfo(Long id) {
        Map<String, Object> streamInfo = new LinkedHashMap<String, Object>();
        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            return streamInfo;
        }
        streamInfo.put("deviceId", device.getDeviceId());
        streamInfo.put("deviceName", device.getDeviceName());
        streamInfo.put("streamUrl", device.getStreamUrl());
        streamInfo.put("status", device.getStatus());
        streamInfo.put("deviceType", device.getDeviceType());
        return streamInfo;
    }

    @Override
    public List<DeviceInfo> exportDevices(String deviceIds) {
        List<Long> ids = parseIds(deviceIds);
        if (ids.isEmpty()) {
            return deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>().orderByDesc(DeviceInfo::getId));
        }
        return deviceInfoMapper.selectBatchIds(ids);
    }

    @Override
    public Map<String, Object> importDevices(MultipartFile file) {
        throw new UnsupportedOperationException("设备导入尚未实现文件解析，未写入任何设备数据");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dispatchAlgorithms(Long algorithmId, String deviceIds) {
        List<Long> ids = parseIds(deviceIds);
        return algorithmDispatchService.dispatch(algorithmId, ids);
    }

    private List<DeviceInfo> devicesByType(String type) {
        return deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>()
            .eq(DeviceInfo::getDeviceType, type)
            .orderByDesc(DeviceInfo::getId));
    }

    /** Perform a real TCP reachability probe against the configured stream endpoint. */
    private Map<String, Object> probeDevice(DeviceInfo device) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (device == null || !StringUtils.hasText(device.getStreamUrl())) {
            result.put("success", false);
            result.put("message", "设备未配置视频流地址");
            return result;
        }
        Socket socket = new Socket();
        long startedAt = System.currentTimeMillis();
        try {
            URI uri = URI.create(device.getStreamUrl().trim());
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                throw new IllegalArgumentException("视频流地址缺少主机名");
            }
            int port = uri.getPort() > 0 ? uri.getPort() : defaultPort(uri.getScheme());
            socket.connect(new InetSocketAddress(host, port), 5000);
            long latency = System.currentTimeMillis() - startedAt;
            result.put("success", true);
            result.put("message", "设备流端点 TCP 连接成功");
            result.put("latency", latency + "ms");
            result.put("host", host);
            result.put("port", port);
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "设备流端点连接失败: " + ex.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
                // Socket close failure does not change the completed probe result.
            }
        }
        return result;
    }

    /** Resolve the conventional TCP port for supported stream URL schemes. */
    private int defaultPort(String scheme) {
        if ("rtsp".equalsIgnoreCase(scheme) || "rtsps".equalsIgnoreCase(scheme)) {
            return 554;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        throw new IllegalArgumentException("不支持的视频流协议: " + scheme);
    }

    private List<Map<String, Object>> deviceNodes(List<DeviceInfo> devices) {
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        for (DeviceInfo device : devices) {
            Map<String, Object> node = new LinkedHashMap<String, Object>();
            node.put("id", "device_" + device.getId());
            node.put("label", device.getDeviceName());
            node.put("type", "device");
            node.put("deviceId", device.getId());
            node.put("deviceCode", device.getDeviceId());
            node.put("deviceName", device.getDeviceName());
            node.put("status", device.getStatus());
            node.put("streamUrl", device.getStreamUrl());
            node.put("deviceType", device.getDeviceType());
            nodes.add(node);
        }
        return nodes;
    }

    private void normalizeDefaults(DeviceInfo deviceInfo) {
        if (deviceInfo.getStatus() == null) {
            deviceInfo.setStatus(1);
        }
        if (deviceInfo.getIsPublic() == null) {
            deviceInfo.setIsPublic(1);
        }
        if (deviceInfo.getIsDeleted() == null) {
            deviceInfo.setIsDeleted(0);
        }
    }

    private void copyNonNull(DeviceInfo source, DeviceInfo target) {
        if (source == null) {
            return;
        }
        target.setTenantId(source.getTenantId() == null ? target.getTenantId() : source.getTenantId());
        target.setDeviceName(source.getDeviceName() == null ? target.getDeviceName() : source.getDeviceName());
        target.setDeviceId(source.getDeviceId() == null ? target.getDeviceId() : source.getDeviceId());
        target.setStreamUrl(source.getStreamUrl() == null ? target.getStreamUrl() : source.getStreamUrl());
        target.setImagePath(source.getImagePath() == null ? target.getImagePath() : source.getImagePath());
        target.setDeviceType(source.getDeviceType() == null ? target.getDeviceType() : source.getDeviceType());
        target.setRemark(source.getRemark() == null ? target.getRemark() : source.getRemark());
        target.setLongitude(source.getLongitude() == null ? target.getLongitude() : source.getLongitude());
        target.setLatitude(source.getLatitude() == null ? target.getLatitude() : source.getLatitude());
        target.setHeightPosition(source.getHeightPosition() == null ? target.getHeightPosition() : source.getHeightPosition());
        target.setAddress(source.getAddress() == null ? target.getAddress() : source.getAddress());
        target.setRegion(source.getRegion() == null ? target.getRegion() : source.getRegion());
        target.setCreator(source.getCreator() == null ? target.getCreator() : source.getCreator());
        target.setTag(source.getTag() == null ? target.getTag() : source.getTag());
        target.setAlgorithmId(source.getAlgorithmId() == null ? target.getAlgorithmId() : source.getAlgorithmId());
        target.setPushUrl(source.getPushUrl() == null ? target.getPushUrl() : source.getPushUrl());
        target.setIsPublic(source.getIsPublic() == null ? target.getIsPublic() : source.getIsPublic());
        target.setStatus(source.getStatus() == null ? target.getStatus() : source.getStatus());
        target.setIsDeleted(source.getIsDeleted() == null ? target.getIsDeleted() : source.getIsDeleted());
    }

    private List<Long> parseIds(String value) {
        List<Long> ids = new ArrayList<Long>();
        if (!StringUtils.hasText(value)) {
            return ids;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String text = part == null ? null : part.trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            ids.add(Long.valueOf(text));
        }
        return ids;
    }

    private Integer toInteger(String value) {
        try {
            return StringUtils.hasText(value) ? Integer.valueOf(value.trim()) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
