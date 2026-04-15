package com.vlstream.mapper;

import com.vlstream.entity.AnnotationImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 标注图片数据访问层
 */
@Mapper
public interface AnnotationImageMapper {

    /**
     * 结果映射：数据库字段到Java属性的映射
     */
    @Results(id = "AnnotationImageResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "annotationId", column = "annotation_id"),
        @Result(property = "imageName", column = "image_name"),
        @Result(property = "originalName", column = "original_name"),
        @Result(property = "localPath", column = "local_path"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "lastModified", column = "last_modified"),
        @Result(property = "isImported", column = "is_imported"),
        @Result(property = "importTime", column = "import_time"),
        @Result(property = "createTime", column = "created_time"),
        @Result(property = "updateTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted")
    })
    @Select("SELECT * FROM annotation_image WHERE id = #{id}")
    AnnotationImage selectById(Long id);

    /**
     * 插入图片记录
     */
    @Insert("INSERT INTO annotation_image (annotation_id, image_name, original_name, local_path, " +
            "file_size, is_imported, import_time, created_time, updated_time) " +
            "VALUES (#{annotationId}, #{imageName}, #{originalName}, #{localPath}, " +
            "#{fileSize}, 1, NOW(), NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnnotationImage image);
    
    /**
     * 根据数据集ID查询图片列表 (兼容旧接口，实际使用annotation_id)
     */
    @ResultMap("AnnotationImageResultMap")
    @Select("SELECT * FROM annotation_image WHERE annotation_id = #{annotationId} ORDER BY created_time DESC")
    List<AnnotationImage> selectByDatasetId(Long annotationId);

    /**
     * 根据标注项目ID查询图片列表
     */
    @ResultMap("AnnotationImageResultMap")
    @Select("SELECT * FROM annotation_image WHERE annotation_id = #{annotationId} AND deleted = 0 ORDER BY created_time DESC")
    List<AnnotationImage> selectByAnnotationId(@Param("annotationId") Long annotationId);
    
    /**
     * 更新图片信息
     */
    @Update("UPDATE annotation_image SET " +
            "image_name = #{imageName}, original_name = #{originalName}, local_path = #{localPath}, " +
            "file_url = #{fileUrl}, file_size = #{fileSize}, mime_type = #{mimeType}, " +
            "width = #{width}, height = #{height}, category = #{category}, " +
            "annotation_data = #{annotationData}, status = #{status}, tags = #{tags}, " +
            "update_time = #{updateTime}, update_by = #{updateBy} " +
            "WHERE id = #{id}")
    int updateById(AnnotationImage image);
    
    /**
     * 删除图片记录
     */
    @Delete("DELETE FROM annotation_image WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 根据数据集ID删除所有图片 (兼容旧接口，实际使用annotation_id)
     */
    @Delete("DELETE FROM annotation_image WHERE annotation_id = #{datasetId}")
    int deleteByDatasetId(Long datasetId);

    /**
     * 统计数据集图片数量 (兼容旧接口，实际使用annotation_id)
     */
    @Select("SELECT COUNT(*) FROM annotation_image WHERE annotation_id = #{datasetId}")
    int countByDatasetId(Long datasetId);

    /**
     * 根据状态统计图片数量 (兼容旧接口，实际使用annotation_id)
     */
    @Select("SELECT COUNT(*) FROM annotation_image WHERE annotation_id = #{datasetId} AND status = #{status}")
    int countByDatasetIdAndStatus(@Param("datasetId") Long datasetId, @Param("status") String status);
}
