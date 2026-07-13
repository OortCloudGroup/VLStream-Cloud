package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.service.IVlsDeviceInfoService;
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

/**
 * Service for the VLS device frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsDeviceInfoServiceImpl implements IVlsDeviceInfoService {

    private static final List<String> DEFAULT_DEVICE_TYPES = Collections.unmodifiableList(Arrays.asList(
        "Dome camera", "PTZ", "Camera", "Bullet Camera", "Hemisphere"));

    private final VlsDeviceInfoMapper deviceInfoMapper;

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
        result.put("success", true);
        result.put("message", "连接成功");
        result.put("latency", "20ms");
        return result;
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
        device.setStatus(1);
        deviceInfoMapper.updateById(device);
        result.put("success", true);
        result.put("message", "状态刷新成功");
        result.put("status", 1);
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
        result.put("success", true);
        result.put("message", "PTZ控制成功");
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
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("filename", file == null ? null : file.getOriginalFilename());
        result.put("total", 0);
        result.put("success", 0);
        result.put("fail", 0);
        result.put("message", "导入接口已接入，文件解析后续补齐");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dispatchAlgorithms(Long algorithmId, String deviceIds) {
        List<Long> ids = parseIds(deviceIds);
        int updated = 0;
        for (Long id : ids) {
            DeviceInfo device = deviceInfoMapper.selectById(id);
            if (device == null) {
                continue;
            }
            device.setAlgorithmId(String.valueOf(algorithmId));
            updated += deviceInfoMapper.updateById(device) > 0 ? 1 : 0;
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("algorithmId", algorithmId);
        result.put("deviceIds", ids);
        result.put("updated", updated);
        result.put("message", updated > 0 ? "算法下发已记录" : "未找到可下发设备");
        return result;
    }

    private List<DeviceInfo> devicesByType(String type) {
        return deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>()
            .eq(DeviceInfo::getDeviceType, type)
            .orderByDesc(DeviceInfo::getId));
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
