/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import javax.servlet.http.HttpServletResponse;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmAnnotationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmAnnotationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * algorithmannotationdata service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmAnnotationService extends BaseService<AlgorithmAnnotation> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAlgorithmAnnotation Query parameter
	 * @return IPage<VlsAlgorithmAnnotationVO>
	 */
	IPage<AlgorithmAnnotationVO> selectVlsAlgorithmAnnotationPage(IPage<AlgorithmAnnotationVO> page, AlgorithmAnnotationVO vlsAlgorithmAnnotation);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAlgorithmAnnotationExcel>
	 */
	List<VlsAlgorithmAnnotationExcel> exportVlsAlgorithmAnnotation(Wrapper<AlgorithmAnnotation> queryWrapper);

	/**
	 * Query algorithmannotation list
	 *
	 * @param page parameter
	 * @param annotationName annotation ( Query )
	 * @param annotationType annotation
	 * @param annotationStatus annotation
	 * @return
	 */
	IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
													String annotationName,
													String annotationType,
													String annotationStatus);

	/**
	 * annotation Query annotation list
	 *
	 * @param annotationType annotation
	 * @return annotation
	 */
	List<AlgorithmAnnotation> getByAnnotationType(String annotationType);

	/**
	 * annotation Query annotation list
	 *
	 * @param annotationStatus annotation
	 * @return annotation
	 */
	List<AlgorithmAnnotation> getByAnnotationStatus(String annotationStatus);

	/**
	 * algorithmannotation
	 *
	 * @param annotation annotationinfo
	 * @return whether successfully
	 */
	boolean createAnnotation(AlgorithmAnnotation annotation);

	/**
	 * new algorithmannotation
	 *
	 * @param annotation annotationinfo
	 * @return whether successfully
	 */
	boolean updateAnnotation(AlgorithmAnnotation annotation);

	/**
	 * Delete algorithmannotation
	 *
	 * @param id annotationID
	 * @return whether successfully
	 */
	boolean deleteAnnotation(Long id);

	/**
	 * Batch delete algorithmannotation
	 *
	 * @param ids annotationID
	 * @return whether successfully
	 */
	boolean batchDeleteAnnotations(List<Long> ids);

	/**
	 * new annotation
	 *
	 * @param id annotationID
	 * @param annotatedCount already annotation
	 * @return whether successfully
	 */
	boolean updateAnnotationProgress(Long id, Integer annotatedCount);

	/**
	 * new annotation
	 *
	 * @param ids annotationID
	 * @param annotationStatus new annotation
	 * @return whether successfully
	 */
	boolean batchUpdateAnnotationStatus(List<Long> ids, String annotationStatus);

	/**
	 * startannotationtask
	 *
	 * @param id annotationID
	 * @return whether successfully
	 */
	boolean startAnnotationTask(Long id);

	/**
	 * annotationtask
	 *
	 * @param id annotationID
	 * @return whether successfully
	 */
	boolean completeAnnotationTask(Long id);

	/**
	 * annotationtask
	 *
	 * @param id annotationID
	 * @return whether successfully
	 */
	boolean resetAnnotationTask(Long id);

	/**
	 * Import annotationdata
	 *
	 * @param id annotationID
	 * @param dataPath data
	 * @return Import
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
	 * Get annotation
	 *
	 * @return annotation info
	 */
	List<Map<String, Object>> getAnnotationTypeStatistics();

	/**
	 * Get annotation
	 *
	 * @return annotation info
	 */
	List<Map<String, Object>> getAnnotationStatusStatistics();

	/**
	 * Get annotation
	 *
	 * @return annotation info
	 */
	List<Map<String, Object>> getProgressStatistics();

	/**
	 * Get annotation
	 *
	 * @return annotation info
	 */
	Map<String, Object> getWorkloadStatistics();

	/**
	 * annotationdata
	 *
	 * @param id annotationID
	 * @return
	 */
	Map<String, Object> validateAnnotationData(Long id);

	/**
	 * annotationdata dataset
	 *
	 * @param annotationId annotationID
	 * @return whether successfully
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
