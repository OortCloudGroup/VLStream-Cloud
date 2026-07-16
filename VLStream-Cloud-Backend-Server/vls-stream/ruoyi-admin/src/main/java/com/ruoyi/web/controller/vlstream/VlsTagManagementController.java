/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpringBlade-compatible tag management routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsTagManagement")
public class VlsTagManagementController extends VlsControllerSupport {

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

    /** Return one tag through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<TagManagement> detail(@RequestParam Long id) {
        return getTag(id);
    }

    /** Return the tag page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<TagManagement>> list(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String categoryType,
                                                      @RequestParam(required = false) Integer level,
                                                      @RequestParam(required = false) Long parentId,
                                                      @RequestParam(required = false) Long tagId) {
        return page(current, size, keyword, categoryType, level, parentId, tagId);
    }

    /** Create a tag through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<TagManagement> save(@RequestBody TagManagement tag) {
        return createTag(tag);
    }

    /** Update a tag through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<TagManagement> update(@RequestBody TagManagement tag) {
        if (tag == null || tag.getId() == null) {
            return BladeResult.fail("Tag ID is required");
        }
        return updateTag(tag.getId(), tag);
    }

    /** Insert or update a tag through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<TagManagement> submit(@RequestBody TagManagement tag) {
        return tag != null && tag.getId() != null ? updateTag(tag.getId(), tag) : createTag(tag);
    }

    /** Delete tags by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(tagManagementService.deleteTags(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Return the requested real tag-category tree. */
    @GetMapping("/tree/{tagType}")
    public BladeResult<List<Map<String, Object>>> treeByType(@PathVariable String tagType) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> root : tagManagementService.getTagTree()) {
            if (tagType != null && tagType.equals(String.valueOf(root.get("categoryType")))) {
                result.add(root);
            }
        }
        return BladeResult.success(result);
    }

    /** Persist the requested active status on a real tag row. */
    @PutMapping("/{id}/toggle-status")
    public BladeResult<TagManagement> toggleStatus(@PathVariable Long id, @RequestParam boolean isActive) {
        TagManagement tag = tagManagementService.getTag(id);
        if (tag == null) {
            return BladeResult.fail("Tag does not exist");
        }
        tag.setIsActive(isActive ? Integer.valueOf(1) : Integer.valueOf(0));
        return updateTag(id, tag);
    }

    /** Export actual filtered tag rows. */
    @GetMapping("/export-vlsTagManagement")
    public void exportVlsTagManagement(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String categoryType,
                                       @RequestParam(required = false) Integer level,
                                       @RequestParam(required = false) Long parentId,
                                       @RequestParam(required = false) Long tagId,
                                       HttpServletResponse response) {
        BladePage<TagManagement> page = tagManagementService.getTagManagementPage(1L, Integer.MAX_VALUE,
            keyword, categoryType, level, parentId, tagId);
        ExcelUtil.exportExcel(page.getRecords(), "VLS Tags", TagManagement.class, response);
    }
}
