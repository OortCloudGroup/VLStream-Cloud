package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.TagManagement;

import java.util.List;

/**
 * Tag management service interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface TagManagementService extends IService<TagManagement> {

    /**
     * Get tag tree structure
     *
     * @return Tag tree
     */
    List<TagManagement> getTagTree();

    /**
     * Get tag tree by type
     *
     * @param tagType Tag type (own-private, public-public)
     * @return Tag tree
     */
    List<TagManagement> getTagTreeByType(String tagType);

    /**
     * Create tag
     *
     * @param tagManagement Tag information
     * @return Created tag
     */
    TagManagement createTag(TagManagement tagManagement);

    /**
     * Update tag
     *
     * @param tagManagement Tag information
     * @return Updated tag
     */
    TagManagement updateTag(TagManagement tagManagement);

    /**
     * Delete tag (cascade delete child tags)
     *
     * @param tagId Tag ID
     * @return Whether deletion successful
     */
    boolean deleteTag(Long tagId);

    /**
     * Batch delete tags
     *
     * @param tagIds Tag ID list
     * @return Whether deletion successful
     */
    boolean deleteTags(List<Long> tagIds);

    /**
     * Move tag (adjust parent or sorting)
     *
     * @param tagId Tag ID
     * @param targetParentId Target parent ID
     * @param targetPosition Target position
     * @return Whether movement successful
     */
    boolean moveTag(Long tagId, Long targetParentId, Integer targetPosition);

    /**
     * Update tag usage count
     *
     * @param tagId Tag ID
     * @param increment Increment count
     */
    void updateUsageCount(Long tagId, Integer increment);

    /**
     * Check if tag name is duplicate (at the same level)
     *
     * @param tagName Tag name
     * @param parentId Parent ID
     * @param excludeId Excluded ID (for validation during editing)
     * @return Whether duplicate
     */
    boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId);

    /**
     * Enable/disable tag
     *
     * @param tagId Tag ID
     * @param isActive Whether to enable
     * @return Whether operation successful
     */
    boolean toggleTagStatus(Long tagId, boolean isActive);

    /**
     * Get tag usage statistics
     *
     * @param tagId Tag ID
     * @return Usage statistics
     */
    TagManagement getTagUsageStats(Long tagId);

    /**
     * Get tags by category type
     *
     * @param categoryType Category type (own-private, public-public)
     * @return Tag list
     */
    List<TagManagement> getTagsByCategory(String categoryType);

    /**
     * Query tag management with pagination
     *
     * @param page Pagination parameters
     * @param keyword Search keyword
     * @param categoryType Tag category
     * @param level Tag level
     * @param parentId Parent ID
     * @param tagId Tag ID
     * @return Pagination result
     */
    IPage<TagManagement> getTagManagementPage(Page<TagManagement> page, String keyword, String categoryType, Integer level, Long parentId, Long tagId);
} 