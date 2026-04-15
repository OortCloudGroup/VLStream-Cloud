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
 * 标签管理 Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tag-management")
@Api(tags = "标签管理")
public class TagManagementController {

    @Autowired
    private TagManagementService tagManagementService;

    @GetMapping("/page")
    @ApiOperation("分页查询标签信息")
    public Result<IPage<TagManagement>> pageTagManagement(
            @ApiParam("当前页") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("搜索关键字") @RequestParam(required = false) String keyword,
            @ApiParam("标签大类") @RequestParam(required = false) String categoryType,
            @ApiParam("标签层级") @RequestParam(required = false) Integer level,
            @ApiParam("父级ID") @RequestParam(required = false) Long parentId,
            @ApiParam("标签ID") @RequestParam(required = false) Long tagId) {
        try {
            Page<TagManagement> page = new Page<>(current, size);
            IPage<TagManagement> result = tagManagementService.getTagManagementPage(page, keyword, categoryType, level, parentId, tagId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询标签失败", e);
            return Result.error("分页查询标签失败: " + e.getMessage());
        }
    }

    @GetMapping("/tree")
    @ApiOperation("获取标签树形结构（用于左侧导航）")
    public Result<List<Map<String, Object>>> getTagTree() {
        try {
            // 获取自有标签和公共标签数据
            List<TagManagement> ownTags = tagManagementService.getTagsByCategory("own");
            List<TagManagement> publicTags = tagManagementService.getTagsByCategory("public");
            
            // 构建返回结构
            Map<String, Object> result = new HashMap<>();
            
            // 构建自有标签树形结构
            Map<String, Object> ownRoot = new HashMap<>();
            ownRoot.put("id", "own");
            ownRoot.put("tagName", "自有标签");
            ownRoot.put("categoryType", "own");
            ownRoot.put("level", 0);
            ownRoot.put("children", buildTreeStructure(ownTags));
            
            // 构建公共标签树形结构  
            Map<String, Object> publicRoot = new HashMap<>();
            publicRoot.put("id", "public");
            publicRoot.put("tagName", "公共标签");
            publicRoot.put("categoryType", "public");
            publicRoot.put("level", 0);
            publicRoot.put("children", buildTreeStructure(publicTags));
            
            List<Map<String, Object>> data = new ArrayList<>();
            data.add(ownRoot);
            data.add(publicRoot);
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取标签树失败", e);
            return Result.error("获取标签树失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建树形结构
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
    @ApiOperation("根据类型获取标签树")
    public Result<List<TagManagement>> getTagTreeByType(
            @ApiParam(value = "标签类型(own-自有, public-公共)", required = true)
            @PathVariable String tagType) {
        try {
            List<TagManagement> tagTree = tagManagementService.getTagTreeByType(tagType);
            return Result.success(tagTree);
        } catch (Exception e) {
            log.error("获取标签树失败，类型: {}", tagType, e);
            return Result.error("获取标签树失败: " + e.getMessage());
        }
    }

    @PostMapping
    @ApiOperation("创建标签")
    public Result<TagManagement> createTag(@RequestBody TagManagement tagManagement) {
        try {
            TagManagement createdTag = tagManagementService.createTag(tagManagement);
            return Result.success(createdTag);
        } catch (Exception e) {
            log.error("创建标签失败", e);
            return Result.error("创建标签失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation("更新标签")
    public Result<TagManagement> updateTag(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long id,
            @RequestBody TagManagement tagManagement) {
        try {
            tagManagement.setId(id);
            TagManagement updatedTag = tagManagementService.updateTag(tagManagement);
            return Result.success(updatedTag);
        } catch (Exception e) {
            log.error("更新标签失败，ID: {}", id, e);
            return Result.error("更新标签失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除标签")
    public Result<Void> deleteTag(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long id) {
        try {
            boolean success = tagManagementService.deleteTag(id);
            if (success) {
                return Result.success();
            } else {
                return Result.error("删除标签失败");
            }
        } catch (Exception e) {
            log.error("删除标签失败，ID: {}", id, e);
            return Result.error("删除标签失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除标签")
    public Result<Void> deleteTags(@RequestBody List<Long> tagIds) {
        try {
            boolean success = tagManagementService.deleteTags(tagIds);
            if (success) {
                return Result.success();
            } else {
                return Result.error("批量删除标签失败");
            }
        } catch (Exception e) {
            log.error("批量删除标签失败，IDs: {}", tagIds, e);
            return Result.error("批量删除标签失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/move")
    @ApiOperation("移动标签")
    public Result<Void> moveTag(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long id,
            @ApiParam(value = "目标父级ID") @RequestParam(required = false) Long targetParentId,
            @ApiParam(value = "目标位置") @RequestParam(required = false) Integer targetPosition) {
        try {
            boolean success = tagManagementService.moveTag(id, targetParentId, targetPosition);
            if (success) {
                return Result.success();
            } else {
                return Result.error("移动标签失败");
            }
        } catch (Exception e) {
            log.error("移动标签失败，ID: {}", id, e);
            return Result.error("移动标签失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/toggle-status")
    @ApiOperation("启用/禁用标签")
    public Result<Void> toggleTagStatus(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long id,
            @ApiParam(value = "是否启用", required = true) @RequestParam boolean isActive) {
        try {
            boolean success = tagManagementService.toggleTagStatus(id, isActive);
            if (success) {
                return Result.success();
            } else {
                return Result.error("切换标签状态失败");
            }
        } catch (Exception e) {
            log.error("切换标签状态失败，ID: {}", id, e);
            return Result.error("切换标签状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/stats")
    @ApiOperation("获取标签使用统计")
    public Result<TagManagement> getTagUsageStats(
            @ApiParam(value = "标签ID", required = true) @PathVariable Long id) {
        try {
            TagManagement tagStats = tagManagementService.getTagUsageStats(id);
            return Result.success(tagStats);
        } catch (Exception e) {
            log.error("获取标签统计失败，ID: {}", id, e);
            return Result.error("获取标签统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/check-name")
    @ApiOperation("检查标签名称是否重复")
    public Result<Boolean> checkTagName(
            @ApiParam(value = "标签名称", required = true) @RequestParam String tagName,
            @ApiParam(value = "父级ID") @RequestParam(required = false) Long parentId,
            @ApiParam(value = "排除的ID") @RequestParam(required = false) Long excludeId) {
        try {
            boolean isDuplicate = tagManagementService.isTagNameDuplicate(tagName, parentId, excludeId);
            return Result.success(isDuplicate);
        } catch (Exception e) {
            log.error("检查标签名称失败", e);
            return Result.error("检查标签名称失败: " + e.getMessage());
        }
    }
} 