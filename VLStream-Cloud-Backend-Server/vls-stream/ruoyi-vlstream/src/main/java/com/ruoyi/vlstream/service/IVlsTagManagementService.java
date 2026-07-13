/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.TagManagement;

import java.util.List;
import java.util.Map;

/**
 * VLS tag management service contract used by compatibility controllers.
 */
public interface IVlsTagManagementService {

    BladePage<TagManagement> getTagManagementPage(long current, long size, String keyword, String categoryType,
                                                  Integer level, Long parentId, Long tagId);

    List<Map<String, Object>> getTagTree();

    TagManagement getTag(Long id);

    TagManagement createTag(TagManagement tagManagement);

    TagManagement updateTag(Long id, TagManagement tagManagement);

    boolean deleteTag(Long id);

    boolean deleteTags(List<Long> ids);

    Map<String, Object> getStatistics();

    TagManagement getTagUsageStats(Long id);

    boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId);

    List<TagManagement> getChildren(Long parentId);

    boolean moveTag(Long id, Long newParentId, Integer targetPosition);

    void updateUsageCount(Long tagId, Integer increment);
}
