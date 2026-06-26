package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAnnotationLabelExcel;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;
import org.springblade.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.List;

/**
 * Annotation Label Entity Class Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationLabelService extends BaseService<AnnotationLabel> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnnotationLabel query parameters
	 * @return IPage<VlsAnnotationLabelVO>
	 */
	IPage<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage<AnnotationLabelVO> page, AnnotationLabelVO vlsAnnotationLabel);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnnotationLabelExcel>
	 */
	List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(Wrapper<AnnotationLabel> queryWrapper);

	/**
	 * Query tag list by annotation project ID (including usage count statistics)
	 *
	 * @param annotationId annotation project ID
	 * @return label list
	 */
	List<AnnotationLabel> getByAnnotationIdWithUsageCount(Long annotationId);

	/**
	 * Create tag
	 *
	 * @param annotationId annotation project ID
	 * @param name label name
	 * @param color label color
	 * @param description label description
	 * @return created label
	 */
	AnnotationLabel createLabel(Long annotationId, String name, String color, String description);

	/**
	 * Update label
	 *
	 * @param labelId label ID
	 * @param name label name
	 * @param color label color
	 * @param description label description
	 * @return updated label
	 */
	AnnotationLabel updateLabel(Long labelId, String name, String color, String description);

	/**
	 * Delete tag
	 *
	 * @param labelId label ID
	 * @return whether deleted successfully
	 */
	boolean deleteLabel(Long labelId);

	/**
	 * Update label usage count
	 *
	 * @param labelId label ID
	 * @return whether updated successfully
	 */
	boolean updateUsageCount(Long labelId);

	/**
	 * Batch update label sorting
	 *
	 * @param annotationId annotation project ID
	 * @param labelIds label ID list (in sort order)
	 * @return whether updated successfully
	 */
	boolean updateSortOrder(Long annotationId, List<Long> labelIds);

	/**
	 * Search labels by name
	 *
	 * @param annotationId annotation project ID
	 * @param keyword search keyword
	 * @return label list
	 */
	List<AnnotationLabel> searchLabels(Long annotationId, String keyword);

}
