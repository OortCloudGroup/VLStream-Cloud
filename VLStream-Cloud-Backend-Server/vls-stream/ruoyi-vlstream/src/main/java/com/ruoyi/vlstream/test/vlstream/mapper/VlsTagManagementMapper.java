/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTagManagementExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TagManagementVO;

import java.util.List;

/**
 * Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsTagManagementMapper extends BaseMapper<TagManagement> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsTagManagement Query parameter
	 * @return List<VlsTagManagementVO>
	 */
	List<TagManagementVO> selectVlsTagManagementPage(IPage page, TagManagementVO vlsTagManagement);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsTagManagementExcel>
	 */
	List<VlsTagManagementExcel> exportVlsTagManagement(@Param("ew") Wrapper<TagManagement> queryWrapper);

	/**
	 * Get ( and layer )
	 *
	 * @return
	 */
	@Select("SELECT t.*, p.tag_name as parent_name " +
		"FROM vls_tag_management t " +
		"LEFT JOIN vls_tag_management p ON t.parent_id = p.id " +
		"WHERE t.is_deleted = 0 " +
		"ORDER BY t.tag_type, t.level, t.sort_order, t.id")
	List<TagManagement> selectTagTree();

	/**
	 * Get
	 *
	 * @param tagType
	 * @return
	 */
	@Select("SELECT t.*, p.tag_name as parent_name " +
		"FROM vls_tag_management t " +
		"LEFT JOIN vls_tag_management p ON t.parent_id = p.id " +
		"WHERE t.is_deleted = 0 AND t.tag_type = #{tagType} " +
		"ORDER BY t.level, t.sort_order, t.id")
	List<TagManagement> selectTagTreeByType(@Param("tagType") String tagType);

	/**
	 * IDGet sub
	 *
	 * @param parentId ID
	 * @return sub
	 */
	@Select("SELECT * FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND parent_id = #{parentId} " +
		"ORDER BY sort_order, id")
	List<TagManagement> selectChildrenByParentId(@Param("parentId") Long parentId);

	/**
	 * Get ( )
	 *
	 * @return
	 */
	@Select("SELECT * FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND level = 0 " +
		"ORDER BY sort_order, id")
	List<TagManagement> selectRootTags();

	/**
	 * new
	 *
	 * @param tagId ID
	 * @param increment
	 */
	@Update("UPDATE vls_tag_management SET usage_count = usage_count + #{increment} " +
		"WHERE id = #{tagId}")
	void updateUsageCount(@Param("tagId") Long tagId, @Param("increment") Integer increment);

	/**
	 * Set
	 *
	 * @param tagId ID
	 * @param count
	 */
	@Update("UPDATE vls_tag_management SET usage_count = #{count} WHERE id = #{tagId}")
	void setUsageCount(@Param("tagId") Long tagId, @Param("count") Integer count);

	/**
	 * whether in ( )
	 *
	 * @param tagName
	 * @param parentId ID
	 * @param excludeId ID ( )
	 * @return
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
	 * Get
	 *
	 * @param parentId ID
	 * @return
	 */
	@Select("SELECT COALESCE(MAX(sort_order), 0) FROM vls_tag_management " +
		"WHERE is_deleted = 0 AND (parent_id = #{parentId} OR (parent_id IS NULL AND #{parentId} IS NULL))")
	Integer getMaxSortOrder(@Param("parentId") Long parentId);

}
