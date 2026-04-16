package com.vlstream.controller;

import com.vlstream.common.Result;
import com.vlstream.entity.DeviceTagRelation;
import com.vlstream.service.DeviceTagRelationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Device Tag Relation Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Api(tags = "Device Tag Relation Management")
@RestController
@RequestMapping("/api/device-tag")
@RequiredArgsConstructor
public class DeviceTagRelationController {

    private final DeviceTagRelationService deviceTagRelationService;

    /**
     * Set device tags (override existing tags)
     */
    @ApiOperation("Set device tags")
    @PutMapping("/device/{deviceId}/tags")
    public Result<String> setDeviceTags(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        boolean success = deviceTagRelationService.setDeviceTags(deviceId, tagIds, "admin");
        if (success) {
            return Result.success("Device tags set successfully");
        } else {
            return Result.error("Failed to set device tags");
        }
    }

    /**
     * Add device tags (append to existing tags)
     */
    @ApiOperation("Add device tags")
    @PostMapping("/device/{deviceId}/tags")
    public Result<String> addDeviceTags(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        boolean success = deviceTagRelationService.addDeviceTags(deviceId, tagIds, "admin");
        if (success) {
            return Result.success("Device tags added successfully");
        } else {
            return Result.error("Failed to add device tags");
        }
    }

    /**
     * Remove device tags
     */
    @ApiOperation("Remove device tags")
    @DeleteMapping("/device/{deviceId}/tags")
    public Result<String> removeDeviceTags(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        boolean success = deviceTagRelationService.removeDeviceTags(deviceId, tagIds);
        if (success) {
            return Result.success("Device tags removed successfully");
        } else {
            return Result.error("Failed to remove device tags");
        }
    }

    /**
     * Clear all device tags
     */
    @ApiOperation("Clear all device tags")
    @DeleteMapping("/device/{deviceId}/tags/all")
    public Result<String> clearDeviceTags(
            @ApiParam("Device ID") @PathVariable Long deviceId) {
        
        boolean success = deviceTagRelationService.clearDeviceTags(deviceId);
        if (success) {
            return Result.success("All device tags cleared successfully");
        } else {
            return Result.error("Failed to clear device tags");
        }
    }

    /**
     * Get all device tags
     */
    @ApiOperation("Get device's all tags")
    @GetMapping("/device/{deviceId}/tags")
    public Result<List<DeviceTagRelation>> getDeviceTags(
            @ApiParam("Device ID") @PathVariable Long deviceId) {
        
        List<DeviceTagRelation> tags = deviceTagRelationService.getDeviceTags(deviceId);
        return Result.success(tags);
    }

    /**
     * Get device tag ID list
     */
    @ApiOperation("Get device's tag ID list")
    @GetMapping("/device/{deviceId}/tag-ids")
    public Result<List<Long>> getDeviceTagIds(
            @ApiParam("Device ID") @PathVariable Long deviceId) {
        
        List<Long> tagIds = deviceTagRelationService.getDeviceTagIds(deviceId);
        return Result.success(tagIds);
    }

    /**
     * Get device tag details
     */
    @ApiOperation("Get device tag details")
    @GetMapping("/device/{deviceId}/tag-details")
    public Result<Map<String, Object>> getDeviceTagDetails(
            @ApiParam("Device ID") @PathVariable Long deviceId) {
        
        Map<String, Object> details = deviceTagRelationService.getDeviceTagDetails(deviceId);
        return Result.success(details);
    }

    /**
     * Get device list with specified tag
     */
    @ApiOperation("Get devices with specified tag")
    @GetMapping("/tag/{tagId}/devices")
    public Result<List<Map<String, Object>>> getDevicesByTag(
            @ApiParam("Tag ID") @PathVariable Long tagId) {
        
        List<Map<String, Object>> devices = deviceTagRelationService.getDevicesByTag(tagId);
        return Result.success(devices);
    }

    /**
     * Query devices by multiple tags (intersection)
     */
    @ApiOperation("Query devices by multiple tags (intersection)")
    @PostMapping("/devices/by-all-tags")
    public Result<List<Long>> findDevicesByAllTags(
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        List<Long> deviceIds = deviceTagRelationService.findDevicesByAllTags(tagIds);
        return Result.success(deviceIds);
    }

    /**
     * Query devices by multiple tags (union)
     */
    @ApiOperation("Query devices by multiple tags (union)")
    @PostMapping("/devices/by-any-tags")
    public Result<List<Long>> findDevicesByAnyTags(
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        List<Long> deviceIds = deviceTagRelationService.findDevicesByAnyTags(tagIds);
        return Result.success(deviceIds);
    }

