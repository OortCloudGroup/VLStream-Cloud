package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.vlstream.excel.VlsAlgorithmAnnotationExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.vo.AlgorithmAnnotationVO;

import java.util.List;
import java.util.Map;

/**
 * Algorithm annotation data table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmAnnotationMapper extends BaseMapper<AlgorithmAnnotation> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmAnnotation query parameters
	 * @return List<VlsAlgorithmAnnotationVO>
	 */
	List<AlgorithmAnnotationVO> selectVlsAlgorithmAnnotationPage(IPage page, AlgorithmAnnotationVO vlsAlgorithmAnnotation);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmAnnotationExcel>
	 */
	List<VlsAlgorithmAnnotationExcel> exportVlsAlgorithmAnnotation(@Param("ew") Wrapper<AlgorithmAnnotation> queryWrapper);

	/**
	 * Paginated query for algorithm annotation list
	 */
	@Select("SELECT * FROM algorithm_annotation " +
		"WHERE is_deleted = 0 " +
		"AND (#{annotationName} IS NULL OR annotation_name LIKE CONCAT('%', #{annotationName}, '%')) " +
		"AND (#{annotationType} IS NULL OR annotation_type = #{annotationType}) " +
		"AND (#{annotationStatus} IS NULL OR annotation_status = #{annotationStatus}) " +
		"ORDER BY create_time DESC")
	IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
													@Param("annotationName") String annotationName,
													@Param("annotationType") String annotationType,
													@Param("annotationStatus") String annotationStatus);

	/**
	 * Query algorithm annotation list by annotation type
	 */
	@Select("SELECT * FROM algorithm_annotation WHERE is_deleted = 0 AND annotation_type = #{annotationType} ORDER BY create_time DESC")
	List<AlgorithmAnnotation> selectByAnnotationType(@Param("annotationType") String annotationType);

	/**
	 * Query algorithm annotation list by annotation status
	 */
	@Select("SELECT * FROM algorithm_annotation WHERE is_deleted = 0 AND annotation_status = #{annotationStatus} ORDER BY create_time DESC")
	List<AlgorithmAnnotation> selectByAnnotationStatus(@Param("annotationStatus") String annotationStatus);

	/**
	 * Query Annotation Type Statistics
	 */
	@Select("SELECT annotation_type, COUNT(*) as count FROM algorithm_annotation WHERE is_deleted = 0 GROUP BY annotation_type")
	List<Map<String, Object>> selectAnnotationTypeStatistics();

	/**
	 * Query Annotation Status Statistics
	 */
	@Select("SELECT annotation_status, COUNT(*) as count FROM algorithm_annotation WHERE is_deleted = 0 GROUP BY annotation_status")
	List<Map<String, Object>> selectAnnotationStatusStatistics();

	/**
	 * Query Annotation Progress Statistics
	 */
	@Select("SELECT " +
		"CASE " +
		"  WHEN progress >= 0 AND progress < 25 THEN '0-25%' " +
		"  WHEN progress >= 25 AND progress < 50 THEN '25-50%' " +
		"  WHEN progress >= 50 AND progress < 75 THEN '50-75%' " +
		"  WHEN progress >= 75 AND progress < 100 THEN '75-100%' " +
		"  ELSE '100%' " +
		"END as progress_range, " +
		"COUNT(*) as count " +
		"FROM algorithm_annotation " +
		"WHERE is_deleted = 0 " +
		"GROUP BY progress_range")
	List<Map<String, Object>> selectProgressStatistics();

	/**
	 * Get annotation workload statistics
	 */
	@Select("SELECT " +
		"SUM(total_count) as total_count, " +
		"SUM(annotated_count) as annotated_count, " +
		"ROUND(SUM(annotated_count) * 100.0 / SUM(total_count), 2) as overall_progress " +
		"FROM algorithm_annotation " +
		"WHERE is_deleted = 0 AND total_count > 0")
	Map<String, Object> selectWorkloadStatistics();

}
