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
 * 设备标签关联控制器
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Api(tags = "设备标签关联管理")
@RestController
@RequestMapping("/api/device-tag")
@RequiredArgsConstructor
public class DeviceTagRelationController {

    private final DeviceTagRelationService deviceTagRelationService;

    /**
     * 设置设备标签（覆盖原有标签）
     */
    @ApiOperation("设置设备标签")
    @PutMapping("/device/{deviceId}/tags")
    public Result<String> setDeviceTags(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        boolean success = deviceTagRelationService.setDeviceTags(deviceId, tagIds, "admin");
        if (success) {
            return Result.success("设备标签设置成功");
        } else {
            return Result.error("设备标签设置失败");
        }
    }

    /**
     * 添加设备标签（追加到现有标签）
     */
    @ApiOperation("添加设备标签")
    @PostMapping("/device/{deviceId}/tags")
    public Result<String> addDeviceTags(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        boolean success = deviceTagRelationService.addDeviceTags(deviceId, tagIds, "admin");
        if (success) {
            return Result.success("设备标签添加成功");
        } else {
            return Result.error("设备标签添加失败");
        }
    }

    /**
     * 移除设备标签
     */
    @ApiOperation("移除设备标签")
    @DeleteMapping("/device/{deviceId}/tags")
    public Result<String> removeDeviceTags(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        boolean success = deviceTagRelationService.removeDeviceTags(deviceId, tagIds);
        if (success) {
            return Result.success("设备标签移除成功");
        } else {
            return Result.error("设备标签移除失败");
        }
    }

    /**
     * 清除设备的所有标签
     */
    @ApiOperation("清除设备的所有标签")
    @DeleteMapping("/device/{deviceId}/tags/all")
    public Result<String> clearDeviceTags(
            @ApiParam("设备ID") @PathVariable Long deviceId) {
        
        boolean success = deviceTagRelationService.clearDeviceTags(deviceId);
        if (success) {
            return Result.success("设备标签清除成功");
        } else {
            return Result.error("设备标签清除失败");
        }
    }

    /**
     * 获取设备的所有标签
     */
    @ApiOperation("获取设备的所有标签")
    @GetMapping("/device/{deviceId}/tags")
    public Result<List<DeviceTagRelation>> getDeviceTags(
            @ApiParam("设备ID") @PathVariable Long deviceId) {
        
        List<DeviceTagRelation> tags = deviceTagRelationService.getDeviceTags(deviceId);
        return Result.success(tags);
    }

    /**
     * 获取设备的标签ID列表
     */
    @ApiOperation("获取设备的标签ID列表")
    @GetMapping("/device/{deviceId}/tag-ids")
    public Result<List<Long>> getDeviceTagIds(
            @ApiParam("设备ID") @PathVariable Long deviceId) {
        
        List<Long> tagIds = deviceTagRelationService.getDeviceTagIds(deviceId);
        return Result.success(tagIds);
    }

    /**
     * 获取设备标签的详细信息
     */
    @ApiOperation("获取设备标签的详细信息")
    @GetMapping("/device/{deviceId}/tag-details")
    public Result<Map<String, Object>> getDeviceTagDetails(
            @ApiParam("设备ID") @PathVariable Long deviceId) {
        
        Map<String, Object> details = deviceTagRelationService.getDeviceTagDetails(deviceId);
        return Result.success(details);
    }

    /**
     * 获取带有指定标签的设备列表
     */
    @ApiOperation("获取带有指定标签的设备列表")
    @GetMapping("/tag/{tagId}/devices")
    public Result<List<Map<String, Object>>> getDevicesByTag(
            @ApiParam("标签ID") @PathVariable Long tagId) {
        
        List<Map<String, Object>> devices = deviceTagRelationService.getDevicesByTag(tagId);
        return Result.success(devices);
    }

    /**
     * 根据多个标签查询设备（交集）
     */
    @ApiOperation("根据多个标签查询设备（交集）")
    @PostMapping("/devices/by-all-tags")
    public Result<List<Long>> findDevicesByAllTags(
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        List<Long> deviceIds = deviceTagRelationService.findDevicesByAllTags(tagIds);
        return Result.success(deviceIds);
    }

    /**
     * 根据多个标签查询设备（并集）
     */
    @ApiOperation("根据多个标签查询设备（并集）")
    @PostMapping("/devices/by-any-tags")
    public Result<List<Long>> findDevicesByAnyTags(
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        List<Long> deviceIds = deviceTagRelationService.findDevicesByAnyTags(tagIds);
        return Result.success(deviceIds);
    }

