/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTagManagementExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceTagRelationMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsTagManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TagManagementDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TagManagementVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsTagManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 标签管理表 服务实现类
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
		// 验证标签名称是否重复
		if (isTagNameDuplicate(tagManagement.getTagName(), tagManagement.getParentId(), null)) {
			throw new RuntimeException("标签名称已存在");
		}

		// 设置排序号
		if (tagManagement.getSortOrder() == null) {
			Integer maxSort = tagManagementMapper.getMaxSortOrder(tagManagement.getParentId());
			tagManagement.setSortOrder(maxSort + 1);
		}

		// 设置层级
		if (tagManagement.getParentId() == null) {
			tagManagement.setLevel(0); // 根级
		} else {
			TagManagement parent = getById(tagManagement.getParentId());
			if (parent != null) {
				tagManagement.setLevel(parent.getLevel() + 1);
				tagManagement.setCategoryType(parent.getCategoryType()); // 继承父级的类型
			}
		}

		// 设置默认值
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
			throw new RuntimeException("标签不存在");
		}

		// 验证标签名称是否重复
		if (isTagNameDuplicate(tagManagement.getTagName(), existingTag.getParentId(), tagManagement.getId())) {
			throw new RuntimeException("标签名称已存在");
		}

		// 更新字段
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

		// 如果是根级标签，不允许删除
		if (tag.getLevel() == 0) {
			throw new RuntimeException("不能删除根级标签分类");
		}

		// 递归删除子标签
		List<TagManagement> children = tagManagementMapper.selectChildrenByParentId(tagId);
		for (TagManagement child : children) {
			deleteTag(child.getId());
		}

		// 删除设备标签关联
		deviceTagRelationMapper.deleteByTagId(tagId);

		// 删除标签
		removeById(tagId);

		log.info("删除标签成功，标签ID: {}, 标签名称: {}", tagId, tag.getTagName());
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

		// 更新父级和层级
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

		// 更新排序
		if (targetPosition != null) {
			tag.setSortOrder(targetPosition);
		}

		updateById(tag);
		return true;
	}

	@Override
	public void updateUsageCount(Long tagId, Integer increment) {
		UpdateWrapper<TagManagement> wrapper = new UpdateWrapper<>();
		wrapper.eq("id", tagId).setSql("usage_count = usage_count + " + increment);
		update(new TagManagement(), wrapper);
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
			// 可以在这里添加更多统计信息
			int deviceCount = deviceTagRelationMapper.selectByTagId(tagId).size();
			tag.setUsageCount(deviceCount);
		}
		return tag;
	}

	/**
	 * 构建树形结构
	 *
	 * @param allTags 所有标签
	 * @return 树形结构
	 */
	private List<TagManagementDTO> buildTree(List<TagManagement> allTags) {
		if (allTags == null || allTags.isEmpty()) {
			return new ArrayList<>();
		}

		// 按父级ID分组
		Map<Long, List<TagManagement>> parentMap = allTags.stream()
			.collect(Collectors.groupingBy(tag -> tag.getParentId() == null ? 0L : tag.getParentId()));

		// 递归构建树
		return buildTreeRecursive(parentMap, 0L);
	}

	/**
	 * 递归构建树形结构
	 *
	 * @param parentMap 父级分组
	 * @param parentId 父级ID
	 * @return 子节点列表
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

		// 按ID精确查询（优先级最高）
		if (tagId != null) {
			queryWrapper.eq(TagManagement::getId, tagId);
			return page(page, queryWrapper);
		}

		// 关键字搜索 - 支持标签名称和描述
		if (keyword != null && !keyword.trim().isEmpty()) {
			queryWrapper.and(wrapper -> wrapper
				.like(TagManagement::getTagName, keyword.trim())
				.or()
				.like(TagManagement::getDescription, keyword.trim())
			);
		}

		// 按标签大类过滤
		if (categoryType != null && !categoryType.trim().isEmpty()) {
			queryWrapper.eq(TagManagement::getCategoryType, categoryType);
		}

		// 按层级过滤
		if (level != null) {
			queryWrapper.eq(TagManagement::getLevel, level);
		}

		// 按父级ID过滤
		if (parentId != null) {
			queryWrapper.eq(TagManagement::getParentId, parentId);
		}

		// 排序
		queryWrapper.orderByAsc(TagManagement::getCategoryType)
			.orderByAsc(TagManagement::getLevel)
			.orderByAsc(TagManagement::getSortOrder)
			.orderByAsc(TagManagement::getCreateTime);

		return page(page, queryWrapper);
	}

}
