/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.domain.DeviceTagRelation;
import com.ruoyi.vlstream.domain.TagManagement;
import com.ruoyi.vlstream.mapper.VlsDeviceTagRelationMapper;
import com.ruoyi.vlstream.mapper.VlsTagManagementMapper;
import com.ruoyi.vlstream.service.IVlsDeviceTagRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real database service for device/tag relations. */
@Service
@RequiredArgsConstructor
public class VlsDeviceTagRelationServiceImpl
    extends AbstractVlsTenantCrudService<VlsDeviceTagRelationMapper, DeviceTagRelation>
    implements IVlsDeviceTagRelationService {

    private final VlsTagManagementMapper tagMapper;

    /** Return real relation rows enriched with their persisted tag information. */
    @Override
    public List<Map<String, Object>> getDeviceTags(Long deviceId) {
        if (deviceId == null) {
            return Collections.emptyList();
        }
        List<DeviceTagRelation> relations = list(new LambdaQueryWrapper<DeviceTagRelation>()
            .eq(DeviceTagRelation::getDeviceId, deviceId));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceTagRelation relation : relations) {
            TagManagement tag = tagMapper.selectById(relation.getTagId());
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", relation.getId());
            row.put("deviceId", relation.getDeviceId());
            row.put("tagId", relation.getTagId());
            row.put("tagName", tag == null ? null : tag.getTagName());
            row.put("categoryType", tag == null ? null : tag.getCategoryType());
            row.put("tagColor", tag == null ? null : tag.getTagColor());
            result.add(row);
        }
        return result;
    }

    /** Replace device tags atomically. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDeviceTags(Long deviceId, List<Long> tagIds) {
        requireDeviceId(deviceId);
        clearDeviceTags(deviceId);
        return addDeviceTags(deviceId, tagIds);
    }

    /** Add missing, valid tag relations. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDeviceTags(Long deviceId, List<Long> tagIds) {
        requireDeviceId(deviceId);
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }
        Set<Long> uniqueIds = new LinkedHashSet<Long>(tagIds);
        for (Long tagId : uniqueIds) {
            if (tagId == null || tagMapper.selectById(tagId) == null) {
                throw new IllegalArgumentException("Tag does not exist: " + tagId);
            }
            long count = count(new LambdaQueryWrapper<DeviceTagRelation>()
                .eq(DeviceTagRelation::getDeviceId, deviceId)
                .eq(DeviceTagRelation::getTagId, tagId));
            if (count == 0L) {
                DeviceTagRelation relation = new DeviceTagRelation();
                relation.setDeviceId(deviceId);
                relation.setTagId(tagId);
                save(relation);
            }
        }
        return true;
    }

    /** Remove selected device/tag relations. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDeviceTags(Long deviceId, List<Long> tagIds) {
        requireDeviceId(deviceId);
        if (tagIds == null || tagIds.isEmpty()) {
            return false;
        }
        return remove(new LambdaQueryWrapper<DeviceTagRelation>()
            .eq(DeviceTagRelation::getDeviceId, deviceId)
            .in(DeviceTagRelation::getTagId, tagIds));
    }

    /** Clear all real relations for one device. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearDeviceTags(Long deviceId) {
        requireDeviceId(deviceId);
        remove(new LambdaQueryWrapper<DeviceTagRelation>().eq(DeviceTagRelation::getDeviceId, deviceId));
        return true;
    }

    /** Copy source relations to each target device atomically. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copyDeviceTags(Long sourceId, List<Long> targetDeviceIds) {
        requireDeviceId(sourceId);
        if (targetDeviceIds == null || targetDeviceIds.isEmpty()) {
            throw new IllegalArgumentException("targetDeviceIds is required");
        }
        List<DeviceTagRelation> source = list(new LambdaQueryWrapper<DeviceTagRelation>()
            .eq(DeviceTagRelation::getDeviceId, sourceId));
        List<Long> tagIds = new ArrayList<Long>();
        for (DeviceTagRelation relation : source) {
            tagIds.add(relation.getTagId());
        }
        for (Long targetId : targetDeviceIds) {
            setDeviceTags(targetId, tagIds);
        }
        return true;
    }

    /** Return tag detail data derived from actual relation and tag rows. */
    @Override
    public Map<String, Object> getDeviceTagDetails(Long deviceId) {
        List<Map<String, Object>> tags = getDeviceTags(deviceId);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("deviceId", deviceId);
        result.put("tagCount", tags.size());
        result.put("tags", tags);
        return result;
    }

    /** Reject missing device identifiers before mutating relations. */
    private void requireDeviceId(Long deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId is required");
        }
    }
}
