package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.AlgorithmAnnotation;
import com.vlstream.service.AlgorithmAnnotationService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Algorithm Annotation Controller
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm-annotation")
@RequiredArgsConstructor
@Api(tags = "Algorithm Annotation Management")
public class AlgorithmAnnotationController {

    private final AlgorithmAnnotationService algorithmAnnotationService;

    /**
     * Page query algorithm annotation list
     */
    @GetMapping("/page")
    @Operation(summary = "Query algorithm annotation list with pagination", description = "Query algorithm annotations with pagination based on conditions")
    public Result<IPage<AlgorithmAnnotation>> getAnnotationPage(
            @Parameter(description = "Current page", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Annotation name") @RequestParam(required = false) String annotationName,
            @Parameter(description = "Annotation type") @RequestParam(required = false) String annotationType,
            @Parameter(description = "Annotation status") @RequestParam(required = false) String annotationStatus) {
        
        log.info("分页查询算法标注列表，参数：current={}, size={}, annotationName={}, annotationType={}, annotationStatus={}", 
                current, size, annotationName, annotationType, annotationStatus);
        
        Page<AlgorithmAnnotation> page = new Page<>(current, size);
        IPage<AlgorithmAnnotation> result = algorithmAnnotationService.selectAnnotationPage(page, annotationName, annotationType, annotationStatus);
        for (AlgorithmAnnotation annotation : result.getRecords()) {
            annotation.setDatasetPath(annotation.getDatasetPath() + "/dataset.yaml");
        }
        return Result.success(result);
    }

    /**
     * Query annotation list by annotation type
     */
    @GetMapping("/type/{annotationType}")
    @Operation(summary = "Query annotation list by annotation type", description = "Get all annotations of specified type")
    public Result<List<AlgorithmAnnotation>> getAnnotationsByType(
            @Parameter(description = "Annotation type", example = "object_detection") @PathVariable String annotationType) {
        
        log.info("根据标注类型查询标注列表：{}", annotationType);
        
        List<AlgorithmAnnotation> annotations = algorithmAnnotationService.getByAnnotationType(annotationType);
        return Result.success(annotations);
    }

    /**
     * Query annotation list by annotation status
     */
    @GetMapping("/status/{annotationStatus}")
    @Operation(summary = "Query annotation list by annotation status", description = "Get all annotations of specified status")
    public Result<List<AlgorithmAnnotation>> getAnnotationsByStatus(
            @Parameter(description = "Annotation status", example = "partial") @PathVariable String annotationStatus) {
        
        log.info("根据标注状态查询标注列表：{}", annotationStatus);
        
        List<AlgorithmAnnotation> annotations = algorithmAnnotationService.getByAnnotationStatus(annotationStatus);
        return Result.success(annotations);
    }

    /**
     * Query annotation details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Query annotation details", description = "Get annotation details by ID")
    public Result<AlgorithmAnnotation> getAnnotationById(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("查询标注详情：ID={}", id);
        
        AlgorithmAnnotation annotation = algorithmAnnotationService.getById(id);
        if (annotation == null) {
            return Result.error("标注不存在");
        }
        
        return Result.success(annotation);
    }

    /**
     * Create algorithm annotation
     */
    @PostMapping
    @Operation(summary = "Create algorithm annotation", description = "Add new algorithm annotation")
    public Result<String> createAnnotation(@Valid @RequestBody AlgorithmAnnotation annotation) {
        log.info("创建算法标注：{}", annotation.getAnnotationName());
        
        boolean success = algorithmAnnotationService.createAnnotation(annotation);
        if (success) {
            return Result.success("标注创建成功");
        } else {
            return Result.error("标注创建失败，名称可能已存在");
        }
    }

    /**
     * Update algorithm annotation
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update algorithm annotation", description = "Update annotation information by ID")
    public Result<String> updateAnnotation(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
            @Valid @RequestBody AlgorithmAnnotation annotation) {
        
        log.info("更新算法标注：ID={}", id);
        
        annotation.setId(id);
        boolean success = algorithmAnnotationService.updateAnnotation(annotation);
        
        if (success) {
            return Result.success("标注更新成功");
        } else {
            return Result.error("标注更新失败");
        }
    }

    /**
     * Delete algorithm annotation
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete algorithm annotation", description = "Delete annotation by ID (soft delete)")
    public Result<String> deleteAnnotation(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("删除算法标注：ID={}", id);
        
        boolean success = algorithmAnnotationService.deleteAnnotation(id);
        if (success) {
            return Result.success("标注删除成功");
        } else {
            return Result.error("标注删除失败");
        }
    }

    /**
     * Batch delete algorithm annotations
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Batch delete algorithm annotations", description = "Batch delete annotations by ID list")
    public Result<String> batchDeleteAnnotations(@RequestBody List<Long> ids) {
        log.info("批量删除算法标注：IDs={}", ids);
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的标注");
        }
        
        boolean success = algorithmAnnotationService.batchDeleteAnnotations(ids);
        if (success) {
            return Result.success("标注批量删除成功");
        } else {
            return Result.error("标注批量删除失败");
        }
    }

    /**
     * Update annotation progress
     */
    @PutMapping("/{id}/progress")
    @Operation(summary = "Update annotation progress", description = "Update progress information of annotation")
    public Result<String> updateAnnotationProgress(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "Annotated count", example = "50") @RequestParam @NotNull Integer annotatedCount) {
        
        log.info("更新标注进度：ID={}, AnnotatedCount={}", id, annotatedCount);
        
        boolean success = algorithmAnnotationService.updateAnnotationProgress(id, annotatedCount);
        if (success) {
            return Result.success("标注进度更新成功");
        } else {
            return Result.error("标注进度更新失败");
        }
    }

