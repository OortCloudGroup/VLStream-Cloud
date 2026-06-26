package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.excel.VlsTagManagementExcel;
import org.springblade.vlstream.mapper.VlsDeviceTagRelationMapper;
import org.springblade.vlstream.mapper.VlsTagManagementMapper;
import org.springblade.vlstream.pojo.dto.TagManagementDTO;
import org.springblade.vlstream.pojo.entity.TagManagement;
import org.springblade.vlstream.pojo.vo.TagManagementVO;
import org.springblade.vlstream.service.IVlsTagManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Label Management Table Service Implementation Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsTagManagementServiceImpl extends BaseServiceImpl<VlsTagManagementMapper, TagManagement> implements IVlsTagManagementService {

	@Resource
	private VlsTagManagementMapper tagManagementMapper;

	@Resource
	private VlsDeviceTagRelationMapper deviceTagRelationMapper;


	@Override
	public IPage<TagManagementVO> selectVlsTagManagementPage(IPage<TagManagementVO> page, TagManagementVO vlsTagManagement) {
		return page.setRecords(baseMapper.selectVlsTagManagementPage(page, vlsTagManagement));
	}

	@Override
	public List<VlsTagManagementExcel> exportVlsTagManagement(Wrapper<TagManagement> queryWrapper) {
		List<VlsTagManagementExcel> vlsTagManagementList = baseMapper.exportVlsTagManagement(queryWrapper);
		//vlsTagManagementList.forEach(vlsTagManagement -> {
		//	vlsTagManagement.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsTagManagementEntity.getType()));
		//});
		return vlsTagManagementList;
	}

	@Override
	public List<TagManagementDTO> getTagTree() {
		List<TagManagement> allTags = tagManagementMapper.selectTagTree();
		return buildTree(allTags);
	}

	@Override
	public List<TagManagementDTO> getTagTreeByType(String tagType) {
		List<TagManagement> tags = tagManagementMapper.selectTagTreeByType(tagType);
		return buildTree(tags);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TagManagement createTag(TagManagement tagManagement) {
		// Verify if the tag name is duplicate
		if (isTagNameDuplicate(tagManagement.getTagName(), tagManagement.getParentId(), null)) {
			throw new RuntimeException("Label name already exists");
		}

		// Set sort number
		if (tagManagement.getSortOrder() == null) {
			Integer maxSort = tagManagementMapper.getMaxSortOrder(tagManagement.getParentId());
			tagManagement.setSortOrder(maxSort + 1);
		}

		// Set hierarchy
		if (tagManagement.getParentId() == null) {
			tagManagement.setLevel(0); // Root level
		} else {
			TagManagement parent = getById(tagManagement.getParentId());
			if (parent != null) {
				tagManagement.setLevel(parent.getLevel() + 1);
				tagManagement.setCategoryType(parent.getCategoryType()); // Inherit parent's type
			}
		}

		// Set default value
		if (tagManagement.getIsActive() == null) {
			tagManagement.setIsActive(1);
		}
		if (tagManagement.getUsageCount() == null) {
			tagManagement.setUsageCount(0);
		}

		save(tagManagement);
		return tagManagement;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TagManagement updateTag(TagManagement tagManagement) {
		TagManagement existingTag = getById(tagManagement.getId());
		if (existingTag == null) {
			throw new RuntimeException("Label does not exist");
		}

		// Verify if the tag name is duplicate
		if (isTagNameDuplicate(tagManagement.getTagName(), existingTag.getParentId(), tagManagement.getId())) {
			throw new RuntimeException("Label name already exists");
		}

		// Update field
		existingTag.setTagName(tagManagement.getTagName());
		existingTag.setTagColor(tagManagement.getTagColor());
		existingTag.setTagIcon(tagManagement.getTagIcon());
		existingTag.setDescription(tagManagement.getDescription());
		existingTag.setIsActive(tagManagement.getIsActive());

		updateById(existingTag);
		return existingTag;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteTag(Long tagId) {
		TagManagement tag = getById(tagId);
		if (tag == null) {
			return false;
		}

		// If it is root level tag, deletion is not allowed
		if (tag.getLevel() == 0) {
			throw new RuntimeException("Cannot delete root label classification");
		}

		// Recursively delete child tags
		List<TagManagement> children = tagManagementMapper.selectChildrenByParentId(tagId);
		for (TagManagement child : children) {
			deleteTag(child.getId());
		}

		// Delete device tag association
		deviceTagRelationMapper.deleteByTagId(tagId);

		// Delete tag
		removeById(tagId);

		log.info("Deleted tag successfully, tag ID: {}, tag name: {}", tagId, tag.getTagName());
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteTags(List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return false;
		}

		for (Long tagId : tagIds) {
			deleteTag(tagId);
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean moveTag(Long tagId, Long targetParentId, Integer targetPosition) {
		TagManagement tag = getById(tagId);
		if (tag == null) {
			return false;
		}

		// Update parent and level
		if (!Objects.equals(tag.getParentId(), targetParentId)) {
			tag.setParentId(targetParentId);

			if (targetParentId == null) {
				tag.setLevel(0);
			} else {
				TagManagement parent = getById(targetParentId);
				if (parent != null) {
					tag.setLevel(parent.getLevel() + 1);
					tag.setCategoryType(parent.getCategoryType());
				}
			}
		}

		// Update sorting
		if (targetPosition != null) {
			tag.setSortOrder(targetPosition);
		}

		updateById(tag);
		return true;
	}

	@Override
	public void updateUsageCount(Long tagId, Integer increment) {
		tagManagementMapper.updateUsageCount(tagId, increment);
	}

	@Override
	public boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId) {
		int count = tagManagementMapper.checkTagNameExists(tagName, parentId, excludeId);
		return count > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean toggleTagStatus(Long tagId, boolean isActive) {
		TagManagement tag = getById(tagId);
		if (tag == null) {
			return false;
		}

		tag.setIsActive(isActive ? 1 : 0);
		updateById(tag);
		return true;
	}

	@Override
	public TagManagement getTagUsageStats(Long tagId) {
		TagManagement tag = getById(tagId);
		if (tag != null) {
			// More statistical information can be added here
			int deviceCount = deviceTagRelationMapper.selectByTagId(tagId).size();
			tag.setUsageCount(deviceCount);
		}
		return tag;
	}

	/**
	 * Build Tree Structure
	 *
	 * @param allTags all tags
	 * @return tree structure
	 */
	private List<TagManagementDTO> buildTree(List<TagManagement> allTags) {
		if (allTags == null || allTags.isEmpty()) {
			return new ArrayList<>();
		}

		// Group by parent ID
		Map<Long, List<TagManagement>> parentMap = allTags.stream()
			.collect(Collectors.groupingBy(tag -> tag.getParentId() == null ? 0L : tag.getParentId()));

		// Recursively build tree
		return buildTreeRecursive(parentMap, 0L);
	}

	/**
	 * Recursively build tree structure
	 *
	 * @param parentMap parent grouping
	 * @param parentId parent ID
	 * @return child node list
	 */
	private List<TagManagementDTO> buildTreeRecursive(Map<Long, List<TagManagement>> parentMap, Long parentId) {
		List<TagManagement> children = parentMap.get(parentId);
		if (children == null || children.isEmpty()) {
			return new ArrayList<>();
		}
		List<TagManagementDTO> childrenList = new ArrayList<>();
		for (TagManagement child : children) {
			TagManagementDTO dto = new TagManagementDTO();
			BeanUtil.copy(child, dto);
			childrenList.add(dto);
		}
		for (TagManagementDTO child : childrenList) {
			List<TagManagementDTO> grandChildren = buildTreeRecursive(parentMap, child.getId());
			child.setChildren(grandChildren);
		}

		return childrenList;
	}

	@Override
	public List<TagManagement> getTagsByCategory(String categoryType) {
		LambdaQueryWrapper<TagManagement> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TagManagement::getCategoryType, categoryType)
			.orderByAsc(TagManagement::getLevel)
			.orderByAsc(TagManagement::getSortOrder);

		return list(queryWrapper);
	}

	@Override
	public IPage<TagManagement> getTagManagementPage(Page<TagManagement> page, String keyword, String categoryType, Integer level, Long parentId, Long tagId) {
		LambdaQueryWrapper<TagManagement> queryWrapper = new LambdaQueryWrapper<>();

		// Precise query by ID (highest priority)
		if (tagId != null) {
			queryWrapper.eq(TagManagement::getId, tagId);
			return page(page, queryWrapper);
		}

		// Keyword search - supports Label Name and Description
		if (keyword != null && !keyword.trim().isEmpty()) {
			queryWrapper.and(wrapper -> wrapper
				.like(TagManagement::getTagName, keyword.trim())
				.or()
				.like(TagManagement::getDescription, keyword.trim())
			);
		}

		// Filter by label category
		if (categoryType != null && !categoryType.trim().isEmpty()) {
			queryWrapper.eq(TagManagement::getCategoryType, categoryType);
		}

		// Filter by hierarchy
		if (level != null) {
			queryWrapper.eq(TagManagement::getLevel, level);
		}

		// Filter by parent ID
		if (parentId != null) {
			queryWrapper.eq(TagManagement::getParentId, parentId);
		}

		// Sort
		queryWrapper.orderByAsc(TagManagement::getCategoryType)
			.orderByAsc(TagManagement::getLevel)
			.orderByAsc(TagManagement::getSortOrder)
			.orderByAsc(TagManagement::getCreateTime);

		return page(page, queryWrapper);
	}

}
