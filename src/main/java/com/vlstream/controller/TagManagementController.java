package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.TagManagement;
import com.vlstream.service.TagManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tag Management Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tag-management")
@Api(tags = "Tag Management")
public class TagManagementController {

    @Autowired
    private TagManagementService tagManagementService;

    @GetMapping("/page")
    @ApiOperation("Page query tag information")
    public Result<IPage<TagManagement>> pageTagManagement(
            @ApiParam("Current page") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("Search keyword") @RequestParam(required = false) String keyword,
            @ApiParam("Tag category") @RequestParam(required = false) String categoryType,
            @ApiParam("Tag level") @RequestParam(required = false) Integer level,
            @ApiParam("Parent ID") @RequestParam(required = false) Long parentId,
            @ApiParam("Tag ID") @RequestParam(required = false) Long tagId) {
        try {
            Page<TagManagement> page = new Page<>(current, size);
            IPage<TagManagement> result = tagManagementService.getTagManagementPage(page, keyword, categoryType, level, parentId, tagId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to query tags by page", e);
            return Result.error("Failed to query tags by page: " + e.getMessage());
        }
    }

    @GetMapping("/tree")
    @ApiOperation("Get tag tree structure (for left navigation)")
    public Result<List<Map<String, Object>>> getTagTree() {
        try {
            // Get own tags and public tags data
            List<TagManagement> ownTags = tagManagementService.getTagsByCategory("own");
            List<TagManagement> publicTags = tagManagementService.getTagsByCategory("public");
            
            // Build return structure
            Map<String, Object> result = new HashMap<>();
            
            // Build own tag tree structure
            Map<String, Object> ownRoot = new HashMap<>();
            ownRoot.put("id", "own");
            ownRoot.put("tagName", "Own Tags");
            ownRoot.put("categoryType", "own");
            ownRoot.put("level", 0);
            ownRoot.put("children", buildTreeStructure(ownTags));
            
            // Build public tag tree structure  
            Map<String, Object> publicRoot = new HashMap<>();
            publicRoot.put("id", "public");
            publicRoot.put("tagName", "Public Tags");
            publicRoot.put("categoryType", "public");
            publicRoot.put("level", 0);
            publicRoot.put("children", buildTreeStructure(publicTags));
            
            List<Map<String, Object>> data = new ArrayList<>();
            data.add(ownRoot);
            data.add(publicRoot);
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get tag tree", e);
            return Result.error("Failed to get tag tree: " + e.getMessage());
        }
    }
    
    /**
     * Build tree structure
     */
    private List<Map<String, Object>> buildTreeStructure(List<TagManagement> tags) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有标签类型（level=1）
        List<TagManagement> categories = tags.stream()
            .filter(tag -> tag.getLevel() == 1)
            .sorted(Comparator.comparing(TagManagement::getSortOrder))
            .collect(Collectors.toList());
        
        // 为每个标签类型构建子标签
        for (TagManagement category : categories) {
            Map<String, Object> categoryNode = new HashMap<>();
            categoryNode.put("id", category.getId());
            categoryNode.put("tagName", category.getTagName());
            categoryNode.put("categoryType", category.getCategoryType());
            categoryNode.put("level", category.getLevel());
            categoryNode.put("parentId", category.getParentId());
            categoryNode.put("sortOrder", category.getSortOrder());
            categoryNode.put("tagColor", category.getTagColor());
            categoryNode.put("tagIcon", category.getTagIcon());
            categoryNode.put("description", category.getDescription());
            categoryNode.put("isActive", category.getIsActive());
            categoryNode.put("usageCount", category.getUsageCount());
            categoryNode.put("createdBy", category.getCreatedBy());
            categoryNode.put("updatedBy", category.getUpdatedBy());
            categoryNode.put("createTime", category.getCreateTime());
            categoryNode.put("updateTime", category.getUpdateTime());
            categoryNode.put("deleted", category.getDeleted());
            
            // 获取该类型下的所有具体标签
            List<TagManagement> subTags = tags.stream()
                .filter(tag -> tag.getLevel() == 2 && Objects.equals(tag.getParentId(), category.getId()))
                .sorted(Comparator.comparing(TagManagement::getSortOrder))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> children = new ArrayList<>();
            for (TagManagement subTag : subTags) {
                Map<String, Object> subNode = new HashMap<>();
                subNode.put("id", subTag.getId());
                subNode.put("tagName", subTag.getTagName());
                subNode.put("categoryType", subTag.getCategoryType());
                subNode.put("level", subTag.getLevel());
                subNode.put("parentId", subTag.getParentId());
                subNode.put("sortOrder", subTag.getSortOrder());
                subNode.put("tagColor", subTag.getTagColor());
                subNode.put("tagIcon", subTag.getTagIcon());
                subNode.put("description", subTag.getDescription());
                subNode.put("isActive", subTag.getIsActive());
                subNode.put("usageCount", subTag.getUsageCount());
                subNode.put("createdBy", subTag.getCreatedBy());
                subNode.put("updatedBy", subTag.getUpdatedBy());
                subNode.put("createTime", subTag.getCreateTime());
                subNode.put("updateTime", subTag.getUpdateTime());
                subNode.put("deleted", subTag.getDeleted());
                subNode.put("parentName", category.getTagName());
                
                children.add(subNode);
            }
            
            categoryNode.put("children", children);
            result.add(categoryNode);
        }
        
        return result;
    }

