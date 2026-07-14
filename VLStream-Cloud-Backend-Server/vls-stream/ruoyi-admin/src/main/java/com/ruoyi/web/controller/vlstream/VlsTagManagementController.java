/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.TagManagement;
import com.ruoyi.vlstream.service.IVlsTagManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SpringBlade-compatible tag management routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsTagManagement")
public class VlsTagManagementController {

    private final IVlsTagManagementService tagManagementService;

    @GetMapping("/page")
    public BladeResult<BladePage<TagManagement>> page(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String categoryType,
                                                       @RequestParam(required = false) Integer level,
                                                       @RequestParam(required = false) Long parentId,
                                                       @RequestParam(required = false) Long tagId) {
        return BladeResult.success(tagManagementService.getTagManagementPage(
            current, size, keyword, categoryType, level, parentId, tagId));
    }

    @GetMapping("/tree")
    public BladeResult<List<Map<String, Object>>> tree() {
        return BladeResult.success(tagManagementService.getTagTree());
    }

    @GetMapping("/{id}")
    public BladeResult<TagManagement> getTag(@PathVariable Long id) {
        return BladeResult.success(tagManagementService.getTag(id));
    }

    @PostMapping
    public BladeResult<TagManagement> createTag(@RequestBody TagManagement tagManagement) {
        try {
            return BladeResult.success(tagManagementService.createTag(tagManagement));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<TagManagement> updateTag(@PathVariable Long id, @RequestBody TagManagement tagManagement) {
        try {
            return BladeResult.success(tagManagementService.updateTag(id, tagManagement));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Void> deleteTag(@PathVariable Long id) {
        return booleanResult(tagManagementService.deleteTag(id), "删除标签失败");
    }

    @DeleteMapping("/batch")
    public BladeResult<Void> deleteTags(@RequestBody List<Long> ids) {
        return booleanResult(tagManagementService.deleteTags(ids), "批量删除标签失败");
    }

    @GetMapping("/statistics")
    public BladeResult<Map<String, Object>> statistics() {
        return BladeResult.success(tagManagementService.getStatistics());
    }

    @GetMapping("/{id}/stats")
    public BladeResult<TagManagement> stats(@PathVariable Long id) {
        return BladeResult.success(tagManagementService.getTagUsageStats(id));
    }

    @GetMapping("/check-name")
    public BladeResult<Boolean> checkName(@RequestParam String tagName,
                                          @RequestParam(required = false) Long parentId,
                                          @RequestParam(required = false) Long excludeId) {
        return BladeResult.success(tagManagementService.isTagNameDuplicate(tagName, parentId, excludeId));
    }

    @GetMapping("/{parentId}/children")
    public BladeResult<List<TagManagement>> children(@PathVariable Long parentId) {
        return BladeResult.success(tagManagementService.getChildren(parentId));
    }

    @PutMapping("/{id}/move")
    public BladeResult<Void> moveTag(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long newParentId = toLong(body == null ? null : body.get("newParentId"));
        Integer targetPosition = toInteger(body == null ? null : body.get("targetPosition"));
        return booleanResult(tagManagementService.moveTag(id, newParentId, targetPosition), "移动标签失败");
    }

    private BladeResult<Void> booleanResult(boolean success, String message) {
        return success ? BladeResult.success() : BladeResult.fail(message);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.valueOf(text);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Integer.valueOf(text);
    }
}