    /**
     * Batch update annotation status
     */
    @PutMapping("/batch/status")
    @Operation(summary = "Batch update annotation status", description = "Batch update status of annotations")
    public Result<String> batchUpdateAnnotationStatus(
            @RequestBody List<Long> ids,
            @Parameter(description = "Annotation status", example = "completed") @RequestParam @NotNull String annotationStatus) {
        
        log.info("批量更新标注状态：IDs={}, Status={}", ids, annotationStatus);
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要更新的标注");
        }
        
        boolean success = algorithmAnnotationService.batchUpdateAnnotationStatus(ids, annotationStatus);
        if (success) {
            return Result.success("标注状态批量更新成功");
        } else {
            return Result.error("标注状态批量更新失败");
        }
    }

    /**
     * Start annotation task
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "Start annotation task", description = "Start specified annotation task")
    public Result<String> startAnnotationTask(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("开始标注任务：ID={}", id);
        
        boolean success = algorithmAnnotationService.startAnnotationTask(id);
        if (success) {
            return Result.success("标注任务开始成功");
        } else {
            return Result.error("标注任务开始失败");
        }
    }

    /**
     * Complete annotation task
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete annotation task", description = "Complete specified annotation task")
    public Result<String> completeAnnotationTask(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("完成标注任务：ID={}", id);
        
        boolean success = algorithmAnnotationService.completeAnnotationTask(id);
        if (success) {
            return Result.success("标注任务完成成功");
        } else {
            return Result.error("标注任务完成失败");
        }
    }

    /**
     * Reset annotation task
     */
    @PostMapping("/{id}/reset")
    @Operation(summary = "Reset annotation task", description = "Reset specified annotation task")
    public Result<String> resetAnnotationTask(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("重置标注任务：ID={}", id);
        
        boolean success = algorithmAnnotationService.resetAnnotationTask(id);
        if (success) {
            return Result.success("标注任务重置成功");
        } else {
            return Result.error("标注任务重置失败");
        }
    }

    /**
     * Export annotation data
     */
    @PostMapping("/{id}/export")
    @Operation(summary = "Export annotation data", description = "Export data of specified annotation")
    public void exportAnnotationData(@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id, HttpServletResponse response) {
        log.info("Download dataset zip request, id={}", id);
        algorithmAnnotationService.downloadAnnotationDataset(id, response);
    }

    /**
     * Import annotation data
     */
    @PostMapping("/{id}/import")
    @Operation(summary = "Import annotation data", description = "Import annotation data")
    public Result<Map<String, Object>> importAnnotationData(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "Data path") @RequestParam @NotNull String dataPath) {
        
        log.info("导入标注数据：ID={}, DataPath={}", id, dataPath);
        
        Map<String, Object> result = algorithmAnnotationService.importAnnotationData(id, dataPath);
        if (result != null) {
            return Result.success(result);
        } else {
            return Result.error("标注数据导入失败");
        }
    }

    /**
     * Validate annotation data
     */
    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate annotation data", description = "Validate data quality of specified annotation")
    public Result<Map<String, Object>> validateAnnotationData(
            @Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("验证标注数据：ID={}", id);
        
        Map<String, Object> result = algorithmAnnotationService.validateAnnotationData(id);
        if (result != null) {
            return Result.success(result);
        } else {
            return Result.error("标注数据验证失败");
        }
    }

    /**
     * Get annotation type statistics
     */
    @GetMapping("/statistics/type")
    @Operation(summary = "Get annotation type statistics", description = "Get annotation count statistics by type")
    public Result<List<Map<String, Object>>> getAnnotationTypeStatistics() {
        log.info("获取标注类型统计");
        
        List<Map<String, Object>> statistics = algorithmAnnotationService.getAnnotationTypeStatistics();
        return Result.success(statistics);
    }

    /**
     * Get annotation status statistics
     */
    @GetMapping("/statistics/status")
    @Operation(summary = "Get annotation status statistics", description = "Get annotation count statistics by status")
    public Result<List<Map<String, Object>>> getAnnotationStatusStatistics() {
        log.info("获取标注状态统计");
        
        List<Map<String, Object>> statistics = algorithmAnnotationService.getAnnotationStatusStatistics();
        return Result.success(statistics);
    }

    /**
     * Get annotation progress statistics
     */
    @GetMapping("/statistics/progress")
    @Operation(summary = "Get annotation progress statistics", description = "Get annotation count statistics by progress range")
    public Result<List<Map<String, Object>>> getProgressStatistics() {
        log.info("获取标注进度统计");
        
        List<Map<String, Object>> statistics = algorithmAnnotationService.getProgressStatistics();
        return Result.success(statistics);
    }

    /**
     * Get annotation workload statistics
     */
    @GetMapping("/statistics/workload")
    @Operation(summary = "Get annotation workload statistics", description = "Get overall statistics of annotation workload")
    public Result<Map<String, Object>> getWorkloadStatistics() {
        log.info("获取标注工作量统计");
        
        Map<String, Object> statistics = algorithmAnnotationService.getWorkloadStatistics();
        return Result.success(statistics);
    }

    /**
     * Save annotation data to dataset file
     */
    @PostMapping("/{id}/save-dataset")
    @Operation(summary = "Save annotation data to dataset", description = "Save annotation data to dataset file and update database path")
    public Result<String> saveAnnotationToDataset(@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

        log.info("保存标注数据到数据集：ID={}", id);

        boolean success = algorithmAnnotationService.saveAnnotationToDataset(id);
        if (success) {
            return Result.success("标注数据保存到数据集成功");
        } else {
            return Result.error("标注数据保存到数据集失败");
        }
    }
}
