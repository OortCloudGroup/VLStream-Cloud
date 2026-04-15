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
 * 算法标注控制器
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm-annotation")
@RequiredArgsConstructor
@Api(tags = "算法标注管理")
public class AlgorithmAnnotationController {

    private final AlgorithmAnnotationService algorithmAnnotationService;

    /**
     * 分页查询算法标注列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询算法标注列表", description = "根据条件分页查询算法标注")
    public Result<IPage<AlgorithmAnnotation>> getAnnotationPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "标注名称") @RequestParam(required = false) String annotationName,
            @Parameter(description = "标注类型") @RequestParam(required = false) String annotationType,
            @Parameter(description = "标注状态") @RequestParam(required = false) String annotationStatus) {
        
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
     * 根据标注类型查询标注列表
     */
    @GetMapping("/type/{annotationType}")
    @Operation(summary = "根据标注类型查询标注列表", description = "获取指定类型的所有标注")
    public Result<List<AlgorithmAnnotation>> getAnnotationsByType(
            @Parameter(description = "标注类型", example = "object_detection") @PathVariable String annotationType) {
        
        log.info("根据标注类型查询标注列表：{}", annotationType);
        
        List<AlgorithmAnnotation> annotations = algorithmAnnotationService.getByAnnotationType(annotationType);
        return Result.success(annotations);
    }

    /**
     * 根据标注状态查询标注列表
     */
    @GetMapping("/status/{annotationStatus}")
    @Operation(summary = "根据标注状态查询标注列表", description = "获取指定状态的所有标注")
    public Result<List<AlgorithmAnnotation>> getAnnotationsByStatus(
            @Parameter(description = "标注状态", example = "partial") @PathVariable String annotationStatus) {
        
        log.info("根据标注状态查询标注列表：{}", annotationStatus);
        
        List<AlgorithmAnnotation> annotations = algorithmAnnotationService.getByAnnotationStatus(annotationStatus);
        return Result.success(annotations);
    }

    /**
     * 根据ID查询标注详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询标注详情", description = "根据ID获取标注详细信息")
    public Result<AlgorithmAnnotation> getAnnotationById(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("查询标注详情：ID={}", id);
        
        AlgorithmAnnotation annotation = algorithmAnnotationService.getById(id);
        if (annotation == null) {
            return Result.error("标注不存在");
        }
        
        return Result.success(annotation);
    }

    /**
     * 创建算法标注
     */
    @PostMapping
    @Operation(summary = "创建算法标注", description = "新增算法标注")
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
     * 更新算法标注
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新算法标注", description = "根据ID更新标注信息")
    public Result<String> updateAnnotation(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id,
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
     * 删除算法标注
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除算法标注", description = "根据ID删除标注（软删除）")
    public Result<String> deleteAnnotation(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("删除算法标注：ID={}", id);
        
        boolean success = algorithmAnnotationService.deleteAnnotation(id);
        if (success) {
            return Result.success("标注删除成功");
        } else {
            return Result.error("标注删除失败");
        }
    }

    /**
     * 批量删除算法标注
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除算法标注", description = "根据ID列表批量删除标注")
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
     * 更新标注进度
     */
    @PutMapping("/{id}/progress")
    @Operation(summary = "更新标注进度", description = "更新标注的进度信息")
    public Result<String> updateAnnotationProgress(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "已标注数量", example = "50") @RequestParam @NotNull Integer annotatedCount) {
        
        log.info("更新标注进度：ID={}, AnnotatedCount={}", id, annotatedCount);
        
        boolean success = algorithmAnnotationService.updateAnnotationProgress(id, annotatedCount);
        if (success) {
            return Result.success("标注进度更新成功");
        } else {
            return Result.error("标注进度更新失败");
        }
    }

    /**
     * 批量更新标注状态
     */
    @PutMapping("/batch/status")
    @Operation(summary = "批量更新标注状态", description = "批量更新标注的状态")
    public Result<String> batchUpdateAnnotationStatus(
            @RequestBody List<Long> ids,
            @Parameter(description = "标注状态", example = "completed") @RequestParam @NotNull String annotationStatus) {
        
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
     * 开始标注任务
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "开始标注任务", description = "开始指定的标注任务")
    public Result<String> startAnnotationTask(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("开始标注任务：ID={}", id);
        
        boolean success = algorithmAnnotationService.startAnnotationTask(id);
        if (success) {
            return Result.success("标注任务开始成功");
        } else {
            return Result.error("标注任务开始失败");
        }
    }

