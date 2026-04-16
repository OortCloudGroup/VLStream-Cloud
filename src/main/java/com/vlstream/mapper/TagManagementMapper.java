package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.TagManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Tag Management Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface TagManagementMapper extends BaseMapper<TagManagement> {

    /**
     * Get tag tree structure (sorted by type and level)
     *
     * @return Tag list
     */
    @Select("SELECT t.*, p.tag_name as parent_name " +
            "FROM tag_management t " +
            "LEFT JOIN tag_management p ON t.parent_id = p.id " +
            "WHERE t.deleted = 0 " +
            "ORDER BY t.tag_type, t.level, t.sort_order, t.id")
    List<TagManagement> selectTagTree();

    /**
     * Get tag tree by type
     *
     * @param tagType Tag type
     * @return Tag list
     */
    @Select("SELECT t.*, p.tag_name as parent_name " +
            "FROM tag_management t " +
            "LEFT JOIN tag_management p ON t.parent_id = p.id " +
            "WHERE t.deleted = 0 AND t.tag_type = #{tagType} " +
            "ORDER BY t.level, t.sort_order, t.id")
    List<TagManagement> selectTagTreeByType(@Param("tagType") String tagType);

    /**
     * Get child tags by parent ID
     *
     * @param parentId Parent ID
     * @return Child tag list
     */
    @Select("SELECT * FROM tag_management " +
            "WHERE deleted = 0 AND parent_id = #{parentId} " +
            "ORDER BY sort_order, id")
    List<TagManagement> selectChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * Get root tags (type level)
     *
     * @return Root tag list
     */
    @Select("SELECT * FROM tag_management " +
            "WHERE deleted = 0 AND level = 0 " +
            "ORDER BY sort_order, id")
    List<TagManagement> selectRootTags();

    /**
     * Update tag usage count
     *
     * @param tagId Tag ID
     * @param increment Increment count
     */
    @Update("UPDATE tag_management SET usage_count = usage_count + #{increment} " +
            "WHERE id = #{tagId}")
    void updateUsageCount(@Param("tagId") Long tagId, @Param("increment") Integer increment);

    /**
     * Set tag usage count
     *
     * @param tagId Tag ID
     * @param count Usage count
     */
    @Update("UPDATE tag_management SET usage_count = #{count} WHERE id = #{tagId}")
    void setUsageCount(@Param("tagId") Long tagId, @Param("count") Integer count);

    /**
     * Check if tag name exists (under the same level)
     *
     * @param tagName Tag name
     * @param parentId Parent ID
     * @param excludeId Exclude ID (used for editing validation)
     * @return Count
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM tag_management " +
            "WHERE deleted = 0 AND tag_name = #{tagName} " +
            "AND (parent_id = #{parentId} OR (parent_id IS NULL AND #{parentId} IS NULL)) " +
            "<if test='excludeId != null'>" +
            "AND id != #{excludeId} " +
            "</if>" +
            "</script>")
    int checkTagNameExists(@Param("tagName") String tagName, 
                          @Param("parentId") Long parentId, 
                          @Param("excludeId") Long excludeId);

    /**
     * Get maximum sort order
     *
     * @param parentId Parent ID
     * @return Maximum sort order
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM tag_management " +
            "WHERE deleted = 0 AND (parent_id = #{parentId} OR (parent_id IS NULL AND #{parentId} IS NULL))")
    Integer getMaxSortOrder(@Param("parentId") Long parentId);
} 
 