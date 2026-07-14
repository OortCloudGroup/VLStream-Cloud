/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import com.ruoyi.vlstream.service.IVlsAlgorithmAnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Algorithm annotation routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmAnnotation")
public class VlsAlgorithmAnnotationController {

    private final IVlsAlgorithmAnnotationService annotationService;

    /**
     * Return paged annotation tasks for the frontend table.
     */
    @GetMapping("/page")
    public BladeResult<BladePage<AlgorithmAnnotation>> getAnnotationPage(@RequestParam(required = false) Long current,
                                                                        @RequestParam(required = false) Long size,
                                                                        @RequestParam(required = false) String annotationName,
                                                                        @RequestParam(required = false) String annotationType,
                                                                        @RequestParam(required = false) String annotationStatus,
                                                                        @RequestParam(required = false) String startTime,
                                                                        @RequestParam(required = false) String endTime) {
        return BladeResult.success(annotationService.getAnnotationPage(current, size, annotationName, annotationType, annotationStatus, startTime, endTime));
    }

    /**
     * Return an annotation task by id.
     */
    @GetMapping("/{id}")
    public BladeResult<AlgorithmAnnotation> getAnnotationById(@PathVariable Long id) {
        AlgorithmAnnotation annotation = annotationService.getAnnotationById(id);
        return annotation == null ? BladeResult.<AlgorithmAnnotation>fail("Annotation does not exist") : BladeResult.success(annotation);
    }

    /**
     * Create an annotation task.
     */
    @PostMapping
    public BladeResult<AlgorithmAnnotation> createAnnotation(@RequestBody AlgorithmAnnotation annotation) {
        try {
            return BladeResult.success(annotationService.createAnnotation(annotation));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Update an annotation task.
     */
    @PutMapping("/{id}")
    public BladeResult<AlgorithmAnnotation> updateAnnotation(@PathVariable Long id, @RequestBody AlgorithmAnnotation annotation) {
        try {
            return BladeResult.success(annotationService.updateAnnotation(id, annotation));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Delete an annotation task.
     */
    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteAnnotation(@PathVariable Long id) {
        return BladeResult.success(annotationService.deleteAnnotation(id));
    }

    /**
     * Delete multiple annotation tasks.
     */
    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteAnnotations(@RequestBody List<Long> ids) {
        return BladeResult.success(annotationService.deleteAnnotations(ids));
    }

    /**
     * Start an annotation task.
     */
    @PostMapping("/{id}/start")
    public BladeResult<Boolean> startAnnotationTask(@PathVariable Long id) {
        return BladeResult.success(annotationService.startAnnotationTask(id));
    }

    /**
     * Complete an annotation task.
     */
    @PostMapping("/{id}/complete")
    public BladeResult<Boolean> completeAnnotationTask(@PathVariable Long id) {
        return BladeResult.success(annotationService.completeAnnotationTask(id));
    }

    /**
     * Reset an annotation task.
     */
    @PostMapping("/{id}/reset")
    public BladeResult<Boolean> resetAnnotationTask(@PathVariable Long id) {
        return BladeResult.success(annotationService.resetAnnotationTask(id));
    }

    /**
     * Update annotation progress from the frontend JSON body.
     */
    @PutMapping("/{id}/progress")
    public BladeResult<Boolean> updateAnnotationProgress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BladeResult.success(annotationService.updateAnnotationProgress(id, toInteger(body == null ? null : body.get("annotatedCount"))));
    }

    /**
     * Export annotation data as a downloadable blob.
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<byte[]> exportAnnotationData(@PathVariable Long id) {
        return annotationService.exportAnnotationData(id);
    }

    /**
     * Import a zip dataset for an annotation task.
     */
    @PostMapping(value = "/{id}/import-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BladeResult<Map<String, Object>> importAnnotationZip(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        try {
            return BladeResult.success(annotationService.importAnnotationZip(id, file));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Return aggregate annotation statistics.
     */
    @GetMapping("/statistics")
    public BladeResult<Map<String, Object>> getStatistics() {
        return BladeResult.success(annotationService.getStatistics());
    }

    /**
     * Return statistics by annotation type.
     */
    @GetMapping("/statistics/type")
    public BladeResult<List<Map<String, Object>>> getTypeStatistics() {
        return BladeResult.success(annotationService.getTypeStatistics());
    }

    /**
     * Return statistics by annotation status.
     */
    @GetMapping("/statistics/status")
    public BladeResult<List<Map<String, Object>>> getStatusStatistics() {
        return BladeResult.success(annotationService.getStatusStatistics());
    }

    /**
     * Return statistics by progress range.
     */
    @GetMapping("/statistics/progress")
    public BladeResult<List<Map<String, Object>>> getProgressStatistics() {
        return BladeResult.success(annotationService.getProgressStatistics());
    }

    /**
     * Return workload statistics.
     */
    @GetMapping("/statistics/workload")
    public BladeResult<Map<String, Object>> getWorkloadStatistics() {
        return BladeResult.success(annotationService.getWorkloadStatistics());
    }

    /**
     * Search annotation tasks.
     */
    @GetMapping("/search")
    public BladeResult<BladePage<AlgorithmAnnotation>> searchAnnotations(@RequestParam(required = false) Long current,
                                                                        @RequestParam(required = false) Long size,
                                                                        @RequestParam(required = false) String annotationName,
                                                                        @RequestParam(required = false) String annotationType,
                                                                        @RequestParam(required = false) String annotationStatus) {
        return BladeResult.success(annotationService.searchAnnotations(current, size, annotationName, annotationType, annotationStatus));
    }

    /**
     * Execute a frontend batch operation.
     */
    @PostMapping("/batch-operation")
    public BladeResult<Boolean> batchOperation(@RequestBody Map<String, Object> body) {
        Object operationValue = body == null ? null : body.get("operation");
        String operation = operationValue == null ? null : String.valueOf(operationValue);
        return BladeResult.success(annotationService.batchOperation(operation, toLongList(body == null ? null : body.get("ids"))));
    }

    /**
     * Save annotation data to dataset metadata.
     */
    @PostMapping("/{id}/save-dataset")
    public BladeResult<Boolean> saveDataset(@PathVariable Long id, @RequestParam(required = false) String annotationData) {
        return BladeResult.success(annotationService.saveDataset(id, annotationData));
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<Long> toLongList(Object value) {
        List<Long> ids = new ArrayList<Long>();
        if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) {
                Long id = toLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.valueOf(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
