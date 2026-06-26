package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.vlstream.excel.VlsAnnotationLabelExcel;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;
import org.springblade.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.List;

/**
 * Annotation Label Entity Class Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationLabelMapper extends BaseMapper<AnnotationLabel> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnnotationLabel query parameters
	 * @return List<VlsAnnotationLabelVO>
	 */
	List<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage page, AnnotationLabelVO vlsAnnotationLabel);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnnotationLabelExcel>
	 */
	List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(@Param("ew") Wrapper<AnnotationLabel> queryWrapper);

	/**
	 * Query tag list by annotation project ID (including usage count statistics)
	 *
	 * @param annotationId annotation project ID
	 * @return label list
	 */
	@Select("SELECT al.*, " +
		"COALESCE((SELECT COUNT(*) FROM vls_annotation_instance ai " +
		"WHERE ai.label_id = al.id AND ai.is_deleted = 0), 0) as usage_count " +
		"FROM vls_annotation_label al " +
		"WHERE al.annotation_id = #{annotationId} AND al.is_deleted = 0 " +
		"ORDER BY al.sort_order ASC, al.id ASC")
	List<AnnotationLabel> selectByAnnotationIdWithUsageCount(@Param("annotationId") Long annotationId);

	/**
	 * Update label usage count
	 *
	 * @param labelId label ID
	 * @param usageCount usage count
	 * @return number of updated rows
	 */
	@Update("UPDATE vls_annotation_label SET usage_count = #{usageCount} WHERE id = #{labelId}")
	int updateUsageCount(@Param("labelId") Long labelId, @Param("usageCount") Integer usageCount);

}
