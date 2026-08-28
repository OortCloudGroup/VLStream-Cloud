/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceTagRelationExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceTagRelationMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsTagManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceTagRelationDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceTagRelation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceTagRelationVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceTagRelationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * device service
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsDeviceTagRelationServiceImpl extends BaseServiceImpl<VlsDeviceTagRelationMapper, DeviceTagRelation> implements IVlsDeviceTagRelationService {

	private final VlsTagManagementMapper tagManagementMapper;

	@Override
	public IPage<DeviceTagRelationVO> selectVlsDeviceTagRelationPage(IPage<DeviceTagRelationVO> page, DeviceTagRelationVO vlsDeviceTagRelation) {
		return page.setRecords(baseMapper.selectVlsDeviceTagRelationPage(page, vlsDeviceTagRelation));
	}

	@Override
	public List<VlsDeviceTagRelationExcel> exportVlsDeviceTagRelation(Wrapper<DeviceTagRelation> queryWrapper) {
		List<VlsDeviceTagRelationExcel> vlsDeviceTagRelationList = baseMapper.exportVlsDeviceTagRelation(queryWrapper);
		//vlsDeviceTagRelationList.forEach(vlsDeviceTagRelation -> {
		//	vlsDeviceTagRelation.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsDeviceTagRelationEntity.getType()));
		//});
		return vlsDeviceTagRelationList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setDeviceTags(Long deviceId, List<Long> tagIds, Long createdBy) {
		// Delete device all
		baseMapper.deleteByDeviceId(deviceId);

		// if new ,
		if (tagIds != null && !tagIds.isEmpty()) {
			//
			List<Long> validTagIds = validateAndFilterTagIds(tagIds);
			if (!validTagIds.isEmpty()) {
				saveDeviceTags(deviceId, validTagIds);
			}
		}

		log.info("设置设备标签成功: deviceId={}, tagIds={}", deviceId, tagIds);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean addDeviceTags(Long deviceId, List<Long> tagIds, Long createdBy) {
		if (tagIds == null || tagIds.isEmpty()) {
			return true;
		}

		// already in
		List<Long> existingTagIds = getDeviceTagIds(deviceId);
		List<Long> newTagIds = tagIds.stream()
			.distinct()
			.filter(tagId -> !existingTagIds.contains(tagId))
			.collect(Collectors.toList());

		if (!newTagIds.isEmpty()) {
			//
			List<Long> validTagIds = validateAndFilterTagIds(newTagIds);
			if (!validTagIds.isEmpty()) {
				saveDeviceTags(deviceId, validTagIds);
			}
		}

		log.info("添加设备标签成功: deviceId={}, newTagIds={}", deviceId, newTagIds);
		return true;
	}

	/**
	 * , 、 new field full fill .
	 */
	private void saveDeviceTags(Long deviceId, List<Long> tagIds) {
		List<DeviceTagRelation> relations = tagIds.stream().map(tagId -> {
			DeviceTagRelation relation = new DeviceTagRelation();
			relation.setDeviceId(deviceId);
			relation.setTagId(tagId);
			return relation;
		}).collect(Collectors.toList());
		saveBatch(relations);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeDeviceTags(Long deviceId, List<Long> tagIds) {
		try {
			if (tagIds == null || tagIds.isEmpty()) {
				return true;
			}

			int deleted = baseMapper.deleteDeviceTagsBatch(deviceId, tagIds);
			log.info("移除设备标签成功: deviceId={}, tagIds={}, deleted={}", deviceId, tagIds, deleted);
			return true;
		} catch (Exception e) {
			log.error("移除设备标签失败: deviceId={}, tagIds={}", deviceId, tagIds, e);
			return false;
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean clearDeviceTags(Long deviceId) {
		try {
			int deleted = baseMapper.deleteByDeviceId(deviceId);
			log.info("清除设备标签成功: deviceId={}, deleted={}", deviceId, deleted);
			return true;
		} catch (Exception e) {
			log.error("清除设备标签失败: deviceId={}", deviceId, e);
			return false;
		}
	}

	@Override
	public List<DeviceTagRelationDTO> getDeviceTags(Long deviceId) {
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
	public int batchSetDeviceTags(Map<Long, List<Long>> deviceTagMap, Long createdBy) {
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
	public boolean copyDeviceTags(Long sourceDeviceId, List<Long> targetDeviceIds, Long createdBy) {
		// Get device
		List<Long> sourceTagIds = getDeviceTagIds(sourceDeviceId);
		if (sourceTagIds.isEmpty()) {
			log.info("源设备无标签，无需复制: sourceDeviceId={}", sourceDeviceId);
			return true;
		}

		// to each deviceSet
		for (Long targetDeviceId : targetDeviceIds) {
			setDeviceTags(targetDeviceId, sourceTagIds, createdBy);
		}

		log.info("复制设备标签成功: sourceDeviceId={}, targetDeviceIds={}, tagIds={}",
			sourceDeviceId, targetDeviceIds, sourceTagIds);
		return true;
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
				// whether in
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
		List<DeviceTagRelationDTO> deviceTags = getDeviceTags(deviceId);

		Map<String, Object> result = new HashMap<>();
		result.put("deviceId", deviceId);
		result.put("totalCount", deviceTags.size());

		// group
		Map<String, List<DeviceTagRelationDTO>> tagsByCategory = deviceTags.stream()
			.collect(Collectors.groupingBy(DeviceTagRelationDTO::getCategoryType));

		result.put("ownTags", tagsByCategory.getOrDefault("own", new ArrayList<>()));
		result.put("publicTags", tagsByCategory.getOrDefault("public", new ArrayList<>()));
		result.put("ownTagCount", tagsByCategory.getOrDefault("own", new ArrayList<>()).size());
		result.put("publicTagCount", tagsByCategory.getOrDefault("public", new ArrayList<>()).size());

		//
		List<String> tagNames = deviceTags.stream()
			.map(DeviceTagRelationDTO::getTagName)
			.collect(Collectors.toList());
		result.put("tagNames", tagNames);

		return result;
	}

	@Override
	public List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level) {
		// need to
		// DeviceInfoMapper and TagManagementMapper Query
		return new ArrayList<>();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean syncTagUsageCount() {
		try {
			// Get
			List<Map<String, Object>> usageStats = getTagUsageStatistics();

			// new each
			for (Map<String, Object> stat : usageStats) {
				Long tagId = (Long) stat.get("tag_id");
				Long deviceCount = (Long) stat.get("device_count");

				// Set tag_management in usage_countfield
				TagManagement tag = new TagManagement();
				tag.setId(tagId);
				tag.setUsageCount(deviceCount.intValue());
				tagManagementMapper.updateById(tag);
			}

			log.info("同步标签使用计数成功，更新了 {} 个标签", usageStats.size());
			return true;
		} catch (Exception e) {
			log.error("同步标签使用计数失败", e);
			return false;
		}
	}

	/**
	 * ID
	 *
	 * @param tagIds ID
	 * @return ID
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
					log.warn("验证标签ID失败: tagId={}", tagId, e);
					return false;
				}
			})
			.collect(Collectors.toList());
	}
}
