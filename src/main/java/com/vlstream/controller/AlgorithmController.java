package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.Algorithm;
import com.vlstream.enums.AlgorithmCategoryEnum;
import com.vlstream.service.AlgorithmService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Algorithm Controller
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm")
@RequiredArgsConstructor
@Api(tags = "Algorithm Management")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    /**
     * Page query algorithm list
     */
    @GetMapping("/page")
    @Operation(summary = "Query algorithm list with pagination", description = "Query algorithms with pagination based on conditions")
    public Result<IPage<Algorithm>> getAlgorithmPage(
            @Parameter(description = "Current page", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Repository ID") @RequestParam(required = false) Long repositoryId,
            @Parameter(description = "Algorithm name") @RequestParam(required = false) String name,
            @Parameter(description = "Algorithm type") @RequestParam(required = false) String category,
            @Parameter(description = "Deployment status") @RequestParam(required = false) String deployStatus) {
        
        log.info("分页查询算法列表，参数：current={}, size={}, repositoryId={}, name={}, category={}, categoryName={}, deployStatus={}",
                current, size, repositoryId, name, category, deployStatus);
        
        Page<Algorithm> page = new Page<>(current, size);
        IPage<Algorithm> result = algorithmService.selectAlgorithmPage(page, repositoryId, name, category, deployStatus);
        for (Algorithm algorithm: result.getRecords()) {
            algorithm.setCategoryName(AlgorithmCategoryEnum.of(algorithm.getCategory()).getDescription());
        }
        return Result.success(result);
    }

    /**
     * Query algorithm list by repository ID
     */
    @GetMapping("/repository/{repositoryId}")
    @Operation(summary = "Query algorithm list by repository ID", description = "Get all algorithms in specified repository")
    public Result<List<Algorithm>> getAlgorithmsByRepositoryId(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long repositoryId) {
        
        log.info("根据仓库ID查询算法列表：{}", repositoryId);
        
        List<Algorithm> algorithms = algorithmService.getByRepositoryId(repositoryId);
        return Result.success(algorithms);
    }

    /**
     * Query algorithm list by category
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Query algorithm list by category", description = "Get all algorithms of specified category")
    public Result<List<Algorithm>> getAlgorithmsByCategory(
            @Parameter(description = "Algorithm type", example = "person-detection") @PathVariable String category) {
        
        log.info("根据分类查询算法列表：{}", category);
        
        List<Algorithm> algorithms = algorithmService.getByCategory(category);
        return Result.success(algorithms);
    }

    /**
     * Query algorithm details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Query algorithm details", description = "Get algorithm details by ID")
    public Result<Algorithm> getAlgorithmById(
            @Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("查询算法详情：ID={}", id);
        
        Algorithm algorithm = algorithmService.getById(id);
        if (algorithm == null) {
            return Result.error("算法不存在");
        }
        
        return Result.success(algorithm);
    }

    /**
     * Create algorithm
     */
    @PostMapping
    @Operation(summary = "Create algorithm", description = "Add new algorithm")
    public Result<String> createAlgorithm(@Valid @RequestBody Algorithm algorithm) {
        log.info("创建算法：{}", algorithm.getName());
        
        boolean success = algorithmService.createAlgorithm(algorithm);
        if (success) {
            return Result.success("算法创建成功");
        } else {
            return Result.error("算法创建失败，同一仓库下名称可能已存在");
        }
    }

    /**
     * Update algorithm
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update algorithm", description = "Update algorithm information by ID")
    public Result<String> updateAlgorithm(
            @Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id,
            @Valid @RequestBody Algorithm algorithm) {
        
        log.info("更新算法：ID={}", id);
        
        algorithm.setId(id);
        boolean success = algorithmService.updateAlgorithm(algorithm);
        
        if (success) {
            return Result.success("算法更新成功");
        } else {
            return Result.error("算法更新失败");
        }
    }

    /**
     * Delete algorithm
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete algorithm", description = "Delete algorithm by ID (soft delete)")
    public Result<String> deleteAlgorithm(
            @Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("删除算法：ID={}", id);
        
        boolean success = algorithmService.deleteAlgorithm(id);
        if (success) {
            return Result.success("算法删除成功");
        } else {
            return Result.error("算法删除失败");
        }
    }

    /**
     * Batch delete algorithms
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Batch delete algorithms", description = "Batch delete algorithms by ID list")
    public Result<String> batchDeleteAlgorithms(@RequestBody List<Long> ids) {
        log.info("批量删除算法：IDs={}", ids);
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的算法");
        }
        
        boolean success = algorithmService.batchDeleteAlgorithms(ids);
        if (success) {
            return Result.success("算法批量删除成功");
        } else {
            return Result.error("算法批量删除失败");
        }
    }

    /**
     * Update deployment status
     */
    @PutMapping("/{id}/deploy-status")
    @Operation(summary = "Update deployment status", description = "Update deployment status of algorithm")
    public Result<String> updateDeployStatus(
            @Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "Deployment status", example = "deployed") @RequestParam @NotNull String deployStatus) {
        
        log.info("更新算法部署状态：ID={}, Status={}", id, deployStatus);
        
        boolean success = algorithmService.updateDeployStatus(id, deployStatus);
        if (success) {
            return Result.success("部署状态更新成功");
        } else {
            return Result.error("部署状态更新失败");
        }
    }

    /**
     * Batch update deployment status
     */
    @PutMapping("/batch/deploy-status")
    @Operation(summary = "Batch update deployment status", description = "Batch update deployment status of algorithms")
    public Result<String> batchUpdateDeployStatus(
            @RequestBody List<Long> ids,
            @Parameter(description = "Deployment status", example = "deployed") @RequestParam @NotNull String deployStatus) {
        
        log.info("批量更新算法部署状态：IDs={}, Status={}", ids, deployStatus);
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要更新的算法");
        }
        
        boolean success = algorithmService.batchUpdateDeployStatus(ids, deployStatus);
        if (success) {
            return Result.success("部署状态批量更新成功");
        } else {
            return Result.error("部署状态批量更新失败");
        }
    }

    /**
     * Deploy algorithm to devices
     */
    @PostMapping("/{id}/deploy")
    @Operation(summary = "Deploy algorithm to devices", description = "Deploy algorithm to specified devices")
    public Result<String> deployAlgorithmToDevices(
            @Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long algorithmId,
            @RequestBody List<Long> deviceIds) {
        
        log.info("部署算法到设备：AlgorithmId={}, DeviceIds={}", algorithmId, deviceIds);
        
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Result.error("请选择要部署的设备");
        }
        
        boolean success = algorithmService.deployAlgorithmToDevices(algorithmId, deviceIds);
        if (success) {
            return Result.success("算法部署成功");
        } else {
            return Result.error("算法部署失败");
        }
    }

    /**
     * Algorithm evaluation
     */
    @PostMapping("/{algorithmId}/evaluate")
    @Operation(summary = "Algorithm evaluation", description = "Perform performance evaluation on algorithm")
    public Result<Map<String, Object>> evaluateAlgorithm(@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long algorithmId) {
        
        log.info("算法评估：AlgorithmId={}", algorithmId);
        
        Map<String, Object> result = algorithmService.evaluateAlgorithm(algorithmId);
        if (result != null) {
            return Result.success(result);
        } else {
            return Result.error("算法评估失败，算法不存在");
        }
    }

    /**
     * Get algorithm category statistics
     */
    @GetMapping("/statistics/category")
    @Operation(summary = "Get algorithm category statistics", description = "Get algorithm count statistics by category")
    public Result<List<Map<String, Object>>> getCategoryStatistics() {
        log.info("获取算法分类统计");
        
        List<Map<String, Object>> statistics = algorithmService.getCategoryStatistics();
        return Result.success(statistics);
    }

    /**
     * Get algorithm type statistics
     */
    @GetMapping("/statistics/type")
    @Operation(summary = "Get algorithm type statistics", description = "Get algorithm count statistics by type")
    public Result<List<Map<String, Object>>> getTypeStatistics() {
        log.info("获取算法类型统计");
        
        List<Map<String, Object>> statistics = algorithmService.getTypeStatistics();
        return Result.success(statistics);
    }

    /**
     * Get deployment status statistics
     */
    @GetMapping("/statistics/deploy-status")
    @Operation(summary = "Get deployment status statistics", description = "Get algorithm count statistics by deployment status")
    public Result<List<Map<String, Object>>> getDeployStatusStatistics() {
        log.info("获取部署状态统计");
        
        List<Map<String, Object>> statistics = algorithmService.getDeployStatusStatistics();
        return Result.success(statistics);
    }

    /**
     * Count algorithms in a repository
     */
    @GetMapping("/count/repository/{repositoryId}")
    @Operation(summary = "Count algorithms in a repository", description = "Get algorithm count in specified repository")
    public Result<Long> countByRepositoryId(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long repositoryId) {
        
        log.info("统计某仓库下的算法数量：RepositoryId={}", repositoryId);
        
        Long count = algorithmService.countByRepositoryId(repositoryId);
        return Result.success(count);
    }
} 