    @GetMapping("/tree/{tagType}")
    @ApiOperation("Get tag tree by type")
    public Result<List<TagManagement>> getTagTreeByType(
            @ApiParam(value = "Tag type (own-own, public-public)", required = true)
            @PathVariable String tagType) {
        try {
            List<TagManagement> tagTree = tagManagementService.getTagTreeByType(tagType);
            return Result.success(tagTree);
        } catch (Exception e) {
            log.error("Failed to get tag tree, type: {}", tagType, e);
            return Result.error("Failed to get tag tree: " + e.getMessage());
        }
    }

    @PostMapping
    @ApiOperation("Create tag")
    public Result<TagManagement> createTag(@RequestBody TagManagement tagManagement) {
        try {
            TagManagement createdTag = tagManagementService.createTag(tagManagement);
            return Result.success(createdTag);
        } catch (Exception e) {
            log.error("Failed to create tag", e);
            return Result.error("Failed to create tag: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation("Update tag")
    public Result<TagManagement> updateTag(
            @ApiParam(value = "Tag ID", required = true) @PathVariable Long id,
            @RequestBody TagManagement tagManagement) {
        try {
            tagManagement.setId(id);
            TagManagement updatedTag = tagManagementService.updateTag(tagManagement);
            return Result.success(updatedTag);
        } catch (Exception e) {
            log.error("Failed to update tag, ID: {}", id, e);
            return Result.error("Failed to update tag: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete tag")
    public Result<Void> deleteTag(
            @ApiParam(value = "Tag ID", required = true) @PathVariable Long id) {
        try {
            boolean success = tagManagementService.deleteTag(id);
            if (success) {
                return Result.success();
            } else {
                return Result.error("Failed to delete tag");
            }
        } catch (Exception e) {
            log.error("Failed to delete tag, ID: {}", id, e);
            return Result.error("Failed to delete tag: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @ApiOperation("Batch delete tags")
    public Result<Void> deleteTags(@RequestBody List<Long> tagIds) {
        try {
            boolean success = tagManagementService.deleteTags(tagIds);
            if (success) {
                return Result.success();
            } else {
                return Result.error("Failed to batch delete tags");
            }
        } catch (Exception e) {
            log.error("Failed to batch delete tags, IDs: {}", tagIds, e);
            return Result.error("Failed to batch delete tags: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/move")
    @ApiOperation("Move tag")
    public Result<Void> moveTag(
            @ApiParam(value = "Tag ID", required = true) @PathVariable Long id,
            @ApiParam(value = "Target parent ID") @RequestParam(required = false) Long targetParentId,
            @ApiParam(value = "Target position") @RequestParam(required = false) Integer targetPosition) {
        try {
            boolean success = tagManagementService.moveTag(id, targetParentId, targetPosition);
            if (success) {
                return Result.success();
            } else {
                return Result.error("Failed to move tag");
            }
        } catch (Exception e) {
            log.error("Failed to move tag, ID: {}", id, e);
            return Result.error("Failed to move tag: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/toggle-status")
    @ApiOperation("Enable/disable tag")
    public Result<Void> toggleTagStatus(
            @ApiParam(value = "Tag ID", required = true) @PathVariable Long id,
            @ApiParam(value = "Is active", required = true) @RequestParam boolean isActive) {
        try {
            boolean success = tagManagementService.toggleTagStatus(id, isActive);
            if (success) {
                return Result.success();
            } else {
                return Result.error("Failed to toggle tag status");
            }
        } catch (Exception e) {
            log.error("Failed to toggle tag status, ID: {}", id, e);
            return Result.error("Failed to toggle tag status: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/stats")
    @ApiOperation("Get tag usage statistics")
    public Result<TagManagement> getTagUsageStats(
            @ApiParam(value = "Tag ID", required = true) @PathVariable Long id) {
        try {
            TagManagement tagStats = tagManagementService.getTagUsageStats(id);
            return Result.success(tagStats);
        } catch (Exception e) {
            log.error("Failed to get tag statistics, ID: {}", id, e);
            return Result.error("Failed to get tag statistics: " + e.getMessage());
        }
    }

    @GetMapping("/check-name")
    @ApiOperation("Check if tag name is duplicate")
    public Result<Boolean> checkTagName(
            @ApiParam(value = "Tag name", required = true) @RequestParam String tagName,
            @ApiParam(value = "Parent ID") @RequestParam(required = false) Long parentId,
            @ApiParam(value = "Exclude ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean isDuplicate = tagManagementService.isTagNameDuplicate(tagName, parentId, excludeId);
            return Result.success(isDuplicate);
        } catch (Exception e) {
            log.error("Failed to check tag name", e);
            return Result.error("Failed to check tag name: " + e.getMessage());
        }
    }
} 