package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.TagManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Mapper for VLS tag management.
 */
@Mapper
public interface VlsTagManagementMapper extends BaseMapperPlus<VlsTagManagementMapper, TagManagement, TagManagement> {

    @Select("<script>" +
        "SELECT * FROM vls_tag_management WHERE is_deleted = 0 " +
        "<choose>" +
        "<when test='parentId != null'>AND parent_id = #{parentId}</when>" +
        "<otherwise>AND parent_id IS NULL</otherwise>" +
        "</choose>" +
        " ORDER BY sort_order, id" +
        "</script>")
    List<TagManagement> selectChildrenByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM vls_tag_management WHERE is_deleted = 0 AND category_type = #{categoryType} " +
        "ORDER BY level, sort_order, id")
    List<TagManagement> selectByCategory(@Param("categoryType") String categoryType);

    @Select("<script>" +
        "SELECT COUNT(*) FROM vls_tag_management WHERE is_deleted = 0 AND tag_name = #{tagName} " +
        "<choose>" +
        "<when test='parentId != null'>AND parent_id = #{parentId}</when>" +
        "<otherwise>AND parent_id IS NULL</otherwise>" +
        "</choose>" +
        "<if test='excludeId != null'> AND id != #{excludeId}</if>" +
        "</script>")
    int checkTagNameExists(@Param("tagName") String tagName,
                           @Param("parentId") Long parentId,
                           @Param("excludeId") Long excludeId);

    @Select("<script>" +
        "SELECT COALESCE(MAX(sort_order), 0) FROM vls_tag_management WHERE is_deleted = 0 " +
        "<choose>" +
        "<when test='parentId != null'>AND parent_id = #{parentId}</when>" +
        "<otherwise>AND parent_id IS NULL</otherwise>" +
        "</choose>" +
        "</script>")
    Integer getMaxSortOrder(@Param("parentId") Long parentId);

    @Update("UPDATE vls_tag_management SET usage_count = COALESCE(usage_count, 0) + #{increment} WHERE id = #{tagId}")
    int updateUsageCount(@Param("tagId") Long tagId, @Param("increment") Integer increment);
}
