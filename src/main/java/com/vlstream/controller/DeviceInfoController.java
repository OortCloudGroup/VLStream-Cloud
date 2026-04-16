package com.vlstream.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.DeviceInfo;
import com.vlstream.entity.DeviceTagRelation;
import com.vlstream.service.DeviceInfoService;
import com.vlstream.service.DeviceTagRelationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Device Information Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Api(tags = "Device Management")
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceInfoController {

    private final DeviceInfoService deviceInfoService;
    private final DeviceTagRelationService deviceTagRelationService;

    /**
     * Page query device information
     */
    @ApiOperation("Page query device information")
    @GetMapping("/page")
    public Result<IPage<DeviceInfo>> pageDevices(
            @ApiParam("Current page") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("Search keyword") @RequestParam(required = false) String keyword,
            @ApiParam("Device type") @RequestParam(required = false) String deviceType,
            @ApiParam("Device status") @RequestParam(required = false) String status,
            @ApiParam("Device tag") @RequestParam(required = false) String tag) {
        
        System.out.println("接收到的查询参数:");
        System.out.println("current: " + current);
        System.out.println("size: " + size);
        System.out.println("keyword: " + keyword);
        System.out.println("deviceType: " + deviceType);
        System.out.println("status: " + status);
        System.out.println("tag: " + tag);
        
        Page<DeviceInfo> page = new Page<>(current, size);
        IPage<DeviceInfo> result = deviceInfoService.getDevicePage(page, keyword, tag, status);
        return Result.success(result);
    }

    /**
     * Query device information by ID
     */
    @ApiOperation("Query device information by ID")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDeviceById(@ApiParam("Device ID") @PathVariable Long id) {
        DeviceInfo deviceInfo = deviceInfoService.getById(id);
        if (deviceInfo == null) {
            return Result.error("设备不存在");
        }
        
        Map<String, Object> result = buildDeviceInfoMap(deviceInfo);
        return Result.success(result);
    }

    /**
     * Query device information by device ID
     */
    @ApiOperation("Query device information by device ID")
    @GetMapping("/deviceId/{deviceId}")
    public Result<Map<String, Object>> getDeviceByDeviceId(@ApiParam("Device ID") @PathVariable String deviceId) {
        DeviceInfo deviceInfo = deviceInfoService.getByDeviceId(deviceId);
        if (deviceInfo == null) {
            return Result.error("设备不存在");
        }
        
        Map<String, Object> result = buildDeviceInfoMap(deviceInfo);
        return Result.success(result);
    }

    /**
     * Add new device information
     */
    @ApiOperation("Add new device information")
    @PostMapping
    public Result<String> addDevice(@Valid @RequestBody DeviceInfo deviceInfo) {
        // 检查设备编号是否已存在
        if (deviceInfoService.checkDeviceIdExists(deviceInfo.getDeviceId())) {
            return Result.error("设备编号已存在");
        }
        
        boolean success = deviceInfoService.addDevice(deviceInfo);
        if (success) {
            return Result.success("新增成功");
        } else {
            return Result.error("新增失败");
        }
    }

    /**
     * Update device information
     */
    @ApiOperation("Update device information")
    @PutMapping("/{id}")
    public Result<String> updateDevice(
            @ApiParam("Device ID") @PathVariable Long id,
            @RequestBody Map<String, Object> requestData) {
        
        // 提取设备信息
        DeviceInfo deviceInfo = extractDeviceInfo(requestData);
        deviceInfo.setId(id);
        
        // 更新设备信息
        boolean success = deviceInfoService.updateDevice(deviceInfo);
        if (!success) {
            return Result.error("设备信息更新失败");
        }
        
        // 处理设备标签
        if (requestData.containsKey("selectedTags")) {
            Object selectedTagsObj = requestData.get("selectedTags");
            if (selectedTagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> selectedTagsList = (List<Object>) selectedTagsObj;
                List<Long> tagIds = new ArrayList<>();
                
                for (Object tagObj : selectedTagsList) {
                    if (tagObj instanceof Number) {
                        tagIds.add(((Number) tagObj).longValue());
                    } else if (tagObj instanceof String) {
                        try {
                            tagIds.add(Long.parseLong((String) tagObj));
                        } catch (NumberFormatException e) {
                            System.err.println("无效的标签ID: " + tagObj);
                        }
                    }
                }
                
                if (!tagIds.isEmpty()) {
                    boolean tagSuccess = deviceTagRelationService.setDeviceTags(id, tagIds, "admin");
                    if (!tagSuccess) {
                        System.err.println("设备标签保存失败，设备ID: " + id);
                    }
                }
            }
        }
        
        return Result.success("更新成功");
    }
    
    /**
     * Dispatch algorithm to camera
     */
    @ApiOperation("Dispatch algorithm to camera")
    @PostMapping("/{algorithmId}/algorithms")
    public Result<String> dispatchAlgorithms(@ApiParam("Algorithm ID") @PathVariable Long algorithmId, @RequestParam String deviceIds) {
        boolean success = deviceInfoService.dispatchAlgorithms(algorithmId, deviceIds);
        if (success) {
            return Result.success("算法下发成功");
        } else {
            return Result.error("算法下发失败，设备不存在");
        }
    }
    
    /**
     * Extract device information from request data
     */
    private DeviceInfo extractDeviceInfo(Map<String, Object> requestData) {
        DeviceInfo deviceInfo = new DeviceInfo();
        
        // 设置基本字段
        setIfNotNull(deviceInfo::setDeviceName, requestData.get("deviceName"));
        setIfNotNull(deviceInfo::setDeviceId, requestData.get("deviceId"));
        setIfNotNull(deviceInfo::setStreamUrl, requestData.get("streamUrl"));
        setIfNotNull(deviceInfo::setStatus, requestData.get("status"));
        setIfNotNull(deviceInfo::setPosition, requestData.get("position"));
        setIfNotNull(deviceInfo::setDeviceType, requestData.get("deviceType"));
        setIfNotNull(deviceInfo::setBrand, requestData.get("brand"));
        setIfNotNull(deviceInfo::setModel, requestData.get("model"));
        setIfNotNull(deviceInfo::setDescription, requestData.get("description"));
        setIfNotNull(deviceInfo::setRemark, requestData.get("remark"));
        setIfNotNull(deviceInfo::setCreatedBy, requestData.get("createdBy"));
        setIfNotNull(deviceInfo::setUpdatedBy, requestData.get("updatedBy"));
        
        // 处理新增字段
        setIfNotNull(deviceInfo::setTag, requestData.get("tag"));
        setIfNotNull(deviceInfo::setLocation, requestData.get("location"));
        setIfNotNull(deviceInfo::setManufacturer, requestData.get("manufacturer"));
        setIfNotNull(deviceInfo::setRtspUrl, requestData.get("rtspUrl"));
        setIfNotNull(deviceInfo::setStreamPath, requestData.get("streamPath"));
        setIfNotNull(deviceInfo::setCreator, requestData.get("creator"));
        setIfNotNull(deviceInfo::setLongitude, requestData.get("longitude"));
        setIfNotNull(deviceInfo::setLatitude, requestData.get("latitude"));
        setIfNotNull(deviceInfo::setImagePath, requestData.get("imagePath"));
        setIfNotNull(deviceInfo::setHeightPosition, requestData.get("heightPosition"));
        setIfNotNull(deviceInfo::setAddress, requestData.get("address"));
        
        // 处理region字段（JSON格式）
        if (requestData.containsKey("region")) {
            Object regionObj = requestData.get("region");
            if (regionObj != null) {
                if (regionObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> regionList = (List<String>) regionObj;
                    // 简单的JSON格式转换，避免引入Jackson依赖
                    String regionJson = "[\"" + String.join("\",\"", regionList) + "\"]";
                    deviceInfo.setRegion(regionJson);
                } else if (regionObj instanceof String) {
                    deviceInfo.setRegion((String) regionObj);
                }
            }
        }
        
        // 处理时间字段
        if (requestData.containsKey("createTime")) {
            Object createTimeObj = requestData.get("createTime");
            if (createTimeObj instanceof String) {
                try {
                    deviceInfo.setCreateTime(LocalDateTime.parse((String) createTimeObj));
                } catch (Exception e) {
                    System.err.println("createTime解析失败: " + createTimeObj);
                }
            }
        }
        
        if (requestData.containsKey("updateTime")) {
            Object updateTimeObj = requestData.get("updateTime");
            if (updateTimeObj instanceof String) {
                try {
                    deviceInfo.setUpdateTime(LocalDateTime.parse((String) updateTimeObj));
                } catch (Exception e) {
                    System.err.println("updateTime解析失败: " + updateTimeObj);
                }
            }
        }
        
        return deviceInfo;
    }

    /**
     * Helper method: set value if not null
     */
    private void setIfNotNull(java.util.function.Consumer<String> setter, Object value) {
        if (value != null) {
            setter.accept(value.toString());
        }
    }
    
    /**
     * Build complete device information map including associated tag information
     */
    private Map<String, Object> buildDeviceInfoMap(DeviceInfo deviceInfo) {
        Map<String, Object> result = new HashMap<>();
        
        // 设备基本信息
        result.put("id", deviceInfo.getId());
        result.put("deviceName", deviceInfo.getDeviceName());
        result.put("deviceId", deviceInfo.getDeviceId());
        result.put("streamUrl", deviceInfo.getStreamUrl());
        result.put("status", deviceInfo.getStatus());
        result.put("position", deviceInfo.getPosition());
        result.put("deviceType", deviceInfo.getDeviceType());
        result.put("brand", deviceInfo.getBrand());
        result.put("model", deviceInfo.getModel());
        result.put("ipAddress", deviceInfo.getIpAddress());
        result.put("port", deviceInfo.getPort());
        result.put("username", deviceInfo.getUsername());
        result.put("password", deviceInfo.getPassword());
        result.put("description", deviceInfo.getDescription());
        result.put("remark", deviceInfo.getRemark());
        result.put("createdBy", deviceInfo.getCreatedBy());
        result.put("updatedBy", deviceInfo.getUpdatedBy());
        result.put("createTime", deviceInfo.getCreateTime());
        result.put("updateTime", deviceInfo.getUpdateTime());
        result.put("deleted", deviceInfo.getDeleted());
        
        // 新增字段
        result.put("tag", deviceInfo.getTag());
        result.put("location", deviceInfo.getLocation());
        result.put("manufacturer", deviceInfo.getManufacturer());
        result.put("rtspUrl", deviceInfo.getRtspUrl());
        result.put("streamPath", deviceInfo.getStreamPath());
        result.put("creator", deviceInfo.getCreator());
        result.put("longitude", deviceInfo.getLongitude());
        result.put("latitude", deviceInfo.getLatitude());
        result.put("imagePath", deviceInfo.getImagePath());
        result.put("heightPosition", deviceInfo.getHeightPosition());
        result.put("address", deviceInfo.getAddress());
        result.put("region", deviceInfo.getRegion());
        
        // 查询关联的标签信息
        try {
            List<DeviceTagRelation> tagRelations = deviceTagRelationService.getDeviceTags(deviceInfo.getId());
            List<Long> selectedTags = new ArrayList<>();
            for (DeviceTagRelation relation : tagRelations) {
                selectedTags.add(relation.getTagId());
            }
            result.put("selectedTags", selectedTags);
        } catch (Exception e) {
            System.err.println("查询设备标签失败: " + e.getMessage());
            result.put("selectedTags", new ArrayList<>());
        }
        
        return result;
    }

    /**
     * Delete device information
     */
    @ApiOperation("Delete device information")
    @DeleteMapping("/{id}")
    public Result<String> deleteDevice(@ApiParam("Device ID") @PathVariable Long id) {
        boolean success = deviceInfoService.deleteDevice(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * Batch delete device information
     */
    @ApiOperation("Batch delete device information")
    @DeleteMapping("/batch")
    public Result<String> deleteDeviceBatch(@ApiParam("Device ID list") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的设备");
        }
        
        boolean success = deviceInfoService.deleteDeviceBatch(ids);
        if (success) {
            return Result.success("批量删除成功");
        } else {
            return Result.error("批量删除失败");
        }
    }

    /**
     * Update device status
     */
    @ApiOperation("Update device status")
    @PutMapping("/{id}/status/{status}")
    public Result<String> updateDeviceStatus(
            @ApiParam("Device ID") @PathVariable Long id,
            @ApiParam("Device status") @PathVariable String status) {
        
        boolean success = deviceInfoService.updateDeviceStatus(id, status);
        if (success) {
            return Result.success("状态更新成功");
        } else {
            return Result.error("状态更新失败");
        }
    }

    /**
     * Batch update device status
     */
    @ApiOperation("Batch update device status")
    @PutMapping("/status/{status}")
    public Result<String> updateDeviceStatusBatch(
            @ApiParam("Device status") @PathVariable String status,
            @ApiParam("Device ID list") @RequestBody List<Long> ids) {
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要更新的设备");
        }
        
        boolean success = deviceInfoService.updateDeviceStatusBatch(ids, status);
        if (success) {
            return Result.success("批量状态更新成功");
        } else {
            return Result.error("批量状态更新失败");
        }
    }

    /**
     * Query device list by status
     */
    @ApiOperation("Query device list by status")
    @GetMapping("/status/{status}")
    public Result<List<DeviceInfo>> getDevicesByStatus(@ApiParam("Device status") @PathVariable String status) {
        List<DeviceInfo> devices = deviceInfoService.getDevicesByStatus(status);
        return Result.success(devices);
    }

    /**
     * Query device list by device type
     */
    @ApiOperation("Query device list by device type")
    @GetMapping("/type/{deviceType}")
    public Result<List<DeviceInfo>> getDevicesByType(@ApiParam("Device type") @PathVariable String deviceType) {
        List<DeviceInfo> devices = deviceInfoService.getDevicesByType(deviceType);
        return Result.success(devices);
    }

    /**
     * Query device list by brand
     */
    @ApiOperation("Query device list by brand")
    @GetMapping("/brand/{brand}")
    public Result<List<DeviceInfo>> getDevicesByBrand(@ApiParam("Device brand") @PathVariable String brand) {
        List<DeviceInfo> devices = deviceInfoService.getDevicesByBrand(brand);
        return Result.success(devices);
    }

    /**
     * Test device connection
     */
    @ApiOperation("Test device connection")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testDeviceConnection(@ApiParam("Device ID") @PathVariable Long id) {
        Map<String, Object> result = deviceInfoService.testDeviceConnection(id);
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * Get device statistics
     */
    @ApiOperation("Get device statistics")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getDeviceStatistics() {
        Map<String, Object> statistics = deviceInfoService.getDeviceStatistics();
        return Result.success(statistics);
    }

    /**
     * Get device group statistics (grouped by tag)
     */
    @ApiOperation("Get device group statistics")
    @GetMapping("/group-statistics")
    public Result<List<Map<String, Object>>> getDeviceGroupStatistics() {
        // 获取所有设备
        List<DeviceInfo> allDevices = deviceInfoService.list();
        
        // 按设备类型分组统计
        Map<String, List<DeviceInfo>> devicesByType = new HashMap<>();
        for (DeviceInfo device : allDevices) {
            String type = device.getDeviceType();
            if (type == null || type.trim().isEmpty()) {
                type = "未分类";
            }
            devicesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(device);
        }
        
        // 构建统计结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<DeviceInfo>> entry : devicesByType.entrySet()) {
            String typeName = entry.getKey();
            List<DeviceInfo> devices = entry.getValue();
            
            Map<String, Object> groupStat = new HashMap<>();
            groupStat.put("type", typeName);
            groupStat.put("total", devices.size());
            
            // 统计各状态数量
            long online = devices.stream().filter(d -> "在线".equals(d.getStatus())).count();
            long offline = devices.stream().filter(d -> "离线".equals(d.getStatus())).count();
            long fault = devices.stream().filter(d -> "故障".equals(d.getStatus())).count();
            
            groupStat.put("online", online);
            groupStat.put("offline", offline);
            groupStat.put("fault", fault);
            
            result.add(groupStat);
        }
        
        return Result.success(result);
    }

    /**
     * Get device type statistics
     */
    @ApiOperation("Get device type statistics")
    @GetMapping("/type-statistics")
    public Result<Map<String, Object>> getDeviceTypeStatistics() {
        // 获取所有设备类型
        List<String> allTypes = deviceInfoService.getAllTags();
        Map<String, Object> statistics = new HashMap<>();
        
        for (String type : allTypes) {
            List<DeviceInfo> devices = deviceInfoService.getDevicesByType(type);
            statistics.put(type, devices.size());
        }
        
        return Result.success(statistics);
    }

    /**
     * Get all device type list (tag list)
     */
    @ApiOperation("Get all device type list")
    @GetMapping("/tags")
    public Result<List<String>> getDeviceTags() {
        List<String> tags = deviceInfoService.getAllTags();
        return Result.success(tags);
    }

    /**
     * Get all device brand list
     */
    @ApiOperation("Get all device brand list")
    @GetMapping("/brands")
    public Result<List<String>> getDeviceBrands() {
        List<String> brands = deviceInfoService.getAllBrands();
        return Result.success(brands);
    }

    /**
     * Refresh device status
     */
    @ApiOperation("Refresh device status")
    @PostMapping("/{id}/refresh")
    public Result<String> refreshDeviceStatus(@ApiParam("Device ID") @PathVariable Long id) {
        Map<String, Object> result = deviceInfoService.refreshDeviceStatus(id);
        if ((Boolean) result.get("success")) {
            return Result.success((String) result.get("message"));
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * Batch refresh device status
     */
    @ApiOperation("Batch refresh device status")
    @PostMapping("/batch/refresh")
    public Result<String> batchRefreshDevices(@ApiParam("Device ID list") @RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要刷新的设备");
        }
        
        int successCount = 0;
        for (Long id : ids) {
            Map<String, Object> result = deviceInfoService.refreshDeviceStatus(id);
            if ((Boolean) result.get("success")) {
                successCount++;
            }
        }
        
        return Result.success("批量刷新完成，成功 " + successCount + " 台设备");
    }

    /**
     * PTZ control - move
     */
    @ApiOperation("PTZ control - move")
    @PostMapping("/{id}/ptz/move")
    public Result<String> ptzMove(
            @ApiParam("Device ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        
        String direction = (String) params.get("direction");
        Integer speed = (Integer) params.getOrDefault("speed", 4);
        
        // 检查设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        Map<String, Object> result = deviceInfoService.ptzControl(id, "move", params);
        if ((Boolean) result.get("success")) {
        return Result.success("PTZ移动成功: " + direction + ", 速度: " + speed);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * PTZ control - stop
     */
    @ApiOperation("PTZ control - stop")
    @PostMapping("/{id}/ptz/stop")
    public Result<String> ptzStop(@ApiParam("Device ID") @PathVariable Long id) {
        // 检查设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        Map<String, Object> result = deviceInfoService.ptzControl(id, "stop", new HashMap<>());
        if ((Boolean) result.get("success")) {
        return Result.success("PTZ停止成功");
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * PTZ control - zoom
     */
    @ApiOperation("PTZ control - zoom")
    @PostMapping("/{id}/ptz/zoom")
    public Result<String> ptzZoom(
            @ApiParam("Device ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        
        String action = (String) params.get("action");
        Integer speed = (Integer) params.getOrDefault("speed", 4);
        
        // 检查设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        Map<String, Object> result = deviceInfoService.ptzControl(id, "zoom", params);
        if ((Boolean) result.get("success")) {
        return Result.success("PTZ缩放成功: " + action + ", 速度: " + speed);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * Get device video stream information
     */
    @ApiOperation("Get device video stream information")
    @GetMapping("/{id}/stream")
    public Result<Map<String, Object>> getDeviceStreamInfo(@ApiParam("Device ID") @PathVariable Long id) {
        Map<String, Object> streamInfo = deviceInfoService.getVideoStreamInfo(id);
        if (streamInfo.isEmpty()) {
            return Result.error("设备不存在");
        }
        return Result.success(streamInfo);
    }

    /**
     * Export device list
     */
    @ApiOperation("Export device list")
    @GetMapping("/export")
    public Result<List<DeviceInfo>> exportDevices(@RequestParam(required = false) List<Long> deviceIds) {
        List<DeviceInfo> devices = deviceInfoService.exportDevices(deviceIds);
        return Result.success(devices);
    }

    /**
     * Import device list
     */
    @ApiOperation("Import device list")
    @PostMapping("/import")
    public Result<Map<String, Object>> importDevices(@RequestParam("file") MultipartFile file) {
        // TODO: 实现文件解析和设备导入功能
        Map<String, Object> result = new HashMap<>();
        result.put("message", "导入功能待实现");
        return Result.success(result);
    }

    /**
     * Get device configuration
     */
    @ApiOperation("Get device configuration")
    @GetMapping("/{id}/config")
    public Result<Map<String, Object>> getDeviceConfig(@ApiParam("Device ID") @PathVariable Long id) {
        Map<String, Object> config = deviceInfoService.getDeviceConfig(id);
        if (config.isEmpty()) {
            return Result.error("设备不存在");
        }
        return Result.success(config);
    }

    /**
     * Update device configuration
     */
    @ApiOperation("Update device configuration")
    @PutMapping("/{id}/config")
    public Result<String> updateDeviceConfig(
            @ApiParam("Device ID") @PathVariable Long id,
            @RequestBody Map<String, Object> config) {
        
        boolean success = deviceInfoService.updateDeviceConfig(id, config);
        if (success) {
            return Result.success("配置更新成功");
        } else {
            return Result.error("配置更新失败");
        }
    }

    // ==================== 设备标签相关接口 ====================

    /**
     * Set device tags
     */
    @ApiOperation("Set device tags")
    @PutMapping("/{id}/tags")
    public Result<String> setDeviceTags(
            @ApiParam("Device ID") @PathVariable Long id,
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        // 验证设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        boolean success = deviceTagRelationService.setDeviceTags(id, tagIds, "admin");
        if (success) {
            return Result.success("设备标签设置成功");
        } else {
            return Result.error("设备标签设置失败");
        }
    }

    /**
     * Get device tags
     */
    @ApiOperation("Get device tags")
    @GetMapping("/{id}/tags")
    public Result<List<DeviceTagRelation>> getDeviceTags(
            @ApiParam("Device ID") @PathVariable Long id) {
        
        // 验证设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        List<DeviceTagRelation> tags = deviceTagRelationService.getDeviceTags(id);
        return Result.success(tags);
    }

    /**
     * Add device tags
     */
    @ApiOperation("Add device tags")
    @PostMapping("/{id}/tags")
    public Result<String> addDeviceTags(
            @ApiParam("Device ID") @PathVariable Long id,
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        // 验证设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        boolean success = deviceTagRelationService.addDeviceTags(id, tagIds, "admin");
        if (success) {
            return Result.success("设备标签添加成功");
        } else {
            return Result.error("设备标签添加失败");
        }
    }

    /**
     * Remove device tags
     */
    @ApiOperation("Remove device tags")
    @DeleteMapping("/{id}/tags")
    public Result<String> removeDeviceTags(
            @ApiParam("Device ID") @PathVariable Long id,
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        // 验证设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        boolean success = deviceTagRelationService.removeDeviceTags(id, tagIds);
        if (success) {
            return Result.success("设备标签移除成功");
        } else {
            return Result.error("设备标签移除失败");
        }
    }

    /**
     * Clear all device tags
     */
    @ApiOperation("Clear all device tags")
    @DeleteMapping("/{id}/tags/all")
    public Result<String> clearDeviceTags(
            @ApiParam("Device ID") @PathVariable Long id) {
        
        // 验证设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        boolean success = deviceTagRelationService.clearDeviceTags(id);
        if (success) {
            return Result.success("设备标签清除成功");
        } else {
            return Result.error("设备标签清除失败");
        }
    }

    /**
     * Get device tag details
     */
    @ApiOperation("Get device tag details")
    @GetMapping("/{id}/tag-details")
    public Result<Map<String, Object>> getDeviceTagDetails(
            @ApiParam("Device ID") @PathVariable Long id) {
        
        // 验证设备是否存在
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        
        Map<String, Object> details = deviceTagRelationService.getDeviceTagDetails(id);
        return Result.success(details);
    }

    /**
     * Copy device tags
     */
    @ApiOperation("Copy device tags to other devices")
    @PostMapping("/{sourceId}/copy-tags")
    public Result<String> copyDeviceTags(
            @ApiParam("Source device ID") @PathVariable Long sourceId,
            @ApiParam("Target device ID list") @RequestBody List<Long> targetDeviceIds) {
        
        // 验证源设备是否存在
        DeviceInfo sourceDevice = deviceInfoService.getById(sourceId);
        if (sourceDevice == null) {
            return Result.error("源设备不存在");
        }
        
        // 验证目标设备是否都存在
        for (Long targetId : targetDeviceIds) {
            DeviceInfo targetDevice = deviceInfoService.getById(targetId);
            if (targetDevice == null) {
                return Result.error("目标设备不存在: " + targetId);
            }
        }
        
        boolean success = deviceTagRelationService.copyDeviceTags(sourceId, targetDeviceIds, "admin");
        if (success) {
            return Result.success("复制设备标签成功");
        } else {
            return Result.error("复制设备标签失败");
        }
    }
    
    /**
     * Get device tree structure
     */
    @ApiOperation("获取设备树形结构")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getDeviceTree() {
        try {
            // 定义固定的设备类型
            String[] deviceTypes = {"球机", "云台", "摄像头", "枪机", "半球"};
            
            List<Map<String, Object>> treeData = new ArrayList<>();
            
            for (String deviceType : deviceTypes) {
                Map<String, Object> typeNode = new HashMap<>();
                typeNode.put("id", "type_" + deviceType);
                typeNode.put("label", deviceType);
                typeNode.put("type", "device_type");
                
                // 使用分页API查询该类型下的所有设备（设置大页面获取全部数据）
                Page<DeviceInfo> page = new Page<>(1, 1000); // 设置大页面获取全部数据
                IPage<DeviceInfo> devicePage = deviceInfoService.getDevicePage(page, null, deviceType, null);
                List<DeviceInfo> devices = devicePage.getRecords();
                typeNode.put("deviceCount", devices.size());
                
                // 构建设备节点
                List<Map<String, Object>> deviceNodes = new ArrayList<>();
                for (DeviceInfo device : devices) {
                    Map<String, Object> deviceNode = new HashMap<>();
                    deviceNode.putAll(BeanUtil.beanToMap(deviceNode));
                    deviceNode.put("id", "device_" + device.getId());
                    deviceNode.put("label", device.getDeviceName());
                    deviceNode.put("type", "device");
                    deviceNode.put("deviceId", device.getId());
                    deviceNode.put("deviceName", device.getDeviceName());
                    deviceNode.put("status", device.getStatus());
                    deviceNode.put("streamUrl", device.getStreamUrl());
                    deviceNodes.add(deviceNode);
                }
                
                typeNode.put("children", deviceNodes);
                
                // 更新label以显示数量
                typeNode.put("label", deviceType + " (" + devices.size() + ")");
                
                treeData.add(typeNode);
            }
            
            return Result.success(treeData);
        } catch (Exception e) {
            System.err.println("获取设备树失败: " + e.getMessage());
            return Result.error("获取设备树失败: " + e.getMessage());
        }
    }
} 
