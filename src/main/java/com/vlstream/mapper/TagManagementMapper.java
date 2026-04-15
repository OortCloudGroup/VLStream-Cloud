package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.TagManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 标签管理 Mapper 接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface TagManagementMapper extends BaseMapper<TagManagement> {

    /**
     * 获取标签树形结构（按类型和层级排序）
     *
     * @return 标签列表
     */
    @Select("SELECT t.*, p.tag_name as parent_name " +
            "FROM tag_management t " +
            "LEFT JOIN tag_management p ON t.parent_id = p.id " +
            "WHERE t.deleted = 0 " +
            "ORDER BY t.tag_type, t.level, t.sort_order, t.id")
    List<TagManagement> selectTagTree();

    /**
     * 根据类型获取标签树
     *
     * @param tagType 标签类型
     * @return 标签列表
     */
    @Select("SELECT t.*, p.tag_name as parent_name " +
            "FROM tag_management t " +
            "LEFT JOIN tag_management p ON t.parent_id = p.id " +
            "WHERE t.deleted = 0 AND t.tag_type = #{tagType} " +
            "ORDER BY t.level, t.sort_order, t.id")
    List<TagManagement> selectTagTreeByType(@Param("tagType") String tagType);

    /**
     * 根据父级ID获取子标签
     *
     * @param parentId 父级ID
     * @return 子标签列表
     */
    @Select("SELECT * FROM tag_management " +
            "WHERE deleted = 0 AND parent_id = #{parentId} " +
            "ORDER BY sort_order, id")
    List<TagManagement> selectChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 获取根级标签（类型级别）
     *
     * @return 根级标签列表
     */
    @Select("SELECT * FROM tag_management " +
            "WHERE deleted = 0 AND level = 0 " +
            "ORDER BY sort_order, id")
    List<TagManagement> selectRootTags();

    /**
     * 更新标签使用次数
     *
     * @param tagId 标签ID
     * @param increment 增加的次数
     */
    @Update("UPDATE tag_management SET usage_count = usage_count + #{increment} " +
            "WHERE id = #{tagId}")
    void updateUsageCount(@Param("tagId") Long tagId, @Param("increment") Integer increment);

    /**
     * 设置标签使用次数
     *
     * @param tagId 标签ID
     * @param count 使用次数
     */
    @Update("UPDATE tag_management SET usage_count = #{count} WHERE id = #{tagId}")
    void setUsageCount(@Param("tagId") Long tagId, @Param("count") Integer count);

    /**
     * 检查标签名称是否存在（同级别下）
     *
     * @param tagName 标签名称
     * @param parentId 父级ID
     * @param excludeId 排除的ID（用于编辑时验证）
     * @return 数量
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
     * 获取最大排序号
     *
     * @param parentId 父级ID
     * @return 最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM tag_management " +
            "WHERE deleted = 0 AND (parent_id = #{parentId} OR (parent_id IS NULL AND #{parentId} IS NULL))")
    Integer getMaxSortOrder(@Param("parentId") Long parentId);
} 
 