package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AlgorithmAnnotation;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Algorithm Annotation Service Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AlgorithmAnnotationService extends IService<AlgorithmAnnotation> {

    /**
     * Query algorithm annotation list with pagination
     * 
     * @param page Pagination parameter
     * @param annotationName Annotation name (fuzzy query)
     * @param annotationType Annotation type
     * @param annotationStatus Annotation status
     * @return Pagination result
     */
    IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
                                                   String annotationName,
                                                   String annotationType,
                                                   String annotationStatus);

    /**
     * Query annotation list by annotation type
     * 
     * @param annotationType Annotation type
     * @return Annotation list
     */
    List<AlgorithmAnnotation> getByAnnotationType(String annotationType);

    /**
     * Query annotation list by annotation status
     * 
     * @param annotationStatus Annotation status
     * @return Annotation list
     */
    List<AlgorithmAnnotation> getByAnnotationStatus(String annotationStatus);

    /**
     * Create algorithm annotation
     * 
     * @param annotation Annotation information
     * @return Whether successful
     */
    boolean createAnnotation(AlgorithmAnnotation annotation);

    /**
     * Update algorithm annotation
     * 
     * @param annotation Annotation information
     * @return Whether successful
     */
    boolean updateAnnotation(AlgorithmAnnotation annotation);

    /**
     * Delete algorithm annotation
     * 
     * @param id Annotation ID
     * @return Whether successful
     */
    boolean deleteAnnotation(Long id);

    /**
     * Batch delete algorithm annotations
     * 
     * @param ids Annotation ID list
     * @return Whether successful
     */
    boolean batchDeleteAnnotations(List<Long> ids);

    /**
     * Update annotation progress
     * 
     * @param id Annotation ID
     * @param annotatedCount Annotated count
     * @return Whether successful
     */
    boolean updateAnnotationProgress(Long id, Integer annotatedCount);

    /**
     * Batch update annotation status
     * 
     * @param ids Annotation ID list
     * @param annotationStatus New annotation status
     * @return Whether successful
     */
    boolean batchUpdateAnnotationStatus(List<Long> ids, String annotationStatus);

    /**
     * Start annotation task
     * 
     * @param id Annotation ID
     * @return Whether successful
     */
    boolean startAnnotationTask(Long id);

    /**
     * Complete annotation task
     * 
     * @param id Annotation ID
     * @return Whether successful
     */
    boolean completeAnnotationTask(Long id);

    /**
     * Reset annotation task
     * 
     * @param id Annotation ID
     * @return Whether successful
     */
    boolean resetAnnotationTask(Long id);

    /**
     * Import annotation data
     * 
     * @param id Annotation ID
     * @param dataPath Data path
     * @return Import result
     */
    Map<String, Object> importAnnotationData(Long id, String dataPath);

    /**
     * Get annotation type statistics
     * 
     * @return Annotation type statistics
     */
    List<Map<String, Object>> getAnnotationTypeStatistics();

    /**
     * Get annotation status statistics
     * 
     * @return Annotation status statistics
     */
    List<Map<String, Object>> getAnnotationStatusStatistics();

    /**
     * Get progress statistics
     * 
     * @return Progress statistics
     */
    List<Map<String, Object>> getProgressStatistics();

    /**
     * Get workload statistics
     * 
     * @return Workload statistics
     */
    Map<String, Object> getWorkloadStatistics();

    /**
     * Validate annotation data
     * 
     * @param id Annotation ID
     * @return Validation result
     */
    Map<String, Object> validateAnnotationData(Long id);

    /**
     * Save annotation data to dataset file
     * 
     * @param annotationId Annotation ID
     * @return Whether save successful
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
