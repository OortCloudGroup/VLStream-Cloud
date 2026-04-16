package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.DeviceTagRelation;

import java.util.List;
import java.util.Map;

/**
 * Device Tag Relation Service Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DeviceTagRelationService extends IService<DeviceTagRelation> {

    /**
     * Set device tags (override existing tags)
     *
     * @param deviceId Device ID
     * @param tagIds Tag ID list
     * @param createdBy Creator
     * @return Whether successful
     */
    boolean setDeviceTags(Long deviceId, List<Long> tagIds, String createdBy);

    /**
     * Add device tags (append to existing tags)
     *
     * @param deviceId Device ID
     * @param tagIds Tag ID list
     * @param createdBy Creator
     * @return Whether successful
     */
    boolean addDeviceTags(Long deviceId, List<Long> tagIds, String createdBy);

    /**
     * Remove device tags
     *
     * @param deviceId Device ID
     * @param tagIds Tag ID list
     * @return Whether successful
     */
    boolean removeDeviceTags(Long deviceId, List<Long> tagIds);

    /**
     * Clear all device tags
     *
     * @param deviceId Device ID
     * @return Whether successful
     */
    boolean clearDeviceTags(Long deviceId);

    /**
     * Get all device tags
     *
     * @param deviceId Device ID
     * @return Tag info list
     */
    List<DeviceTagRelation> getDeviceTags(Long deviceId);

    /**
     * Get device tag ID list
     *
     * @param deviceId Device ID
     * @return Tag ID list
     */
    List<Long> getDeviceTagIds(Long deviceId);

    /**
     * Get device list with specified tag
     *
     * @param tagId Tag ID
     * @return Device info list
     */
    List<Map<String, Object>> getDevicesByTag(Long tagId);

    /**
     * Query devices by multiple tags (intersection - must contain all tags)
     *
     * @param tagIds Tag ID list
     * @return Device ID list
     */
    List<Long> findDevicesByAllTags(List<Long> tagIds);

    /**
     * Query devices by multiple tags (union - contains any tag)
     *
     * @param tagIds Tag ID list
     * @return Device ID list
     */
    List<Long> findDevicesByAnyTags(List<Long> tagIds);

    /**
     * Batch set device tags
     *
     * @param deviceTagMap Map of device ID -> tag ID list
     * @param createdBy Creator
     * @return Number of successfully set devices
     */
    int batchSetDeviceTags(Map<Long, List<Long>> deviceTagMap, String createdBy);

    /**
     * Copy device tags to other devices
     *
     * @param sourceDeviceId Source device ID
     * @param targetDeviceIds Target device ID list
     * @param createdBy Creator
     * @return Whether successful
     */
    boolean copyDeviceTags(Long sourceDeviceId, List<Long> targetDeviceIds, String createdBy);

    /**
     * Get device tag statistics
     *
     * @return Statistics
     */
    List<Map<String, Object>> getDeviceTagStatistics();

    /**
     * Get tag usage statistics
     *
     * @return Tag usage statistics
     */
    List<Map<String, Object>> getTagUsageStatistics();

    /**
     * Check if device has specified tag
     *
     * @param deviceId Device ID
     * @param tagId Tag ID
     * @return Whether exists
     */
    boolean hasDeviceTag(Long deviceId, Long tagId);

    /**
     * Get tag device count
     *
     * @param tagId Tag ID
     * @return Device count
     */
    int getTagDeviceCount(Long tagId);

    /**
     * Validate tag ID list
     *
     * @param tagIds Tag ID list
     * @return Validation result
     */
    Map<String, Object> validateTagIds(List<Long> tagIds);

    /**
     * Get device tag details (including tag hierarchy)
     *
     * @param deviceId Device ID
     * @return Tag details
     */
    Map<String, Object> getDeviceTagDetails(Long deviceId);

    /**
     * Get device list by tag category
     *
     * @param categoryType Tag type (own/public)
     * @param level Tag level (1/2)
     * @return Device info
     */
    List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level);

    /**
     * Sync tag usage count
     * Update usage_count field in tag_management table
     *
     * @return Whether successful
     */
    boolean syncTagUsageCount();
} 