/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTagManagementExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.TagManagementDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TagManagementVO;

import java.util.List;

/**
 * service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsTagManagementService extends BaseService<TagManagement> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsTagManagement Query parameter
	 * @return IPage<VlsTagManagementVO>
	 */
	IPage<TagManagementVO> selectVlsTagManagementPage(IPage<TagManagementVO> page, TagManagementVO vlsTagManagement);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsTagManagementExcel>
	 */
	List<VlsTagManagementExcel> exportVlsTagManagement(Wrapper<TagManagement> queryWrapper);

	/**
	 * Get
	 *
	 * @return
	 */
	List<TagManagementDTO> getTagTree();

	/**
	 * Get
	 *
	 * @param tagType (own- , public- )
	 * @return
	 */
	List<TagManagementDTO> getTagTreeByType(String tagType);

	/**
	 *
	 *
	 * @param tagManagement info
	 * @return successfully
	 */
	TagManagement createTag(TagManagement tagManagement);

	/**
	 * new
	 *
	 * @param tagManagement info
	 * @return new successfully
	 */
	TagManagement updateTag(TagManagement tagManagement);

	/**
	 * Delete ( Delete sub )
	 *
	 * @param tagId ID
	 * @return whether Delete successfully
	 */
	boolean deleteTag(Long tagId);

	/**
	 * Batch delete
	 *
	 * @param tagIds ID
	 * @return whether Delete successfully
	 */
	boolean deleteTags(List<Long> tagIds);

	/**
	 * ( )
	 *
	 * @param tagId ID
	 * @param targetParentId ID
	 * @param targetPosition
	 * @return whether successfully
	 */
	boolean moveTag(Long tagId, Long targetParentId, Integer targetPosition);

	/**
	 * new
	 *
	 * @param tagId ID
	 * @param increment
	 */
	void updateUsageCount(Long tagId, Integer increment);

	/**
	 * whether ( )
	 *
	 * @param tagName
	 * @param parentId ID
	 * @param excludeId ID ( )
	 * @return whether
	 */
	boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId);

	/**
	 * /
	 *
	 * @param tagId ID
	 * @param isActive whether
	 * @return whether operationsuccessfully
	 */
	boolean toggleTagStatus(Long tagId, boolean isActive);

	/**
	 * Get
	 *
	 * @param tagId ID
	 * @return info
	 */
	TagManagement getTagUsageStats(Long tagId);

	/**
	 * Get
	 *
	 * @param categoryType (own- , public- )
	 * @return
	 */
	List<TagManagement> getTagsByCategory(String categoryType);

	/**
	 * Query
	 *
	 * @param page parameter
	 * @param keyword
	 * @param categoryType
	 * @param level layer
	 * @param parentId ID
	 * @param tagId ID
	 * @return
	 */
	IPage<TagManagement> getTagManagementPage(Page<TagManagement> page, String keyword, String categoryType, Integer level, Long parentId, Long tagId);

}
