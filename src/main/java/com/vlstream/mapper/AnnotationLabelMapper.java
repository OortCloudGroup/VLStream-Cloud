package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.AnnotationLabel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Annotation Label Mapper Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AnnotationLabelMapper extends BaseMapper<AnnotationLabel> {

    /**
     * Query label list by annotation project ID (including usage count statistics)
     * 
     * @param annotationId Annotation project ID
     * @return Label list
     */
    @Select("SELECT al.*, " +
            "COALESCE((SELECT COUNT(*) FROM annotation_instance ai " +
            "WHERE ai.label_id = al.id AND ai.deleted = 0), 0) as usage_count " +
            "FROM annotation_label al " +
            "WHERE al.annotation_id = #{annotationId} AND al.deleted = 0 " +
            "ORDER BY al.sort_order ASC, al.id ASC")
    List<AnnotationLabel> selectByAnnotationIdWithUsageCount(@Param("annotationId") Long annotationId);

    /**
     * Update label usage count
     * 
     * @param labelId Label ID
     * @param usageCount Usage count
     * @return Updated rows
     */
    @Update("UPDATE annotation_label SET usage_count = #{usageCount} WHERE id = #{labelId}")
    int updateUsageCount(@Param("labelId") Long labelId, @Param("usageCount") Integer usageCount);

    /**
     * Batch update label sort order
     * 
     * @param annotationId Annotation project ID
     * @param labelIds Label ID list (in sort order)
     * @return Updated rows
     */
    int updateSortOrder(@Param("annotationId") Long annotationId, @Param("labelIds") List<Long> labelIds);
} 