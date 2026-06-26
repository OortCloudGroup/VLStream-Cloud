package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsDeviceTagRelationExcel;
import org.springblade.vlstream.mapper.VlsDeviceTagRelationMapper;
import org.springblade.vlstream.mapper.VlsTagManagementMapper;
import org.springblade.vlstream.pojo.dto.DeviceTagRelationDTO;
import org.springblade.vlstream.pojo.entity.DeviceTagRelation;
import org.springblade.vlstream.pojo.vo.DeviceTagRelationVO;
import org.springblade.vlstream.service.IVlsDeviceTagRelationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Device Tag Association Table Service Implementation Class
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
	public boolean setDeviceTags(Long deviceId, List<Long> tagIds, String createdBy) {
		try {
			// Delete all tags of the device first
			baseMapper.deleteByDeviceId(deviceId);

			// If there are new tags, bulk insert them
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
				// Verify tag validity
				List<Long> validTagIds = validateAndFilterTagIds(newTagIds);
				if (!validTagIds.isEmpty()) {
					baseMapper.batchInsertDeviceTags(deviceId, validTagIds, createdBy);
				}
			}

			log.info("Added device tags successfully: deviceId={}, newTagIds={}", deviceId, newTagIds);
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
			log.info("Removed device tags successfully: deviceId={}, tagIds={}, deleted={}", deviceId, tagIds, deleted);
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
			log.info("Cleared device tags successfully: deviceId={}, deleted={}", deviceId, deleted);
			return true;
		} catch (Exception e) {
			log.error("Failed to clear device tags: deviceId={}", deviceId, e);
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
			// Get labels of source device
			List<Long> sourceTagIds = getDeviceTagIds(sourceDeviceId);
			if (sourceTagIds.isEmpty()) {
				log.info("Source device has no tags, no copy needed: sourceDeviceId={}", sourceDeviceId);
				return true;
			}

			// Set labels for each target device
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
				// Check if tag exists and is valid
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

		// Group by type
		Map<String, List<DeviceTagRelationDTO>> tagsByCategory = deviceTags.stream()
			.collect(Collectors.groupingBy(DeviceTagRelationDTO::getCategoryType));

		result.put("ownTags", tagsByCategory.getOrDefault("own", new ArrayList<>()));
		result.put("publicTags", tagsByCategory.getOrDefault("public", new ArrayList<>()));
		result.put("ownTagCount", tagsByCategory.getOrDefault("own", new ArrayList<>()).size());
		result.put("publicTagCount", tagsByCategory.getOrDefault("public", new ArrayList<>()).size());

		// Label Name List
		List<String> tagNames = deviceTags.stream()
			.map(DeviceTagRelationDTO::getTagName)
			.collect(Collectors.toList());
		result.put("tagNames", tagNames);

		return result;
	}

	@Override
	public List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level) {
		// This needs to be implemented based on specific requirements
		// Can query in combination with DeviceInfoMapper and TagManagementMapper
		return new ArrayList<>();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean syncTagUsageCount() {
		try {
			// Get label usage statistics
			List<Map<String, Object>> usageStats = getTagUsageStatistics();

			// Update usage count of each label
			for (Map<String, Object> stat : usageStats) {
				Long tagId = (Long) stat.get("tag_id");
				Long deviceCount = (Long) stat.get("device_count");

				// Set the usage_count field in the tag_management table
				tagManagementMapper.setUsageCount(tagId, deviceCount.intValue());
			}

			log.info("Synchronized tag usage count successfully, updated {} tags", usageStats.size());
			return true;
		} catch (Exception e) {
			log.error("Failed to synchronize tag usage count", e);
			return false;
		}
	}

	/**
	 * Verify and filter tag ID list
	 *
	 * @param tagIds original label ID list
	 * @return valid label ID list
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
					log.warn("Failed to verify tag ID: tagId={}", tagId, e);
					return false;
				}
			})
			.collect(Collectors.toList());
	}
}