    /**
     * 完成标注任务
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成标注任务", description = "完成指定的标注任务")
    public Result<String> completeAnnotationTask(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("完成标注任务：ID={}", id);
        
        boolean success = algorithmAnnotationService.completeAnnotationTask(id);
        if (success) {
            return Result.success("标注任务完成成功");
        } else {
            return Result.error("标注任务完成失败");
        }
    }

    /**
     * 重置标注任务
     */
    @PostMapping("/{id}/reset")
    @Operation(summary = "重置标注任务", description = "重置指定的标注任务")
    public Result<String> resetAnnotationTask(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("重置标注任务：ID={}", id);
        
        boolean success = algorithmAnnotationService.resetAnnotationTask(id);
        if (success) {
            return Result.success("标注任务重置成功");
        } else {
            return Result.error("标注任务重置失败");
        }
    }

    /**
     * 导出标注数据
     */
    @PostMapping("/{id}/export")
    @Operation(summary = "导出标注数据", description = "导出指定标注的数据")
    public void exportAnnotationData(@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id, HttpServletResponse response) {
        log.info("Download dataset zip request, id={}", id);
        algorithmAnnotationService.downloadAnnotationDataset(id, response);
    }

    /**
     * 导入标注数据
     */
    @PostMapping("/{id}/import")
    @Operation(summary = "导入标注数据", description = "导入标注数据")
    public Result<Map<String, Object>> importAnnotationData(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "数据路径") @RequestParam @NotNull String dataPath) {
        
        log.info("导入标注数据：ID={}, DataPath={}", id, dataPath);
        
        Map<String, Object> result = algorithmAnnotationService.importAnnotationData(id, dataPath);
        if (result != null) {
            return Result.success(result);
        } else {
            return Result.error("标注数据导入失败");
        }
    }

    /**
     * 验证标注数据
     */
    @PostMapping("/{id}/validate")
    @Operation(summary = "验证标注数据", description = "验证指定标注的数据质量")
    public Result<Map<String, Object>> validateAnnotationData(
            @Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("验证标注数据：ID={}", id);
        
        Map<String, Object> result = algorithmAnnotationService.validateAnnotationData(id);
        if (result != null) {
            return Result.success(result);
        } else {
            return Result.error("标注数据验证失败");
        }
    }

    /**
     * 获取标注类型统计
     */
    @GetMapping("/statistics/type")
    @Operation(summary = "获取标注类型统计", description = "获取各类型的标注数量统计")
    public Result<List<Map<String, Object>>> getAnnotationTypeStatistics() {
        log.info("获取标注类型统计");
        
        List<Map<String, Object>> statistics = algorithmAnnotationService.getAnnotationTypeStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取标注状态统计
     */
    @GetMapping("/statistics/status")
    @Operation(summary = "获取标注状态统计", description = "获取各状态的标注数量统计")
    public Result<List<Map<String, Object>>> getAnnotationStatusStatistics() {
        log.info("获取标注状态统计");
        
        List<Map<String, Object>> statistics = algorithmAnnotationService.getAnnotationStatusStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取标注进度统计
     */
    @GetMapping("/statistics/progress")
    @Operation(summary = "获取标注进度统计", description = "获取各进度区间的标注数量统计")
    public Result<List<Map<String, Object>>> getProgressStatistics() {
        log.info("获取标注进度统计");
        
        List<Map<String, Object>> statistics = algorithmAnnotationService.getProgressStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取标注工作量统计
     */
    @GetMapping("/statistics/workload")
    @Operation(summary = "获取标注工作量统计", description = "获取标注工作量的总体统计")
    public Result<Map<String, Object>> getWorkloadStatistics() {
        log.info("获取标注工作量统计");
        
        Map<String, Object> statistics = algorithmAnnotationService.getWorkloadStatistics();
        return Result.success(statistics);
    }

    /**
     * 保存标注数据到数据集文件
     */
    @PostMapping("/{id}/save-dataset")
    @Operation(summary = "保存标注数据到数据集", description = "将标注数据保存到数据集文件并更新数据库路径")
    public Result<String> saveAnnotationToDataset(@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

        log.info("保存标注数据到数据集：ID={}", id);

        boolean success = algorithmAnnotationService.saveAnnotationToDataset(id);
        if (success) {
            return Result.success("标注数据保存到数据集成功");
        } else {
            return Result.error("标注数据保存到数据集失败");
        }
    }
}
