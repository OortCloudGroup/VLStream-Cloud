package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAlgorithmAnnotationExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.vo.AlgorithmAnnotationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Algorithm annotation data table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmAnnotationService extends BaseService<AlgorithmAnnotation> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmAnnotation query parameters
	 * @return IPage<VlsAlgorithmAnnotationVO>
	 */
	IPage<AlgorithmAnnotationVO> selectVlsAlgorithmAnnotationPage(IPage<AlgorithmAnnotationVO> page, AlgorithmAnnotationVO vlsAlgorithmAnnotation);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmAnnotationExcel>
	 */
	List<VlsAlgorithmAnnotationExcel> exportVlsAlgorithmAnnotation(Wrapper<AlgorithmAnnotation> queryWrapper);

	/**
	 * Paginated query for algorithm annotation list
	 *
	 * @param page pagination parameters
	 * @param annotationName annotation name (fuzzy query)
	 * @param annotationType annotation type
	 * @param annotationStatus annotation status
	 * @return pagination results
	 */
	IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
													String annotationName,
													String annotationType,
													String annotationStatus);

	/**
	 * Query annotation list by annotation type
	 *
	 * @param annotationType annotation type
	 * @return annotation list
	 */
	List<AlgorithmAnnotation> getByAnnotationType(String annotationType);

	/**
	 * Query annotation list by annotation status
	 *
	 * @param annotationStatus annotation status
	 * @return annotation list
	 */
	List<AlgorithmAnnotation> getByAnnotationStatus(String annotationStatus);

	/**
	 * Create algorithm annotation
	 *
	 * @param annotation annotation information
	 * @return whether successful
	 */
	boolean createAnnotation(AlgorithmAnnotation annotation);

	/**
	 * Update algorithm annotation
	 *
	 * @param annotation annotation information
	 * @return whether successful
	 */
	boolean updateAnnotation(AlgorithmAnnotation annotation);

	/**
	 * Delete algorithm annotation
	 *
	 * @param id annotation ID
	 * @return whether successful
	 */
	boolean deleteAnnotation(Long id);

	/**
	 * Batch delete algorithm annotations
	 *
	 * @param ids annotation ID list
	 * @return whether successful
	 */
	boolean batchDeleteAnnotations(List<Long> ids);

	/**
	 * Update annotation progress
	 *
	 * @param id annotation ID
	 * @param annotatedCount annotated quantity
	 * @return whether successful
	 */
	boolean updateAnnotationProgress(Long id, Integer annotatedCount);

	/**
	 * Batch update annotation status
	 *
	 * @param ids annotation ID list
	 * @param annotationStatus new annotation status
	 * @return whether successful
	 */
	boolean batchUpdateAnnotationStatus(List<Long> ids, String annotationStatus);

	/**
	 * Start annotation task
	 *
	 * @param id annotation ID
	 * @return whether successful
	 */
	boolean startAnnotationTask(Long id);

	/**
	 * Complete annotation task
	 *
	 * @param id annotation ID
	 * @return whether successful
	 */
	boolean completeAnnotationTask(Long id);

	/**
	 * Reset labeling task
	 *
	 * @param id annotation ID
	 * @return whether successful
	 */
	boolean resetAnnotationTask(Long id);

	/**
	 * Import annotation data
	 *
	 * @param id annotation ID
	 * @param dataPath data path
	 * @return import result
	 */
	Map<String, Object> importAnnotationData(Long id, String dataPath);

	/**
	 * Import annotation dataset from zip file.
	 *
	 * @param annotationId annotation id
	 * @param zipFile zip dataset file
	 * @return import result
	 */
	Map<String, Object> importAnnotationDatasetZip(Long annotationId, MultipartFile zipFile);

	/**
	 * Get annotation type statistics
	 *
	 * @return annotation type statistics
	 */
	List<Map<String, Object>> getAnnotationTypeStatistics();

	/**
	 * Get annotation status statistics
	 *
	 * @return annotation status statistics
	 */
	List<Map<String, Object>> getAnnotationStatusStatistics();

	/**
	 * Get annotation progress statistics
	 *
	 * @return annotation progress statistics
	 */
	List<Map<String, Object>> getProgressStatistics();

	/**
	 * Get annotation workload statistics
	 *
	 * @return annotation workload statistics
	 */
	Map<String, Object> getWorkloadStatistics();

	/**
	 * Verify labeling data
	 *
	 * @param id annotation ID
	 * @return Verification results
	 */
	Map<String, Object> validateAnnotationData(Long id);

	/**
	 * Save annotation data to dataset file
	 *
	 * @param annotationId annotation ID
	 * @return whether saved successfully
	 */
	boolean saveAnnotationToDataset(Long annotationId);

	/**
	 * Download the annotation dataset as a zip package.
	 *
	 * @param id       annotation id
	 * @param response http response
	 */
	void downloadAnnotationDataset(Long id, HttpServletResponse response);

}
