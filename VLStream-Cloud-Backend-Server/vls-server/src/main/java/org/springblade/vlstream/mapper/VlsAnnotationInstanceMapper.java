package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.vlstream.excel.VlsAnnotationInstanceExcel;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;
import org.springblade.vlstream.pojo.vo.AnnotationInstanceVO;

import java.util.List;

/**
 * Annotation Instance Entity Class Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationInstanceMapper extends BaseMapper<AnnotationInstance> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnnotationInstance query parameters
	 * @return List<VlsAnnotationInstanceVO>
	 */
	List<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage page, AnnotationInstanceVO vlsAnnotationInstance);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnnotationInstanceExcel>
	 */
	List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(@Param("ew") Wrapper<AnnotationInstance> queryWrapper);

	/**
	 * Query annotation instance by annotation project ID and image name
	 *
	 * @param annotationId annotation project ID
	 * @param imageId image ID
	 * @return annotation instances list
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND image_id = #{imageId} AND is_deleted = 0")
	List<AnnotationInstance> selectByAnnotationIdAndImageName(@Param("annotationId") Long annotationId,
															  @Param("imageId") String imageId);

	/**
	 * Count usage times by tag ID
	 *
	 * @param labelId label ID
	 * @return usage count
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_instance " +
		"WHERE label_id = #{labelId} AND is_deleted = 0")
	Integer countByLabelId(@Param("labelId") Long labelId);

	/**
	 * Query all annotation instances by annotation project ID
	 *
	 * @param annotationId annotation project ID
	 * @return annotation instances list
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND is_deleted = 0 " +
		"ORDER BY image_id, create_time")
	List<AnnotationInstance> selectByAnnotationId(@Param("annotationId") Long annotationId);

	/**
	 * Query annotation instance by annotation project ID and tag ID
	 *
	 * @param annotationId annotation project ID
	 * @param labelId label ID
	 * @return annotation instances list
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND label_id = #{labelId} AND is_deleted = 0")
	List<AnnotationInstance> selectByAnnotationIdAndLabelId(@Param("annotationId") Long annotationId,
															@Param("labelId") Long labelId);

}
