package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.vlstream.excel.VlsTagManagementExcel;
import org.springblade.vlstream.pojo.entity.TagManagement;
import org.springblade.vlstream.pojo.vo.TagManagementVO;

import java.util.List;

/**
 * Label Management Table Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsTagManagementMapper extends BaseMapper<TagManagement> {

	/**
	 * Custom paging
	 *
	 * @param page             pagination parameters
	 * @param vlsTagManagement query parameters
	 * @return List<VlsTagManagementVO>
	 */
	List<TagManagementVO> selectVlsTagManagementPage(IPage page, TagManagementVO vlsTagManagement);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsTagManagementExcel>
	 */
	List<VlsTagManagementExcel> exportVlsTagManagement(@Param("ew") Wrapper<TagManagement> queryWrapper);

	/**
	 * Get label tree structure (sorted by type and level)
	 *
	 * @return label list
	 */
	@Select("SELECT t.*, p.tag_name as parent_name " +
		"FROM vls_tag_management t " +
		"LEFT JOIN vls_tag_management p ON t.parent_id = p.id " +
		"WHERE t.is_deleted = 0 " +
		"ORDER BY t.tag_type, t.level, t.sort_order, t.id")
	List<TagManagement> selectTagTree();

	/**
	 * Get tag tree by type
	 *
	 * @param tagType label type
	 * @return label list
	 */
	@Select("SELECT t.*, p.tag_name as parent_name " +
		"FROM vls_tag_management t " +
		"LEFT JOIN vls_tag_management p ON t.parent_id = p.id " +
		"WHERE t.is_deleted = 0 AND t.tag_type = #{tagType} " +
		"ORDER BY t.level, t.sort_order, t.id")
	List<TagManagement> selectTagTreeByType(@Param("tagType") String tagType);

	/**
	 * Get child tags by parent ID
	 *
	 * @param parentId parent ID
	 * @return sub-label list
	 */
	@Select("SELECT * FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND parent_id = #{parentId} " +
		"ORDER BY sort_order, id")
	List<TagManagement> selectChildrenByParentId(@Param("parentId") Long parentId);

	/**
	 * Get root-level label (type level)
	 *
	 * @return root level label list
	 */
	@Select("SELECT * FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND level = 0 " +
		"ORDER BY sort_order, id")
	List<TagManagement> selectRootTags();

	/**
	 * Update label usage count
	 *
	 * @param tagId     label ID
	 * @param increment increased count
	 */
	@Update("UPDATE vls_tag_management SET usage_count = usage_count + #{increment} " +
		"WHERE id = #{tagId}")
	void updateUsageCount(@Param("tagId") Long tagId, @Param("increment") Integer increment);

	/**
	 * Set tag usage count
	 *
	 * @param tagId label ID
	 * @param count usage count
	 */
	@Update("UPDATE vls_tag_management SET usage_count = #{count} WHERE id = #{tagId}")
	void setUsageCount(@Param("tagId") Long tagId, @Param("count") Integer count);

	/**
	 * Check if tag name exists (under the same level)
	 *
	 * @param tagName   label name
	 * @param parentId  parent ID
	 * @param excludeId excluded ID (used for validation during editing)
	 * @return quantity
	 */
	@Select("<script>" +
		"SELECT COUNT(*) FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND tag_name = #{tagName} " +
		"AND (parent_id = #{parentId} OR (parent_id IS NULL AND #{parentId} IS NULL)) " +
		"<if test='excludeId != null'>" +
		"AND id != #{excludeId} " +
		"</if>" +
		"</script>")
	int checkTagNameExists(@Param("tagName") String tagName,
						   @Param("parentId") Long parentId,
						   @Param("excludeId") Long excludeId);

	/**
	 * Get maximum sort number
	 *
	 * @param parentId parent ID
	 * @return maximum sort number
	 */
	@Select("SELECT COALESCE(MAX(sort_order), 0) FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND (parent_id = #{parentId} OR (parent_id IS NULL AND #{parentId} IS NULL))")
	Integer getMaxSortOrder(@Param("parentId") Long parentId);

}
