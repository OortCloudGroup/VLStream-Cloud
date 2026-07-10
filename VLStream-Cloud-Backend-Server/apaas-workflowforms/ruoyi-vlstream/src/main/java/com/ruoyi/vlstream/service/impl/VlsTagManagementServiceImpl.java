package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.TagManagement;
import com.ruoyi.vlstream.mapper.VlsTagManagementMapper;
import com.ruoyi.vlstream.service.IVlsTagManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for the VLS tag management frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsTagManagementServiceImpl implements IVlsTagManagementService {

    private final VlsTagManagementMapper tagManagementMapper;

    @Override
    public BladePage<TagManagement> getTagManagementPage(long current, long size, String keyword, String categoryType,
                                                         Integer level, Long parentId, Long tagId) {
        Page<TagManagement> page = new Page<TagManagement>(current, size);
        LambdaQueryWrapper<TagManagement> queryWrapper = new LambdaQueryWrapper<TagManagement>();

        if (tagId != null) {
            queryWrapper.eq(TagManagement::getId, tagId);
            Page<TagManagement> result = tagManagementMapper.selectPage(page, queryWrapper);
            prepareAliases(result.getRecords());
            return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
        }

        if (StringUtils.hasText(keyword)) {
            final String trimmed = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                .like(TagManagement::getTagName, trimmed)
                .or()
                .like(TagManagement::getDescription, trimmed));
        }
        if (StringUtils.hasText(categoryType)) {
            queryWrapper.eq(TagManagement::getCategoryType, categoryType.trim());
        }
        if (level != null) {
            queryWrapper.eq(TagManagement::getLevel, level);
        }
        if (parentId != null) {
            queryWrapper.eq(TagManagement::getParentId, parentId);
        }

        queryWrapper.orderByAsc(TagManagement::getCategoryType)
            .orderByAsc(TagManagement::getLevel)
            .orderByAsc(TagManagement::getSortOrder)
            .orderByAsc(TagManagement::getCreateTime);

        Page<TagManagement> result = tagManagementMapper.selectPage(page, queryWrapper);
        prepareAliases(result.getRecords());
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public List<Map<String, Object>> getTagTree() {
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        roots.add(rootNode("own", "自有标签", tagManagementMapper.selectByCategory("own")));
        roots.add(rootNode("public", "公共标签", tagManagementMapper.selectByCategory("public")));
        return roots;
    }

    @Override
    public TagManagement getTag(Long id) {
        TagManagement tag = tagManagementMapper.selectById(id);
        prepareAlias(tag);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagManagement createTag(TagManagement tagManagement) {
        normalizeWriteFields(tagManagement);
        if (isTagNameDuplicate(tagManagement.getTagName(), tagManagement.getParentId(), null)) {
            throw new IllegalArgumentException("标签名称已存在");
        }
        if (tagManagement.getSortOrder() == null) {
            Integer maxSort = tagManagementMapper.getMaxSortOrder(tagManagement.getParentId());
            tagManagement.setSortOrder((maxSort == null ? 0 : maxSort) + 1);
        }
        if (tagManagement.getParentId() == null) {
            if (tagManagement.getLevel() == null) {
                tagManagement.setLevel(1);
            }
        } else {
            TagManagement parent = tagManagementMapper.selectById(tagManagement.getParentId());
            if (parent != null) {
                tagManagement.setLevel((parent.getLevel() == null ? 0 : parent.getLevel()) + 1);
                tagManagement.setCategoryType(parent.getCategoryType());
            }
        }
        if (tagManagement.getIsActive() == null) {
            tagManagement.setIsActive(1);
        }
        if (tagManagement.getUsageCount() == null) {
            tagManagement.setUsageCount(0);
        }
        if (tagManagement.getIsDeleted() == null) {
            tagManagement.setIsDeleted(0);
        }
        tagManagementMapper.insert(tagManagement);
        prepareAlias(tagManagement);
        return tagManagement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagManagement updateTag(Long id, TagManagement tagManagement) {
        TagManagement existing = tagManagementMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        tagManagement.setId(id);
        normalizeWriteFields(tagManagement);
        Long duplicateParentId = tagManagement.getParentId() == null ? existing.getParentId() : tagManagement.getParentId();
        if (StringUtils.hasText(tagManagement.getTagName())
            && isTagNameDuplicate(tagManagement.getTagName(), duplicateParentId, id)) {
            throw new IllegalArgumentException("标签名称已存在");
        }

        existing.setTagName(firstText(tagManagement.getTagName(), existing.getTagName()));
        existing.setDescription(firstText(tagManagement.getDescription(), existing.getDescription()));
        existing.setTagColor(firstText(tagManagement.getTagColor(), existing.getTagColor()));
        existing.setTagIcon(firstText(tagManagement.getTagIcon(), existing.getTagIcon()));
        existing.setIsActive(tagManagement.getIsActive() == null ? existing.getIsActive() : tagManagement.getIsActive());
        existing.setSortOrder(tagManagement.getSortOrder() == null ? existing.getSortOrder() : tagManagement.getSortOrder());
        existing.setCategoryType(firstText(tagManagement.getCategoryType(), existing.getCategoryType()));
        existing.setLevel(tagManagement.getLevel() == null ? existing.getLevel() : tagManagement.getLevel());
        tagManagementMapper.updateById(existing);
        prepareAlias(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Long id) {
        TagManagement existing = tagManagementMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        List<TagManagement> children = tagManagementMapper.selectChildrenByParentId(id);
        for (TagManagement child : children) {
            deleteTag(child.getId());
        }
        return tagManagementMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTags(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean result = true;
        for (Long id : ids) {
            result = deleteTag(id) && result;
        }
        return result;
    }

    @Override
    public Map<String, Object> getStatistics() {
        long total = count(new LambdaQueryWrapper<TagManagement>());
        long own = count(new LambdaQueryWrapper<TagManagement>().eq(TagManagement::getCategoryType, "own"));
        long publicCount = count(new LambdaQueryWrapper<TagManagement>().eq(TagManagement::getCategoryType, "public"));
        long active = count(new LambdaQueryWrapper<TagManagement>().eq(TagManagement::getIsActive, 1));
        long inactive = count(new LambdaQueryWrapper<TagManagement>().eq(TagManagement::getIsActive, 0));

        Map<String, Object> statistics = new LinkedHashMap<String, Object>();
        statistics.put("total", total);
        statistics.put("totalTags", total);
        statistics.put("own", own);
        statistics.put("ownTags", own);
        statistics.put("public", publicCount);
        statistics.put("publicTags", publicCount);
        statistics.put("active", active);
        statistics.put("activeTags", active);
        statistics.put("inactive", inactive);
        statistics.put("inactiveTags", inactive);
        return statistics;
    }

    @Override
    public TagManagement getTagUsageStats(Long id) {
        return getTag(id);
    }

    @Override
    public boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId) {
        if (!StringUtils.hasText(tagName)) {
            return false;
        }
        return tagManagementMapper.checkTagNameExists(tagName.trim(), parentId, excludeId) > 0;
    }

    @Override
    public List<TagManagement> getChildren(Long parentId) {
        List<TagManagement> children = tagManagementMapper.selectChildrenByParentId(parentId);
        prepareAliases(children);
        return children;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveTag(Long id, Long newParentId, Integer targetPosition) {
        TagManagement tag = tagManagementMapper.selectById(id);
        if (tag == null) {
            return false;
        }
        if (!Objects.equals(tag.getParentId(), newParentId)) {
            tag.setParentId(newParentId);
            if (newParentId == null) {
                tag.setLevel(1);
            } else {
                TagManagement parent = tagManagementMapper.selectById(newParentId);
                if (parent != null) {
                    tag.setLevel((parent.getLevel() == null ? 0 : parent.getLevel()) + 1);
                    tag.setCategoryType(parent.getCategoryType());
                }
            }
        }
        if (targetPosition != null) {
            tag.setSortOrder(targetPosition);
        }
        return tagManagementMapper.updateById(tag) > 0;
    }

    @Override
    public void updateUsageCount(Long tagId, Integer increment) {
        tagManagementMapper.updateUsageCount(tagId, increment == null ? 0 : increment);
    }

    private long count(LambdaQueryWrapper<TagManagement> queryWrapper) {
        Long count = tagManagementMapper.selectCount(queryWrapper);
        return count == null ? 0L : count;
    }

    private Map<String, Object> rootNode(String id, String label, List<TagManagement> tags) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("id", id);
        root.put("tagName", label);
        root.put("categoryType", id);
        root.put("tagType", id);
        root.put("level", 0);
        root.put("children", buildTree(tags));
        return root;
    }

    private List<Map<String, Object>> buildTree(List<TagManagement> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<TagManagement>> byParent = new HashMap<Long, List<TagManagement>>();
        for (TagManagement tag : tags) {
            Long parentKey = tag.getParentId() == null ? 0L : tag.getParentId();
            if (!byParent.containsKey(parentKey)) {
                byParent.put(parentKey, new ArrayList<TagManagement>());
            }
            byParent.get(parentKey).add(tag);
        }
        return buildChildren(byParent, 0L);
    }

    private List<Map<String, Object>> buildChildren(Map<Long, List<TagManagement>> byParent, Long parentId) {
        List<TagManagement> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(children, TAG_ORDER);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (TagManagement child : children) {
            Map<String, Object> node = toNode(child);
            node.put("children", buildChildren(byParent, child.getId()));
            result.add(node);
        }
        return result;
    }

    private Map<String, Object> toNode(TagManagement tag) {
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("id", tag.getId());
        node.put("tagName", tag.getTagName());
        node.put("categoryType", tag.getCategoryType());
        node.put("tagType", tag.getCategoryType());
        node.put("level", tag.getLevel());
        node.put("parentId", tag.getParentId());
        node.put("sortOrder", tag.getSortOrder());
        node.put("position", tag.getSortOrder());
        node.put("tagColor", tag.getTagColor());
        node.put("tagIcon", tag.getTagIcon());
        node.put("description", tag.getDescription());
        node.put("isActive", tag.getIsActive());
        node.put("usageCount", tag.getUsageCount());
        node.put("createTime", tag.getCreateTime());
        node.put("updateTime", tag.getUpdateTime());
        return node;
    }

    private void normalizeWriteFields(TagManagement tagManagement) {
        if (!StringUtils.hasText(tagManagement.getCategoryType()) && StringUtils.hasText(tagManagement.getTagType())) {
            tagManagement.setCategoryType(tagManagement.getTagType().trim());
        }
        if (tagManagement.getSortOrder() == null && tagManagement.getPosition() != null) {
            tagManagement.setSortOrder(tagManagement.getPosition());
        }
    }

    private void prepareAliases(List<TagManagement> tags) {
        if (tags == null) {
            return;
        }
        for (TagManagement tag : tags) {
            prepareAlias(tag);
        }
    }

    private void prepareAlias(TagManagement tag) {
        if (tag == null) {
            return;
        }
        tag.setTagType(tag.getCategoryType());
        tag.setPosition(tag.getSortOrder());
    }

    private String firstText(String candidate, String fallback) {
        return StringUtils.hasText(candidate) ? candidate : fallback;
    }

    private static final Comparator<TagManagement> TAG_ORDER = new Comparator<TagManagement>() {
        @Override
        public int compare(TagManagement left, TagManagement right) {
            int sort = nullableInteger(left.getSortOrder()).compareTo(nullableInteger(right.getSortOrder()));
            if (sort != 0) {
                return sort;
            }
            return nullableLong(left.getId()).compareTo(nullableLong(right.getId()));
        }
    };

    private static Integer nullableInteger(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private static Long nullableLong(Long value) {
        return value == null ? Long.MAX_VALUE : value;
    }
}
