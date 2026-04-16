package com.vlstream.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.DeviceTagRelation;
import com.vlstream.mapper.DeviceTagRelationMapper;
import com.vlstream.mapper.TagManagementMapper;
import com.vlstream.service.DeviceTagRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Device Tag Relation Service Implementation Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTagRelationServiceImpl extends ServiceImpl<DeviceTagRelationMapper, DeviceTagRelation> 
        implements DeviceTagRelationService {

    private final TagManagementMapper tagManagementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDeviceTags(Long deviceId, List<Long> tagIds, String createdBy) {
        try {
            // First delete all tags for the device
            baseMapper.deleteByDeviceId(deviceId);
            
            // If there are new tags, batch insert
            if (tagIds != null && !tagIds.isEmpty()) {
                // Deduplicate and filter invalid tags
                List<Long> validTagIds = validateAndFilterTagIds(tagIds);
                if (!validTagIds.isEmpty()) {
                    baseMapper.batchInsertDeviceTags(deviceId, validTagIds, createdBy);
                }
            }
            
            log.info("Device tags set successfully: deviceId={}, tagIds={}", deviceId, tagIds);
            return true;
        } catch (Exception e) {
            log.error("Failed to set device tags: deviceId={}, tagIds={}", deviceId, tagIds, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDeviceTags(Long deviceId, List<Long> tagIds, String createdBy) {
        try {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }

            // Deduplicate and filter existing tags
            List<Long> existingTagIds = getDeviceTagIds(deviceId);
            List<Long> newTagIds = tagIds.stream()
                    .distinct()
                    .filter(tagId -> !existingTagIds.contains(tagId))
                    .collect(Collectors.toList());
            
            if (!newTagIds.isEmpty()) {
                // Validate tag validity
                List<Long> validTagIds = validateAndFilterTagIds(newTagIds);
                if (!validTagIds.isEmpty()) {
                    baseMapper.batchInsertDeviceTags(deviceId, validTagIds, createdBy);
                }
            }
            
            log.info("Device tags added successfully: deviceId={}, newTagIds={}", deviceId, newTagIds);
            return true;
        } catch (Exception e) {
            log.error("Failed to add device tags: deviceId={}, tagIds={}", deviceId, tagIds, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDeviceTags(Long deviceId, List<Long> tagIds) {
        try {
            if (tagIds == null || tagIds.isEmpty()) {
                return true;
            }
            
            int deleted = baseMapper.deleteDeviceTagsBatch(deviceId, tagIds);
            log.info("Device tags removed successfully: deviceId={}, tagIds={}, deleted={}", deviceId, tagIds, deleted);
            return true;
        } catch (Exception e) {
            log.error("Failed to remove device tags: deviceId={}, tagIds={}", deviceId, tagIds, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearDeviceTags(Long deviceId) {
        try {
            int deleted = baseMapper.deleteByDeviceId(deviceId);
            log.info("Device tags cleared successfully: deviceId={}, deleted={}", deviceId, deleted);
            return true;
        } catch (Exception e) {
            log.error("Failed to clear device tags: deviceId={}", deviceId, e);
            return false;
        }
    }

    @Override
    public List<DeviceTagRelation> getDeviceTags(Long deviceId) {
        return baseMapper.selectTagsByDeviceId(deviceId);
    }

    @Override
    public List<Long> getDeviceTagIds(Long deviceId) {
        return baseMapper.selectTagIdsByDeviceId(deviceId);
    }

    @Override
    public List<Map<String, Object>> getDevicesByTag(Long tagId) {
        return baseMapper.selectDevicesByTagId(tagId);
    }

    @Override
    public List<Long> findDevicesByAllTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return baseMapper.findDevicesByAllTags(tagIds);
    }

    @Override
    public List<Long> findDevicesByAnyTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return baseMapper.findDevicesByAnyTags(tagIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSetDeviceTags(Map<Long, List<Long>> deviceTagMap, String createdBy) {
        int successCount = 0;
        for (Map.Entry<Long, List<Long>> entry : deviceTagMap.entrySet()) {
            Long deviceId = entry.getKey();
            List<Long> tagIds = entry.getValue();
            if (setDeviceTags(deviceId, tagIds, createdBy)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copyDeviceTags(Long sourceDeviceId, List<Long> targetDeviceIds, String createdBy) {
        try {
            // Get tags from source device
            List<Long> sourceTagIds = getDeviceTagIds(sourceDeviceId);
            if (sourceTagIds.isEmpty()) {
                log.info("Source device has no tags, no need to copy: sourceDeviceId={}", sourceDeviceId);
                return true;
            }
            
            // Set tags for each target device
            for (Long targetDeviceId : targetDeviceIds) {
                setDeviceTags(targetDeviceId, sourceTagIds, createdBy);
            }
            
            log.info("Device tags copied successfully: sourceDeviceId={}, targetDeviceIds={}, tagIds={}", 
                    sourceDeviceId, targetDeviceIds, sourceTagIds);
            return true;
        } catch (Exception e) {
            log.error("Failed to copy device tags: sourceDeviceId={}, targetDeviceIds={}", 
                    sourceDeviceId, targetDeviceIds, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getDeviceTagStatistics() {
        return baseMapper.getDeviceTagStatistics();
    }

    @Override
    public List<Map<String, Object>> getTagUsageStatistics() {
        return baseMapper.getTagUsageStatistics();
    }

    @Override
    public boolean hasDeviceTag(Long deviceId, Long tagId) {
        return baseMapper.checkDeviceTagExists(deviceId, tagId) > 0;
    }

    @Override
    public int getTagDeviceCount(Long tagId) {
        return baseMapper.countDevicesByTagId(tagId);
    }

    @Override
    public Map<String, Object> validateTagIds(List<Long> tagIds) {
        Map<String, Object> result = new HashMap<>();
        List<Long> validTagIds = new ArrayList<>();
        List<Long> invalidTagIds = new ArrayList<>();
        
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                // 检查标签是否存在且有效
                if (tagManagementMapper.selectById(tagId) != null) {
                    validTagIds.add(tagId);
                } else {
                    invalidTagIds.add(tagId);
                }
            }
        }
        
        result.put("validTagIds", validTagIds);
        result.put("invalidTagIds", invalidTagIds);
        result.put("totalCount", tagIds != null ? tagIds.size() : 0);
        result.put("validCount", validTagIds.size());
        result.put("invalidCount", invalidTagIds.size());
        result.put("allValid", invalidTagIds.isEmpty());
        
        return result;
    }

    @Override
    public Map<String, Object> getDeviceTagDetails(Long deviceId) {
        List<DeviceTagRelation> deviceTags = getDeviceTags(deviceId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("totalCount", deviceTags.size());
        
        // Group by type
        Map<String, List<DeviceTagRelation>> tagsByCategory = deviceTags.stream()
                .collect(Collectors.groupingBy(tag -> tag.getCategoryType()));
        
        result.put("ownTags", tagsByCategory.getOrDefault("own", new ArrayList<>()));
        result.put("publicTags", tagsByCategory.getOrDefault("public", new ArrayList<>()));
        result.put("ownTagCount", tagsByCategory.getOrDefault("own", new ArrayList<>()).size());
        result.put("publicTagCount", tagsByCategory.getOrDefault("public", new ArrayList<>()).size());
        
        // Tag name list
        List<String> tagNames = deviceTags.stream()
                .map(DeviceTagRelation::getTagName)
                .collect(Collectors.toList());
        result.put("tagNames", tagNames);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level) {
        // Implementation needed based on specific requirements
        // Can query using DeviceInfoMapper and TagManagementMapper
        return new ArrayList<>();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncTagUsageCount() {
        try {
            // Get tag usage statistics
            List<Map<String, Object>> usageStats = getTagUsageStatistics();
            
            // Update usage count for each tag
            for (Map<String, Object> stat : usageStats) {
                Long tagId = (Long) stat.get("tag_id");
                Long deviceCount = (Long) stat.get("device_count");
                
                // Set usage_count field in tag_management table
                tagManagementMapper.setUsageCount(tagId, deviceCount.intValue());
            }
            
            log.info("Tag usage count synced successfully, updated {} tags", usageStats.size());
        return true;
        } catch (Exception e) {
            log.error("Failed to sync tag usage count", e);
            return false;
        }
    }

    /**
     * Validate and filter tag ID list
     *
     * @param tagIds Original tag ID list
     * @return Valid tag ID list
     */
    private List<Long> validateAndFilterTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return tagIds.stream()
                .distinct()
                .filter(Objects::nonNull)
                .filter(tagId -> {
                    try {
                        return tagManagementMapper.selectById(tagId) != null;
                    } catch (Exception e) {
                        log.warn("Failed to validate tag ID: tagId={}", tagId, e);
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
} 