package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.TagManagement;
import com.vlstream.mapper.TagManagementMapper;
import com.vlstream.mapper.DeviceTagRelationMapper;
import com.vlstream.service.TagManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tag Management Service Implementation
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TagManagementServiceImpl extends ServiceImpl<TagManagementMapper, TagManagement> implements TagManagementService {

    @Autowired
    private TagManagementMapper tagManagementMapper;

    @Autowired
    private DeviceTagRelationMapper deviceTagRelationMapper;

    @Override
    public List<TagManagement> getTagTree() {
        List<TagManagement> allTags = tagManagementMapper.selectTagTree();
        return buildTree(allTags);
    }

    @Override
    public List<TagManagement> getTagTreeByType(String tagType) {
        List<TagManagement> tags = tagManagementMapper.selectTagTreeByType(tagType);
        return buildTree(tags);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagManagement createTag(TagManagement tagManagement) {
        // Validate tag name duplication
        if (isTagNameDuplicate(tagManagement.getTagName(), tagManagement.getParentId(), null)) {
            throw new RuntimeException("Tag name already exists");
        }

        // Set sort order
        if (tagManagement.getSortOrder() == null) {
            Integer maxSort = tagManagementMapper.getMaxSortOrder(tagManagement.getParentId());
            tagManagement.setSortOrder(maxSort + 1);
        }

        // Set level
        if (tagManagement.getParentId() == null) {
            tagManagement.setLevel(0); // Root level
        } else {
            TagManagement parent = getById(tagManagement.getParentId());
            if (parent != null) {
                tagManagement.setLevel(parent.getLevel() + 1);
                tagManagement.setCategoryType(parent.getCategoryType()); // Inherit parent type
            }
        }

        // Set default values
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
            throw new RuntimeException("Tag does not exist");
        }

        // Validate tag name duplication
        if (isTagNameDuplicate(tagManagement.getTagName(), existingTag.getParentId(), tagManagement.getId())) {
            throw new RuntimeException("Tag name already exists");
        }

        // Update fields
        existingTag.setTagName(tagManagement.getTagName());
        existingTag.setTagColor(tagManagement.getTagColor());
        existingTag.setTagIcon(tagManagement.getTagIcon());
        existingTag.setDescription(tagManagement.getDescription());
        existingTag.setIsActive(tagManagement.getIsActive());
        existingTag.setUpdatedBy(tagManagement.getUpdatedBy());

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

        // Cannot delete root level tag
        if (tag.getLevel() == 0) {
            throw new RuntimeException("Cannot delete root level tag category");
        }

        // Recursively delete child tags
        List<TagManagement> children = tagManagementMapper.selectChildrenByParentId(tagId);
        for (TagManagement child : children) {
            deleteTag(child.getId());
        }

        // Delete device-tag relationships
        deviceTagRelationMapper.deleteByTagId(tagId);

        // Delete tag
        removeById(tagId);
        
        log.info("Tag deleted successfully, tag ID: {}, tag name: {}", tagId, tag.getTagName());
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
            // 可以在这里添加更多统计信息
            int deviceCount = deviceTagRelationMapper.selectByTagId(tagId).size();
            tag.setUsageCount(deviceCount);
        }
        return tag;
    }

    /**
     * Build tree structure
     *
     * @param allTags all tags
     * @return tree structure
     */
    private List<TagManagement> buildTree(List<TagManagement> allTags) {
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
    private List<TagManagement> buildTreeRecursive(Map<Long, List<TagManagement>> parentMap, Long parentId) {
        List<TagManagement> children = parentMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }

        for (TagManagement child : children) {
            List<TagManagement> grandChildren = buildTreeRecursive(parentMap, child.getId());
            child.setChildren(grandChildren);
        }

        return children;
    }

    @Override
    public List<TagManagement> getTagsByCategory(String categoryType) {
        LambdaQueryWrapper<TagManagement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TagManagement::getCategoryType, categoryType)
                   .eq(TagManagement::getDeleted, 0)
                   .orderByAsc(TagManagement::getLevel)
                   .orderByAsc(TagManagement::getSortOrder);
        
        return list(queryWrapper);
    }

    @Override
    public IPage<TagManagement> getTagManagementPage(Page<TagManagement> page, String keyword, String categoryType, Integer level, Long parentId, Long tagId) {
        LambdaQueryWrapper<TagManagement> queryWrapper = new LambdaQueryWrapper<>();
        
        // 基础查询条件
        queryWrapper.eq(TagManagement::getDeleted, 0);
        
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