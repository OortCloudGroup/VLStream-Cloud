package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.AnnotationInstance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for VLS annotation instances.
 */
public interface VlsAnnotationInstanceMapper extends BaseMapperPlus<VlsAnnotationInstanceMapper, AnnotationInstance, AnnotationInstance> {

    /**
     * Query instances for one image, accepting either image id or image name.
     */
    @Select({
        "<script>",
        "SELECT ai.*, img.image_name AS image_name, img.original_name AS original_name, label.name AS label_name",
        "FROM vls_annotation_instance ai",
        "LEFT JOIN vls_annotation_image img ON img.id = ai.image_id AND img.is_deleted = 0",
        "LEFT JOIN vls_annotation_label label ON label.id = ai.label_id AND label.is_deleted = 0",
        "WHERE ai.annotation_id = #{annotationId} AND ai.is_deleted = 0",
        "AND (CAST(ai.image_id AS CHAR) = #{imageToken}",
        "  OR img.image_name = #{imageToken}",
        "  OR img.original_name = #{imageToken})",
        "ORDER BY ai.create_time ASC, ai.id ASC",
        "</script>"
    })
    List<AnnotationInstance> selectByAnnotationIdAndImageToken(@Param("annotationId") Long annotationId,
                                                               @Param("imageToken") String imageToken);

    /**
     * Query all active instances under an annotation task.
     */
    @Select("SELECT ai.*, img.image_name AS image_name, img.original_name AS original_name, label.name AS label_name " +
        "FROM vls_annotation_instance ai " +
        "LEFT JOIN vls_annotation_image img ON img.id = ai.image_id AND img.is_deleted = 0 " +
        "LEFT JOIN vls_annotation_label label ON label.id = ai.label_id AND label.is_deleted = 0 " +
        "WHERE ai.annotation_id = #{annotationId} AND ai.is_deleted = 0 " +
        "ORDER BY ai.image_id ASC, ai.create_time ASC, ai.id ASC")
    List<AnnotationInstance> selectByAnnotationId(@Param("annotationId") Long annotationId);

    /**
     * Count active instances for a label.
     */
    @Select("SELECT COUNT(*) FROM vls_annotation_instance WHERE label_id = #{labelId} AND is_deleted = 0")
    Integer countByLabelId(@Param("labelId") Long labelId);

    /**
     * Count distinct annotated images under an annotation task.
     */
    @Select("SELECT COUNT(DISTINCT image_id) FROM vls_annotation_instance WHERE annotation_id = #{annotationId} AND is_deleted = 0")
    Integer countAnnotatedImages(@Param("annotationId") Long annotationId);
}
