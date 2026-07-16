package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmAnnotationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmAnnotationVO;

import java.util.List;
import java.util.Map;

/**
 * 算法标注数据表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmAnnotationMapper extends BaseMapper<AlgorithmAnnotation> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmAnnotation 查询参数
	 * @return List<VlsAlgorithmAnnotationVO>
	 */
	List<AlgorithmAnnotationVO> selectVlsAlgorithmAnnotationPage(IPage page, AlgorithmAnnotationVO vlsAlgorithmAnnotation);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmAnnotationExcel>
	 */
	List<VlsAlgorithmAnnotationExcel> exportVlsAlgorithmAnnotation(@Param("ew") Wrapper<AlgorithmAnnotation> queryWrapper);

	/**
	 * 分页查询算法标注列表
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
	 * 根据标注类型查询算法标注列表
	 */
	@Select("SELECT * FROM algorithm_annotation WHERE is_deleted = 0 AND annotation_type = #{annotationType} ORDER BY create_time DESC")
	List<AlgorithmAnnotation> selectByAnnotationType(@Param("annotationType") String annotationType);

	/**
	 * 根据标注状态查询算法标注列表
	 */
	@Select("SELECT * FROM algorithm_annotation WHERE is_deleted = 0 AND annotation_status = #{annotationStatus} ORDER BY create_time DESC")
	List<AlgorithmAnnotation> selectByAnnotationStatus(@Param("annotationStatus") String annotationStatus);

	/**
	 * 查询标注类型统计
	 */
	@Select("SELECT annotation_type, COUNT(*) as count FROM algorithm_annotation WHERE is_deleted = 0 GROUP BY annotation_type")
	List<Map<String, Object>> selectAnnotationTypeStatistics();

	/**
	 * 查询标注状态统计
	 */
	@Select("SELECT annotation_status, COUNT(*) as count FROM algorithm_annotation WHERE is_deleted = 0 GROUP BY annotation_status")
	List<Map<String, Object>> selectAnnotationStatusStatistics();

	/**
	 * 查询标注进度统计
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
	 * 获取标注工作量统计
	 */
	@Select("SELECT " +
		"SUM(total_count) as total_count, " +
		"SUM(annotated_count) as annotated_count, " +
		"ROUND(SUM(annotated_count) * 100.0 / SUM(total_count), 2) as overall_progress " +
		"FROM algorithm_annotation " +
		"WHERE is_deleted = 0 AND total_count > 0")
	Map<String, Object> selectWorkloadStatistics();

}
