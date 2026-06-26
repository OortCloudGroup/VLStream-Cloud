package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.enums.AlgorithmAnnotationTypeEnum;
import org.springblade.vlstream.excel.VlsAnnotationInstanceExcel;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;
import org.springblade.vlstream.pojo.vo.AnnotationInstanceVO;

import java.util.List;

/**
 * Annotation Instance Entity Class Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationInstanceService extends BaseService<AnnotationInstance> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAnnotationInstance query parameters
	 * @return IPage<VlsAnnotationInstanceVO>
	 */
	IPage<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage<AnnotationInstanceVO> page, AnnotationInstanceVO vlsAnnotationInstance);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAnnotationInstanceExcel>
	 */
	List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(Wrapper<AnnotationInstance> queryWrapper);

	/**
	 * Query annotation instance by annotation project ID and image name
	 *
	 * @param annotationId annotation project ID
	 * @param imageName image name
	 * @return annotation instances list
	 */
	List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName);

	/**
	 * Save annotation instance
	 *
	 * @param annotationId annotation project ID
	 * @param labelId label ID
	 * @param imageId image ID
	 * @param annotationType annotation type
	 * @param annotationData annotation data (JSON format)
	 * @return saved annotation instances
	 */
	AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, AlgorithmAnnotationTypeEnum annotationType, String annotationData);

	/**
	 * Update annotation instance
	 *
	 * @param instanceId instance ID
	 * @param labelId label ID
	 * @param annotationType annotation type
	 * @param annotationData annotation data (JSON format)
	 * @return updated annotation instances
	 */
	AnnotationInstance updateAnnotation(Long instanceId, Long labelId,
										AlgorithmAnnotationTypeEnum annotationType, String annotationData);

	/**
	 * Delete annotation instance
	 *
	 * @param instanceId instance ID
	 * @return whether deleted successfully
	 */
	boolean deleteAnnotation(Long instanceId);

	/**
	 * Batch save annotation instances
	 *
	 * @param annotationId annotation project ID
	 * @param imageId image ID
	 * @param annotations annotation instances list
	 * @return whether saved successfully
	 */
	boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations);

	/**
	 * Query all annotation instances by annotation project ID
	 *
	 * @param annotationId annotation project ID
	 * @return annotation instances list
	 */
	List<AnnotationInstance> getByAnnotationId(Long annotationId);

	/**
	 * Count usage times by tag ID
	 *
	 * @param labelId label ID
	 * @return usage count
	 */
	Integer countByLabelId(Long labelId);

	/**
	 * Delete image and all related data
	 * Includes: annotation_image, annotation_instance, and updating the usage count of annotation_label
	 *
	 * @param annotationId annotation project ID
	 * @param imageId image ID
	 * @return deletion result
	 */
	boolean deleteImageAndRelatedData(Long annotationId, Long imageId);

}
