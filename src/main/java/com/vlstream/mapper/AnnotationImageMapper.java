package com.vlstream.mapper;

import com.vlstream.entity.AnnotationImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Annotation Image Data Access Layer
 */
@Mapper
public interface AnnotationImageMapper {

    /**
     * Result mapping: Database field to Java property mapping
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
     * Insert image record
     */
    @Insert("INSERT INTO annotation_image (annotation_id, image_name, original_name, local_path, " +
            "file_size, is_imported, import_time, created_time, updated_time) " +
            "VALUES (#{annotationId}, #{imageName}, #{originalName}, #{localPath}, " +
            "#{fileSize}, 1, NOW(), NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnnotationImage image);
    
    /**
     * Query image list by dataset ID (compatible with old interface, actually using annotation_id)
     */
    @ResultMap("AnnotationImageResultMap")
    @Select("SELECT * FROM annotation_image WHERE annotation_id = #{annotationId} ORDER BY created_time DESC")
    List<AnnotationImage> selectByDatasetId(Long annotationId);

    /**
     * Query image list by annotation project ID
     */
    @ResultMap("AnnotationImageResultMap")
    @Select("SELECT * FROM annotation_image WHERE annotation_id = #{annotationId} AND deleted = 0 ORDER BY created_time DESC")
    List<AnnotationImage> selectByAnnotationId(@Param("annotationId") Long annotationId);
    
    /**
     * Update image information
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
     * Delete image record
     */
    @Delete("DELETE FROM annotation_image WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * Delete all images by dataset ID (compatible with old interface, actually using annotation_id)
     */
    @Delete("DELETE FROM annotation_image WHERE annotation_id = #{datasetId}")
    int deleteByDatasetId(Long datasetId);

    /**
     * Count images in dataset (compatible with old interface, actually using annotation_id)
     */
    @Select("SELECT COUNT(*) FROM annotation_image WHERE annotation_id = #{datasetId}")
    int countByDatasetId(Long datasetId);

    /**
     * Count images by status in dataset (compatible with old interface, actually using annotation_id)
     */
    @Select("SELECT COUNT(*) FROM annotation_image WHERE annotation_id = #{datasetId} AND status = #{status}")
    int countByDatasetIdAndStatus(@Param("datasetId") Long datasetId, @Param("status") String status);
}
