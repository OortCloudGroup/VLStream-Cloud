package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.*;
import org.springblade.vlstream.excel.VlsAnnotationImageExcel;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.vo.AnnotationImageVO;

import java.util.List;

/**
 * Annotation Image Info Table Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationImageMapper extends BaseMapper<AnnotationImage> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnnotationImage query parameters
	 * @return List<VlsAnnotationImageVO>
	 */
	List<AnnotationImageVO> selectVlsAnnotationImagePage(IPage page, AnnotationImageVO vlsAnnotationImage);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnnotationImageExcel>
	 */
	List<VlsAnnotationImageExcel> exportVlsAnnotationImage(@Param("ew") Wrapper<AnnotationImage> queryWrapper);

	/**
	 * Result mapping: mapping from database fields to Java properties
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
		@Result(property = "createTime", column = "create_time"),
		@Result(property = "updateTime", column = "update_time"),
		@Result(property = "isDeleted", column = "is_deleted")
	})
	@Select("SELECT * FROM vls_annotation_image WHERE id = #{id}")
	AnnotationImage selectById(Long id);

	/**
	 * Insert image record
	 */
	@Insert("INSERT INTO vls_annotation_image (annotation_id, image_name, original_name, local_path, " +
		"file_size, is_imported, import_time, create_time, update_time) " +
		"VALUES (#{annotationId}, #{imageName}, #{originalName}, #{localPath}, " +
		"#{fileSize}, 1, NOW(), NOW(), NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(AnnotationImage image);

	/**
	 * Query image list by dataset ID (compatible with old interface, actually using annotation_id)
	 */
	@ResultMap("AnnotationImageResultMap")
	@Select("SELECT * FROM vls_annotation_image WHERE annotation_id = #{annotationId} ORDER BY create_time DESC")
	List<AnnotationImage> selectByDatasetId(Long annotationId);

	/**
	 * Query image list by annotation project ID
	 */
	@ResultMap("AnnotationImageResultMap")
	@Select("SELECT * FROM vls_annotation_image WHERE annotation_id = #{annotationId} AND is_deleted = 0 ORDER BY create_time DESC")
	List<AnnotationImage> selectByAnnotationId(@Param("annotationId") Long annotationId);

	/**
	 * Update image information
	 */
	@Update("UPDATE vls_annotation_image SET " +
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
	@Delete("DELETE FROM vls_annotation_image WHERE id = #{id}")
	int deleteById(Long id);

	/**
	 * Delete all images by dataset ID (compatible with old interface, actually using annotation_id)
	 */
	@Delete("DELETE FROM vls_annotation_image WHERE annotation_id = #{datasetId}")
	int deleteByDatasetId(Long datasetId);

	/**
	 * Count dataset images (compatible with old interface, actually uses annotation_id)
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id = #{datasetId}")
	int countByDatasetId(Long datasetId);

	/**
	 * Count images by status (compatible with old interface, actually using annotation_id)
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id = #{datasetId} AND status = #{status}")
	int countByDatasetIdAndStatus(@Param("datasetId") Long datasetId, @Param("status") String status);

}
