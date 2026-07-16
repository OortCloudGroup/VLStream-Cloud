/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm annotation routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmAnnotation")
public class VlsAlgorithmAnnotationController extends VlsControllerSupport {

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
        return operationResult(annotationService.deleteAnnotation(id), "Annotation was not deleted");
    }

    /**
     * Delete multiple annotation tasks.
     */
    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteAnnotations(@RequestBody List<Long> ids) {
        return operationResult(annotationService.deleteAnnotations(ids), "No annotations were deleted");
    }

    /**
     * Start an annotation task.
     */
    @PostMapping("/{id}/start")
    public BladeResult<Boolean> startAnnotationTask(@PathVariable Long id) {
        return operationResult(annotationService.startAnnotationTask(id), "Annotation task was not started");
    }

    /**
     * Complete an annotation task.
     */
    @PostMapping("/{id}/complete")
    public BladeResult<Boolean> completeAnnotationTask(@PathVariable Long id) {
        return operationResult(annotationService.completeAnnotationTask(id), "Annotation task was not completed");
    }

    /**
     * Reset an annotation task.
     */
    @PostMapping("/{id}/reset")
    public BladeResult<Boolean> resetAnnotationTask(@PathVariable Long id) {
        return operationResult(annotationService.resetAnnotationTask(id), "Annotation task was not reset");
    }

    /**
     * Update annotation progress from the frontend JSON body.
     */
    @PutMapping("/{id}/progress")
    public BladeResult<Boolean> updateAnnotationProgress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return operationResult(annotationService.updateAnnotationProgress(id, toInteger(body == null ? null : body.get("annotatedCount"))),
            "Annotation progress was not updated");
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
        return operationResult(annotationService.saveDataset(id, annotationData), "Annotation dataset was not saved");
    }

    /** Return one annotation through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<AlgorithmAnnotation> detail(@RequestParam Long id) {
        return getAnnotationById(id);
    }

    /** Return the annotation page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<AlgorithmAnnotation>> list(@RequestParam(required = false) Long current,
                                                            @RequestParam(required = false) Long size,
                                                            @RequestParam(required = false) String annotationName,
                                                            @RequestParam(required = false) String annotationType,
                                                            @RequestParam(required = false) String annotationStatus,
                                                            @RequestParam(required = false) String startTime,
                                                            @RequestParam(required = false) String endTime) {
        return getAnnotationPage(current, size, annotationName, annotationType, annotationStatus, startTime, endTime);
    }

    /** Create an annotation through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<AlgorithmAnnotation> save(@RequestBody AlgorithmAnnotation annotation) {
        return createAnnotation(annotation);
    }

    /** Update an annotation through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<AlgorithmAnnotation> update(@RequestBody AlgorithmAnnotation annotation) {
        if (annotation == null || annotation.getId() == null) {
            return BladeResult.fail("Annotation ID is required");
        }
        return updateAnnotation(annotation.getId(), annotation);
    }

    /** Insert or update an annotation through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<AlgorithmAnnotation> submit(@RequestBody AlgorithmAnnotation annotation) {
        return annotation != null && annotation.getId() != null
            ? updateAnnotation(annotation.getId(), annotation)
            : createAnnotation(annotation);
    }

    /** Delete annotations by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(annotationService.deleteAnnotations(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual filtered annotation rows. */
    @GetMapping("/export-vlsAlgorithmAnnotation")
    public void exportVlsAlgorithmAnnotation(@RequestParam(required = false) String annotationName,
                                             @RequestParam(required = false) String annotationType,
                                             @RequestParam(required = false) String annotationStatus,
                                             @RequestParam(required = false) String startTime,
                                             @RequestParam(required = false) String endTime,
                                             HttpServletResponse response) {
        BladePage<AlgorithmAnnotation> page = annotationService.getAnnotationPage(Long.valueOf(1L),
            Long.valueOf(Integer.MAX_VALUE), annotationName, annotationType, annotationStatus, startTime, endTime);
        ExcelUtil.exportExcel(page.getRecords(), "Algorithm Annotations", AlgorithmAnnotation.class, response);
    }

    /** Query annotations by type through the source route. */
    @GetMapping("/type/{annotationType}")
    public BladeResult<List<AlgorithmAnnotation>> getAnnotationsByType(@PathVariable String annotationType) {
        return BladeResult.success(annotationService.searchAnnotations(Long.valueOf(1L), Long.valueOf(Integer.MAX_VALUE),
            null, annotationType, null).getRecords());
    }

    /** Query annotations by status through the source route. */
    @GetMapping("/status/{annotationStatus}")
    public BladeResult<List<AlgorithmAnnotation>> getAnnotationsByStatus(@PathVariable String annotationStatus) {
        return BladeResult.success(annotationService.searchAnnotations(Long.valueOf(1L), Long.valueOf(Integer.MAX_VALUE),
            null, null, annotationStatus).getRecords());
    }

    /** Apply a supported status transition to every requested annotation. */
    @PutMapping("/batch/status")
    public BladeResult<Boolean> batchUpdateAnnotationStatus(@RequestBody List<Long> ids,
                                                            @RequestParam String annotationStatus) {
        if (ids == null || ids.isEmpty()) {
            return BladeResult.fail("Please select annotations to update");
        }
        String operation;
        if ("completed".equalsIgnoreCase(annotationStatus)) {
            operation = "completed";
        } else if ("partial".equalsIgnoreCase(annotationStatus)) {
            operation = "partial";
        } else if ("none".equalsIgnoreCase(annotationStatus)) {
            operation = "reset";
        } else {
            return BladeResult.fail("Unsupported annotation status transition: " + annotationStatus);
        }
        return BladeResult.success(annotationService.batchOperation(operation, ids));
    }

    /** Reject the source path-only import route with an actionable real-import alternative. */
    @PostMapping("/{id}/import")
    public BladeResult<Map<String, Object>> importAnnotationData(@PathVariable Long id,
                                                                 @RequestParam String dataPath) {
        return BladeResult.fail("Path-only annotation import is unsafe and unsupported; use /" + id + "/import-zip with a real dataset archive");
    }

    /** Validate persisted annotation counters and progress without returning fabricated success. */
    @PostMapping("/{id}/validate")
    public BladeResult<Map<String, Object>> validateAnnotationData(@PathVariable Long id) {
        AlgorithmAnnotation annotation = annotationService.getAnnotationById(id);
        if (annotation == null) {
            return BladeResult.fail("Annotation does not exist");
        }
        int total = annotation.getTotalCount() == null ? 0 : annotation.getTotalCount().intValue();
        int annotated = annotation.getAnnotatedCount() == null ? 0 : annotation.getAnnotatedCount().intValue();
        int progress = annotation.getProgress() == null ? 0 : annotation.getProgress().intValue();
        List<String> errors = new ArrayList<String>();
        if (total < 0 || annotated < 0 || annotated > total) {
            errors.add("Annotation counters are inconsistent");
        }
        if (progress < 0 || progress > 100) {
            errors.add("Progress must be between 0 and 100");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("valid", Boolean.valueOf(errors.isEmpty()));
        result.put("totalCount", Integer.valueOf(total));
        result.put("annotatedCount", Integer.valueOf(annotated));
        result.put("progress", Integer.valueOf(progress));
        result.put("errors", errors);
        return errors.isEmpty() ? BladeResult.success(result) : BladeResult.<Map<String, Object>>fail(String.join("; ", errors));
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
