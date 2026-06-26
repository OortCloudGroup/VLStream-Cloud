package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsTagManagementExcel;
import org.springblade.vlstream.pojo.dto.TagManagementDTO;
import org.springblade.vlstream.pojo.entity.TagManagement;
import org.springblade.vlstream.pojo.vo.TagManagementVO;

import java.util.List;

/**
 * Label Management Table Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsTagManagementService extends BaseService<TagManagement> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsTagManagement query parameters
	 * @return IPage<VlsTagManagementVO>
	 */
	IPage<TagManagementVO> selectVlsTagManagementPage(IPage<TagManagementVO> page, TagManagementVO vlsTagManagement);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsTagManagementExcel>
	 */
	List<VlsTagManagementExcel> exportVlsTagManagement(Wrapper<TagManagement> queryWrapper);

	/**
	 * Get label tree structure
	 *
	 * @return label tree
	 */
	List<TagManagementDTO> getTagTree();

	/**
	 * Get tag tree by type
	 *
	 * @param tagType label type (own-owned, public-public)
	 * @return label tree
	 */
	List<TagManagementDTO> getTagTreeByType(String tagType);

	/**
	 * Create tag
	 *
	 * @param tagManagement label information
	 * @return successfully created label
	 */
	TagManagement createTag(TagManagement tagManagement);

	/**
	 * Update label
	 *
	 * @param tagManagement label information
	 * @return successfully updated label
	 */
	TagManagement updateTag(TagManagement tagManagement);

	/**
	 * Delete tag (cascade delete sub-tags)
	 *
	 * @param tagId label ID
	 * @return whether deleted successfully
	 */
	boolean deleteTag(Long tagId);

	/**
	 * Batch delete labels
	 *
	 * @param tagIds label ID list
	 * @return whether deleted successfully
	 */
	boolean deleteTags(List<Long> tagIds);

	/**
	 * Move tag (adjust parent or order)
	 *
	 * @param tagId label ID
	 * @param targetParentId target parent ID
	 * @param targetPosition target position
	 * @return whether moved successfully
	 */
	boolean moveTag(Long tagId, Long targetParentId, Integer targetPosition);

	/**
	 * Update label usage count
	 *
	 * @param tagId label ID
	 * @param increment increased count
	 */
	void updateUsageCount(Long tagId, Integer increment);

	/**
	 * Check if tag name is duplicated (under the same level)
	 *
	 * @param tagName label name
	 * @param parentId parent ID
	 * @param excludeId excluded ID (used for validation during editing)
	 * @return whether duplicated
	 */
	boolean isTagNameDuplicate(String tagName, Long parentId, Long excludeId);

	/**
	 * Enable/disable tag
	 *
	 * @param tagId label ID
	 * @param isActive whether enabled
	 * @return whether operated successfully
	 */
	boolean toggleTagStatus(Long tagId, boolean isActive);

	/**
	 * Get label usage statistics
	 *
	 * @param tagId label ID
	 * @return usage statistics
	 */
	TagManagement getTagUsageStats(Long tagId);

	/**
	 * Get labels by category type
	 *
	 * @param categoryType category type (own-owned, public-public)
	 * @return label list
	 */
	List<TagManagement> getTagsByCategory(String categoryType);

	/**
	 * Paginated query for tag management
	 *
	 * @param page pagination parameters
	 * @param keyword search keyword
	 * @param categoryType label major category
	 * @param level label level
	 * @param parentId parent ID
	 * @param tagId label ID
	 * @return pagination results
	 */
	IPage<TagManagement> getTagManagementPage(Page<TagManagement> page, String keyword, String categoryType, Integer level, Long parentId, Long tagId);

}
