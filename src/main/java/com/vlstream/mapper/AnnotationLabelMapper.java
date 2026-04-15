package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.AnnotationLabel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 标注标签Mapper接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AnnotationLabelMapper extends BaseMapper<AnnotationLabel> {

    /**
     * 根据标注项目ID查询标签列表（包含使用次数统计）
     * 
     * @param annotationId 标注项目ID
     * @return 标签列表
     */
    @Select("SELECT al.*, " +
            "COALESCE((SELECT COUNT(*) FROM annotation_instance ai " +
            "WHERE ai.label_id = al.id AND ai.deleted = 0), 0) as usage_count " +
            "FROM annotation_label al " +
            "WHERE al.annotation_id = #{annotationId} AND al.deleted = 0 " +
            "ORDER BY al.sort_order ASC, al.id ASC")
    List<AnnotationLabel> selectByAnnotationIdWithUsageCount(@Param("annotationId") Long annotationId);

    /**
     * 更新标签的使用次数
     * 
     * @param labelId 标签ID
     * @param usageCount 使用次数
     * @return 更新行数
     */
    @Update("UPDATE annotation_label SET usage_count = #{usageCount} WHERE id = #{labelId}")
    int updateUsageCount(@Param("labelId") Long labelId, @Param("usageCount") Integer usageCount);

    /**
     * 批量更新标签排序
     * 
     * @param annotationId 标注项目ID
     * @param labelIds 标签ID列表（按排序顺序）
     * @return 更新行数
     */
    int updateSortOrder(@Param("annotationId") Long annotationId, @Param("labelIds") List<Long> labelIds);
} 