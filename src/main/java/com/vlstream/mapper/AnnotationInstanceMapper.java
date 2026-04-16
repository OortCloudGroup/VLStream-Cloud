package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.AnnotationInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Annotation Instance Mapper Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AnnotationInstanceMapper extends BaseMapper<AnnotationInstance> {

    /**
     * Query annotation instance by annotation project ID and image name
     * 
     * @param annotationId Annotation project ID
     * @param imageId Image ID
     * @return Annotation instance list
     */
    @Select("SELECT * FROM annotation_instance " +
            "WHERE annotation_id = #{annotationId} AND image_id = #{imageId} AND deleted = 0")
    List<AnnotationInstance> selectByAnnotationIdAndImageName(@Param("annotationId") Long annotationId, 
                                                              @Param("imageId") String imageId);

    /**
     * Count usage by label ID
     * 
     * @param labelId Label ID
     * @return Usage count
     */
    @Select("SELECT COUNT(*) FROM annotation_instance " +
            "WHERE label_id = #{labelId} AND deleted = 0")
    Integer countByLabelId(@Param("labelId") Long labelId);

    /**
     * Query all annotation instances by annotation project ID
     * 
     * @param annotationId Annotation project ID
     * @return Annotation instance list
     */
    @Select("SELECT * FROM annotation_instance " +
            "WHERE annotation_id = #{annotationId} AND deleted = 0 " +
            "ORDER BY image_id, created_time")
    List<AnnotationInstance> selectByAnnotationId(@Param("annotationId") Long annotationId);

    /**
     * Query annotation instances by annotation project ID and label ID
     * 
     * @param annotationId Annotation project ID
     * @param labelId Label ID
     * @return Annotation instance list
     */
    @Select("SELECT * FROM annotation_instance " +
            "WHERE annotation_id = #{annotationId} AND label_id = #{labelId} AND deleted = 0")
    List<AnnotationInstance> selectByAnnotationIdAndLabelId(@Param("annotationId") Long annotationId, 
                                                            @Param("labelId") Long labelId);
} 