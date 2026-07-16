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
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceTagRelationVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceTagRelationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备标签关联表 服务实现类
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
		// 先删除设备的所有标签
		baseMapper.deleteByDeviceId(deviceId);

		// 如果有新标签，则批量插入
		if (tagIds != null && !tagIds.isEmpty()) {
			// 去重并过滤无效标签
			List<Long> validTagIds = validateAndFilterTagIds(tagIds);
			if (!validTagIds.isEmpty()) {
				baseMapper.batchInsertDeviceTags(deviceId, validTagIds, createdBy);
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

		// 去重并过滤已存在的标签
		List<Long> existingTagIds = getDeviceTagIds(deviceId);
		List<Long> newTagIds = tagIds.stream()
			.distinct()
			.filter(tagId -> !existingTagIds.contains(tagId))
			.collect(Collectors.toList());

		if (!newTagIds.isEmpty()) {
			// 验证标签有效性
			List<Long> validTagIds = validateAndFilterTagIds(newTagIds);
			if (!validTagIds.isEmpty()) {
				baseMapper.batchInsertDeviceTags(deviceId, validTagIds, createdBy);
			}
		}

		log.info("添加设备标签成功: deviceId={}, newTagIds={}", deviceId, newTagIds);
		return true;
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
		// 获取源设备的标签
		List<Long> sourceTagIds = getDeviceTagIds(sourceDeviceId);
		if (sourceTagIds.isEmpty()) {
			log.info("源设备无标签，无需复制: sourceDeviceId={}", sourceDeviceId);
			return true;
		}

		// 为每个目标设备设置标签
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
		List<DeviceTagRelationDTO> deviceTags = getDeviceTags(deviceId);

		Map<String, Object> result = new HashMap<>();
		result.put("deviceId", deviceId);
		result.put("totalCount", deviceTags.size());

		// 按类型分组
		Map<String, List<DeviceTagRelationDTO>> tagsByCategory = deviceTags.stream()
			.collect(Collectors.groupingBy(DeviceTagRelationDTO::getCategoryType));

		result.put("ownTags", tagsByCategory.getOrDefault("own", new ArrayList<>()));
		result.put("publicTags", tagsByCategory.getOrDefault("public", new ArrayList<>()));
		result.put("ownTagCount", tagsByCategory.getOrDefault("own", new ArrayList<>()).size());
		result.put("publicTagCount", tagsByCategory.getOrDefault("public", new ArrayList<>()).size());

		// 标签名称列表
		List<String> tagNames = deviceTags.stream()
			.map(DeviceTagRelationDTO::getTagName)
			.collect(Collectors.toList());
		result.put("tagNames", tagNames);

		return result;
	}

	@Override
	public List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level) {
		// 这里需要根据具体需求实现
		// 可以结合DeviceInfoMapper和TagManagementMapper来查询
		return new ArrayList<>();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean syncTagUsageCount() {
		try {
			// 获取标签使用统计
			List<Map<String, Object>> usageStats = getTagUsageStatistics();

			// 更新每个标签的使用次数
			for (Map<String, Object> stat : usageStats) {
				Long tagId = (Long) stat.get("tag_id");
				Long deviceCount = (Long) stat.get("device_count");

				// 设置tag_management表中的usage_count字段
				tagManagementMapper.setUsageCount(tagId, deviceCount.intValue());
			}

			log.info("同步标签使用计数成功，更新了 {} 个标签", usageStats.size());
			return true;
		} catch (Exception e) {
			log.error("同步标签使用计数失败", e);
			return false;
		}
	}

	/**
	 * 验证并过滤标签ID列表
	 *
	 * @param tagIds 原始标签ID列表
	 * @return 有效的标签ID列表
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
