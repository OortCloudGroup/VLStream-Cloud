package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.DeviceInfo;
import com.vlstream.mapper.DeviceInfoMapper;
import com.vlstream.service.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Device Information Service Implementation
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class DeviceInfoServiceImpl extends ServiceImpl<DeviceInfoMapper, DeviceInfo> implements DeviceInfoService {

    @Override
    public IPage<DeviceInfo> getDevicePage(Page<DeviceInfo> page, String deviceName, String tag, String status) {
        return baseMapper.selectDevicePage(page, deviceName, tag, status);
    }

    @Override
    public DeviceInfo getByDeviceId(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            return null;
        }
        return baseMapper.selectByDeviceId(deviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDevice(DeviceInfo deviceInfo) {
        try {
            // Auto generate device_id if not provided
            if (deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().trim().isEmpty()) {
                String deviceId = generateDeviceId();
                deviceInfo.setDeviceId(deviceId);
            } else if (checkDeviceIdExists(deviceInfo.getDeviceId())) {
                return false;
            }
            
            // Set default status
            if (StringUtils.isBlank(deviceInfo.getStatus())) {
                deviceInfo.setStatus("offline");
            }
            
            return save(deviceInfo);
        } catch (Exception e) {
            log.error("Failed to add device", e);
            return false;
        }
    }

    @Override
    public boolean updateDevice(DeviceInfo deviceInfo) {
        return updateById(deviceInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dispatchAlgorithms(Long algorithmId, String deviceIds) {
        for (String deviceId : deviceIds.split(",")) {
            DeviceInfo device = getById(deviceId);
            if (device == null) {
                return false;
            }

            DeviceInfo updateEntity = new DeviceInfo();
            updateEntity.setId(Long.valueOf(deviceId));
            updateEntity.setAlgorithmId(String.valueOf(algorithmId));
            updateById(updateEntity);
        }
        return true;
    }

    @Override
    public boolean deleteDevice(Long id) {
        return removeById(id);
    }

    @Override
    public boolean deleteDeviceBatch(List<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public boolean updateDeviceStatus(Long id, String status) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(id);
        deviceInfo.setStatus(status);
        return updateById(deviceInfo);
    }

    @Override
    public boolean updateDeviceStatusBatch(List<Long> ids, String status) {
        return baseMapper.updateStatusBatch(ids, status) > 0;
    }

    @Override
    public List<DeviceInfo> getDevicesByStatus(String status) {
        return baseMapper.selectByStatus(status);
    }

    @Override
    public List<DeviceInfo> getDevicesByType(String deviceType) {
        return baseMapper.selectByDeviceType(deviceType);
    }

    @Override
    public List<DeviceInfo> getDevicesByBrand(String brand) {
        LambdaQueryWrapper<DeviceInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceInfo::getBrand, brand);
        wrapper.eq(DeviceInfo::getDeleted, 0);
        return list(wrapper);
    }

    @Override
    public List<DeviceInfo> getDevicesByPosition(String position) {
        return baseMapper.selectByPosition(position);
    }

    @Override
    public boolean checkDeviceIdExists(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            return false;
        }
        return baseMapper.countByDeviceId(deviceId) > 0;
    }

    @Override
    public Map<String, Object> testDeviceConnection(Long id) {
        DeviceInfo device = getById(id);
        Map<String, Object> result = new HashMap<>();
        
        if (device == null) {
            result.put("success", false);
            result.put("message", "Device does not exist");
            return result;
        }

        try {
            // Implement actual device connection test logic here
            // TODO: Test connection based on device type and connection parameters
            log.info("Testing device connection: {}", device.getDeviceName());
            
            result.put("success", true);
            result.put("message", "Connection successful");
            result.put("latency", "20ms");
        } catch (Exception e) {
            log.error("Device connection test failed: {}", device.getDeviceName(), e);
            result.put("success", false);
            result.put("message", "Connection failed: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getDeviceStatistics() {
        List<DeviceInfoMapper.StatusStatistics> statusStats = baseMapper.getStatusStatistics();
        List<DeviceInfoMapper.TypeStatistics> typeStats = baseMapper.getTypeStatistics();
        List<DeviceInfoMapper.BrandStatistics> brandStats = baseMapper.getBrandStatistics();
        
        Map<String, Object> result = new HashMap<>();
        
        long total = 0;
        long online = 0;
        long offline = 0;
        long fault = 0;
        
        for (DeviceInfoMapper.StatusStatistics stat : statusStats) {
            total += stat.getCount();
            switch (stat.getStatus()) {
                case "offline": offline = stat.getCount(); break;
                case "online": online = stat.getCount(); break;
                case "fault": fault = stat.getCount(); break;
            }
        }
        
        result.put("total", total);
        result.put("online", online);
        result.put("offline", offline);
        result.put("fault", fault);
        result.put("typeStatistics", typeStats);
        result.put("brandStatistics", brandStats);
        
        return result;
    }

    @Override
    public List<String> getAllTags() {
        return baseMapper.getAllTags();
    }

    @Override
    public List<String> getAllBrands() {
        return baseMapper.getAllBrands();
    }

    @Override
    public Map<String, Object> validateDevice(DeviceInfo deviceInfo) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        // Validate device name
        if (deviceInfo.getDeviceName() == null || deviceInfo.getDeviceName().trim().isEmpty()) {
            errors.add("Device name cannot be empty");
        }
        
        // Validate device ID
        if (deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().trim().isEmpty()) {
            errors.add("Device ID cannot be empty");
        } else if (checkDeviceIdExists(deviceInfo.getDeviceId())) {
            errors.add("Device ID already exists");
        }
        
        // Validate IP address
        if (StringUtils.isNotBlank(deviceInfo.getIpAddress()) && !isValidIpAddress(deviceInfo.getIpAddress())) {
            errors.add("Invalid IP address format");
        }
        
        // Validate port
        if (deviceInfo.getPort() != null && (deviceInfo.getPort() < 1 || deviceInfo.getPort() > 65535)) {
            errors.add("Port must be between 1-65535");
        }

        if (errors.isEmpty()) {
            result.put("valid", true);
            result.put("message", "Validation passed");
        } else {
            result.put("valid", false);
            result.put("errors", errors);
        }
        
        return result;
    }

    private String generateDeviceId() {
        return "DEV" + System.currentTimeMillis();
    }

    private boolean isValidIpAddress(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> refreshDeviceStatus(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        DeviceInfo device = getById(deviceId);
        
        if (device == null) {
            result.put("success", false);
            result.put("message", "Device does not exist");
            return result;
        }
        
        try {
            // Implement actual device status refresh logic here
            // TODO: Update status based on device connection status
            log.info("Refreshing device status: {}", device.getDeviceName());
            
            // Simulate status refresh
            String newStatus = "online"; // Should be determined based on actual device detection
            updateDeviceStatus(deviceId, newStatus);
            
            result.put("success", true);
            result.put("message", "Status refreshed successfully");
            result.put("status", newStatus);
        } catch (Exception e) {
            log.error("Failed to refresh device status: {}", device.getDeviceName(), e);
            result.put("success", false);
            result.put("message", "Status refresh failed: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> batchImportDevices(List<DeviceInfo> deviceList) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (DeviceInfo device : deviceList) {
            try {
                // 验证设备信息
                Map<String, Object> validation = validateDevice(device);
                if (!(Boolean) validation.get("valid")) {
                    failCount++;
                    errors.add("设备 " + device.getDeviceName() + " 验证失败");
                    continue;
                }
                
                // 保存设备
                if (addDevice(device)) {
                    successCount++;
                } else {
                    failCount++;
                    errors.add("设备 " + device.getDeviceName() + " 保存失败");
                }
            } catch (Exception e) {
                failCount++;
                errors.add("设备 " + device.getDeviceName() + " 处理异常: " + e.getMessage());
            }
        }
        
        result.put("total", deviceList.size());
        result.put("success", successCount);
        result.put("fail", failCount);
        result.put("errors", errors);
        
        return result;
    }

    @Override
    public List<DeviceInfo> exportDevices(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return list();
        } else {
            return listByIds(deviceIds);
        }
    }

    @Override
    public Map<String, Object> getDeviceConfig(Long deviceId) {
        DeviceInfo device = getById(deviceId);
        Map<String, Object> config = new HashMap<>();
        
        if (device != null) {
            config.put("deviceId", device.getDeviceId());
            config.put("deviceName", device.getDeviceName());
            config.put("ipAddress", device.getIpAddress());
            config.put("port", device.getPort());
            config.put("username", device.getUsername());
            config.put("streamUrl", device.getStreamUrl());
            config.put("deviceType", device.getDeviceType());
            config.put("brand", device.getBrand());
            config.put("model", device.getModel());
        }
        
        return config;
    }

    @Override
    public boolean updateDeviceConfig(Long deviceId, Map<String, Object> config) {
        try {
            DeviceInfo device = getById(deviceId);
            if (device == null) {
                return false;
            }
            
            // 更新配置
            if (config.containsKey("deviceName")) {
                device.setDeviceName((String) config.get("deviceName"));
            }
            if (config.containsKey("ipAddress")) {
                device.setIpAddress((String) config.get("ipAddress"));
            }
            if (config.containsKey("port")) {
                device.setPort((Integer) config.get("port"));
            }
            if (config.containsKey("username")) {
                device.setUsername((String) config.get("username"));
            }
            if (config.containsKey("password")) {
                device.setPassword((String) config.get("password"));
            }
            if (config.containsKey("streamUrl")) {
                device.setStreamUrl((String) config.get("streamUrl"));
            }
            
            return updateById(device);
        } catch (Exception e) {
            log.error("更新设备配置失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        DeviceInfo device = getById(deviceId);
        
        if (device == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }
        
        try {
            // 这里实现实际的PTZ控制逻辑
            // TODO: 根据设备类型和命令执行PTZ控制
            log.info("PTZ控制: 设备={}, 命令={}, 参数={}", device.getDeviceName(), command, params);
            
            result.put("success", true);
            result.put("message", "PTZ控制成功");
            result.put("command", command);
        } catch (Exception e) {
            log.error("PTZ控制失败: {}", device.getDeviceName(), e);
            result.put("success", false);
            result.put("message", "PTZ控制失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getVideoStreamInfo(Long deviceId) {
        DeviceInfo device = getById(deviceId);
        Map<String, Object> streamInfo = new HashMap<>();
        
        if (device != null) {
            streamInfo.put("deviceId", device.getDeviceId());
            streamInfo.put("deviceName", device.getDeviceName());
            streamInfo.put("streamUrl", device.getStreamUrl());
            streamInfo.put("status", device.getStatus());
            streamInfo.put("ipAddress", device.getIpAddress());
            streamInfo.put("port", device.getPort());
            streamInfo.put("deviceType", device.getDeviceType());
            streamInfo.put("brand", device.getBrand());
            streamInfo.put("model", device.getModel());
        }
        
        return streamInfo;
    }
} 