    /**
     * 批量设置设备标签
     */
    @ApiOperation("批量设置设备标签")
    @PutMapping("/devices/batch-tags")
    public Result<String> batchSetDeviceTags(
            @ApiParam("设备标签映射") @RequestBody Map<Long, List<Long>> deviceTagMap) {
        
        int successCount = deviceTagRelationService.batchSetDeviceTags(deviceTagMap, "admin");
        return Result.success("批量设置成功，成功设置 " + successCount + " 个设备的标签");
    }

    /**
     * 复制设备标签到其他设备
     */
    @ApiOperation("复制设备标签到其他设备")
    @PostMapping("/device/{sourceDeviceId}/copy-tags")
    public Result<String> copyDeviceTags(
            @ApiParam("源设备ID") @PathVariable Long sourceDeviceId,
            @ApiParam("目标设备ID列表") @RequestBody List<Long> targetDeviceIds) {
        
        boolean success = deviceTagRelationService.copyDeviceTags(sourceDeviceId, targetDeviceIds, "admin");
        if (success) {
            return Result.success("复制设备标签成功");
        } else {
            return Result.error("复制设备标签失败");
        }
    }

    /**
     * 获取设备标签统计信息
     */
    @ApiOperation("获取设备标签统计信息")
    @GetMapping("/statistics/device-tags")
    public Result<List<Map<String, Object>>> getDeviceTagStatistics() {
        List<Map<String, Object>> statistics = deviceTagRelationService.getDeviceTagStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取标签使用统计
     */
    @ApiOperation("获取标签使用统计")
    @GetMapping("/statistics/tag-usage")
    public Result<List<Map<String, Object>>> getTagUsageStatistics() {
        List<Map<String, Object>> statistics = deviceTagRelationService.getTagUsageStatistics();
        return Result.success(statistics);
    }

    /**
     * 检查设备是否有指定标签
     */
    @ApiOperation("检查设备是否有指定标签")
    @GetMapping("/device/{deviceId}/has-tag/{tagId}")
    public Result<Boolean> hasDeviceTag(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("标签ID") @PathVariable Long tagId) {
        
        boolean hasTag = deviceTagRelationService.hasDeviceTag(deviceId, tagId);
        return Result.success(hasTag);
    }

    /**
     * 获取标签的设备数量
     */
    @ApiOperation("获取标签的设备数量")
    @GetMapping("/tag/{tagId}/device-count")
    public Result<Integer> getTagDeviceCount(
            @ApiParam("标签ID") @PathVariable Long tagId) {
        
        int count = deviceTagRelationService.getTagDeviceCount(tagId);
        return Result.success(count);
    }

    /**
     * 验证标签ID列表是否有效
     */
    @ApiOperation("验证标签ID列表是否有效")
    @PostMapping("/validate-tags")
    public Result<Map<String, Object>> validateTagIds(
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        Map<String, Object> result = deviceTagRelationService.validateTagIds(tagIds);
        return Result.success(result);
    }

    /**
     * 同步标签使用计数
     */
    @ApiOperation("同步标签使用计数")
    @PostMapping("/sync-usage-count")
    public Result<String> syncTagUsageCount() {
        boolean success = deviceTagRelationService.syncTagUsageCount();
        if (success) {
            return Result.success("同步标签使用计数成功");
        } else {
            return Result.error("同步标签使用计数失败");
        }
    }

    /**
     * 获取设备标签的完整信息（包含设备信息和标签信息）
     */
    @ApiOperation("获取设备标签的完整信息")
    @GetMapping("/device/{deviceId}/full-info")
    public Result<Map<String, Object>> getDeviceTagFullInfo(
            @ApiParam("设备ID") @PathVariable Long deviceId) {
        
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
     * 根据标签批量获取设备标签关联信息
     */
    @ApiOperation("根据标签批量获取设备标签关联信息")
    @PostMapping("/tags/device-relations")
    public Result<Map<String, Object>> getDeviceRelationsByTags(
            @ApiParam("标签ID列表") @RequestBody List<Long> tagIds) {
        
        Map<String, Object> result = new java.util.HashMap<>();
        
        for (Long tagId : tagIds) {
            List<Map<String, Object>> devices = deviceTagRelationService.getDevicesByTag(tagId);
            result.put("tag_" + tagId, devices);
        }
        
        return Result.success(result);
    }
} 