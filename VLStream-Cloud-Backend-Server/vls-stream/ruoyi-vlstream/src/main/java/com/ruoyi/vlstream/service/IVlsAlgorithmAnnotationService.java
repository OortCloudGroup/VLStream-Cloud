/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IVlsAlgorithmAnnotationService {

    /**
     * Return a SpringBlade-compatible page for frontend annotation tables.
     */
    BladePage<AlgorithmAnnotation> getAnnotationPage(Long current, Long size, String annotationName,
                                                     String annotationType, String annotationStatus,
                                                     String startTime, String endTime);

    /**
     * Return an annotation task by id.
     */
    AlgorithmAnnotation getAnnotationById(Long id);

    /**
     * Create an annotation task with VLS defaults.
     */
    AlgorithmAnnotation createAnnotation(AlgorithmAnnotation annotation);

    /**
     * Update an annotation task by id.
     */
    AlgorithmAnnotation updateAnnotation(Long id, AlgorithmAnnotation annotation);

    /**
     * Delete one annotation task.
     */
    boolean deleteAnnotation(Long id);

    /**
     * Delete multiple annotation tasks.
     */
    boolean deleteAnnotations(List<Long> ids);

    /**
     * Mark an annotation task as in progress.
     */
    boolean startAnnotationTask(Long id);

    /**
     * Mark an annotation task as completed.
     */
    boolean completeAnnotationTask(Long id);

    /**
     * Reset an annotation task to unannotated.
     */
    boolean resetAnnotationTask(Long id);

    /**
     * Update annotated count and derived progress.
     */
    boolean updateAnnotationProgress(Long id, Integer annotatedCount);

    /**
     * Export annotation data as a downloadable blob.
     */
    ResponseEntity<byte[]> exportAnnotationData(Long id);

    /**
     * Import a zip dataset into the annotation task compatibility surface.
     */
    Map<String, Object> importAnnotationZip(Long id, MultipartFile file);

    /**
     * Return aggregate annotation statistics.
     */
    Map<String, Object> getStatistics();

    /**
     * Return grouped statistics by annotation type.
     */
    List<Map<String, Object>> getTypeStatistics();

    /**
     * Return grouped statistics by annotation status.
     */
    List<Map<String, Object>> getStatusStatistics();

    /**
     * Return grouped statistics by progress range.
     */
    List<Map<String, Object>> getProgressStatistics();

    /**
     * Return workload totals for annotation tasks.
     */
    Map<String, Object> getWorkloadStatistics();

    /**
     * Search annotations with the frontend search filters.
     */
    BladePage<AlgorithmAnnotation> searchAnnotations(Long current, Long size, String annotationName,
                                                    String annotationType, String annotationStatus);

    /**
     * Execute a frontend batch operation.
     */
    boolean batchOperation(String operation, List<Long> ids);

    /**
     * Persist dataset metadata and return whether the operation succeeded.
     */
    boolean saveDataset(Long id, String annotationData);
}
