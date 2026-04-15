package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.AnnotationInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标注实例Mapper接口
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface AnnotationInstanceMapper extends BaseMapper<AnnotationInstance> {

    /**
     * 根据标注项目ID和图片名称查询标注实例
     * 
     * @param annotationId 标注项目ID
     * @param imageId 图片id
     * @return 标注实例列表
     */
    @Select("SELECT * FROM annotation_instance " +
            "WHERE annotation_id = #{annotationId} AND image_id = #{imageId} AND deleted = 0")
    List<AnnotationInstance> selectByAnnotationIdAndImageName(@Param("annotationId") Long annotationId, 
                                                              @Param("imageId") String imageId);

    /**
     * 根据标签ID统计使用次数
     * 
     * @param labelId 标签ID
     * @return 使用次数
     */
    @Select("SELECT COUNT(*) FROM annotation_instance " +
            "WHERE label_id = #{labelId} AND deleted = 0")
    Integer countByLabelId(@Param("labelId") Long labelId);

    /**
     * 根据标注项目ID查询所有标注实例
     * 
     * @param annotationId 标注项目ID
     * @return 标注实例列表
     */
    @Select("SELECT * FROM annotation_instance " +
            "WHERE annotation_id = #{annotationId} AND deleted = 0 " +
            "ORDER BY image_id, created_time")
    List<AnnotationInstance> selectByAnnotationId(@Param("annotationId") Long annotationId);

    /**
     * 根据标注项目ID和标签ID查询标注实例
     * 
     * @param annotationId 标注项目ID
     * @param labelId 标签ID
     * @return 标注实例列表
     */
    @Select("SELECT * FROM annotation_instance " +
            "WHERE annotation_id = #{annotationId} AND label_id = #{labelId} AND deleted = 0")
    List<AnnotationInstance> selectByAnnotationIdAndLabelId(@Param("annotationId") Long annotationId, 
                                                            @Param("labelId") Long labelId);
} 