    /**
     * Batch set device tags
     */
    @ApiOperation("Batch set device tags")
    @PutMapping("/devices/batch-tags")
    public Result<String> batchSetDeviceTags(
            @ApiParam("Device tag mapping") @RequestBody Map<Long, List<Long>> deviceTagMap) {
        
        int successCount = deviceTagRelationService.batchSetDeviceTags(deviceTagMap, "admin");
        return Result.success("Batch setting successful, successfully set tags for " + successCount + " devices");
    }

    /**
     * Copy device tags to other devices
     */
    @ApiOperation("Copy device tags to other devices")
    @PostMapping("/device/{sourceDeviceId}/copy-tags")
    public Result<String> copyDeviceTags(
            @ApiParam("Source device ID") @PathVariable Long sourceDeviceId,
            @ApiParam("Target device ID list") @RequestBody List<Long> targetDeviceIds) {
        
        boolean success = deviceTagRelationService.copyDeviceTags(sourceDeviceId, targetDeviceIds, "admin");
        if (success) {
            return Result.success("Device tags copied successfully");
        } else {
            return Result.error("Failed to copy device tags");
        }
    }

    /**
     * Get device tag statistics
     */
    @ApiOperation("Get device tag statistics")
    @GetMapping("/statistics/device-tags")
    public Result<List<Map<String, Object>>> getDeviceTagStatistics() {
        List<Map<String, Object>> statistics = deviceTagRelationService.getDeviceTagStatistics();
        return Result.success(statistics);
    }

    /**
     * Get tag usage statistics
     */
    @ApiOperation("Get tag usage statistics")
    @GetMapping("/statistics/tag-usage")
    public Result<List<Map<String, Object>>> getTagUsageStatistics() {
        List<Map<String, Object>> statistics = deviceTagRelationService.getTagUsageStatistics();
        return Result.success(statistics);
    }

    /**
     * Check if device has specified tag
     */
    @ApiOperation("Check if device has specified tag")
    @GetMapping("/device/{deviceId}/has-tag/{tagId}")
    public Result<Boolean> hasDeviceTag(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Tag ID") @PathVariable Long tagId) {
        
        boolean hasTag = deviceTagRelationService.hasDeviceTag(deviceId, tagId);
        return Result.success(hasTag);
    }

    /**
     * Get device count of tag
     */
    @ApiOperation("Get tag's device count")
    @GetMapping("/tag/{tagId}/device-count")
    public Result<Integer> getTagDeviceCount(
            @ApiParam("Tag ID") @PathVariable Long tagId) {
        
        int count = deviceTagRelationService.getTagDeviceCount(tagId);
        return Result.success(count);
    }

    /**
     * Validate tag ID list
     */
    @ApiOperation("Validate tag ID list")
    @PostMapping("/validate-tags")
    public Result<Map<String, Object>> validateTagIds(
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        Map<String, Object> result = deviceTagRelationService.validateTagIds(tagIds);
        return Result.success(result);
    }

    /**
     * Sync tag usage count
     */
    @ApiOperation("Sync tag usage count")
    @PostMapping("/sync-usage-count")
    public Result<String> syncTagUsageCount() {
        boolean success = deviceTagRelationService.syncTagUsageCount();
        if (success) {
            return Result.success("Tag usage count synchronized successfully");
        } else {
            return Result.error("Failed to sync tag usage count");
        }
    }

    /**
     * Get complete device tag information (including device and tag info)
     */
    @ApiOperation("Get device tag full information")
    @GetMapping("/device/{deviceId}/full-info")
    public Result<Map<String, Object>> getDeviceTagFullInfo(
            @ApiParam("Device ID") @PathVariable Long deviceId) {
        
        Map<String, Object> info = new java.util.HashMap<>();
        
        // 获取设备标签详情
        Map<String, Object> tagDetails = deviceTagRelationService.getDeviceTagDetails(deviceId);
        info.put("tagDetails", tagDetails);
        
        // 获取设备的标签列表
        List<DeviceTagRelation> tags = deviceTagRelationService.getDeviceTags(deviceId);
        info.put("tags", tags);
        
        return Result.success(info);
    }

    /**
     * Batch get device tag relation info by tags
     */
    @ApiOperation("Batch get device tag relations by tags")
    @PostMapping("/tags/device-relations")
    public Result<Map<String, Object>> getDeviceRelationsByTags(
            @ApiParam("Tag ID list") @RequestBody List<Long> tagIds) {
        
        Map<String, Object> result = new java.util.HashMap<>();
        
        for (Long tagId : tagIds) {
            List<Map<String, Object>> devices = deviceTagRelationService.getDevicesByTag(tagId);
            result.put("tag_" + tagId, devices);
        }
        
        return Result.success(result);
    }
} 