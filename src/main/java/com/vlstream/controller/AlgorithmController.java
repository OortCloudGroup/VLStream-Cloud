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
 * 算法控制器
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm")
@RequiredArgsConstructor
@Api(tags = "算法管理")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    /**
     * 分页查询算法列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询算法列表", description = "根据条件分页查询算法")
    public Result<IPage<Algorithm>> getAlgorithmPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long repositoryId,
            @Parameter(description = "算法名称") @RequestParam(required = false) String name,
            @Parameter(description = "算法类型") @RequestParam(required = false) String category,
            @Parameter(description = "部署状态") @RequestParam(required = false) String deployStatus) {
        
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
     * 根据仓库ID查询算法列表
     */
    @GetMapping("/repository/{repositoryId}")
    @Operation(summary = "根据仓库ID查询算法列表", description = "获取指定仓库下的所有算法")
    public Result<List<Algorithm>> getAlgorithmsByRepositoryId(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long repositoryId) {
        
        log.info("根据仓库ID查询算法列表：{}", repositoryId);
        
        List<Algorithm> algorithms = algorithmService.getByRepositoryId(repositoryId);
        return Result.success(algorithms);
    }

    /**
     * 根据分类查询算法列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据分类查询算法列表", description = "获取指定分类的所有算法")
    public Result<List<Algorithm>> getAlgorithmsByCategory(
            @Parameter(description = "算法类型", example = "person-detection") @PathVariable String category) {
        
        log.info("根据分类查询算法列表：{}", category);
        
        List<Algorithm> algorithms = algorithmService.getByCategory(category);
        return Result.success(algorithms);
    }

    /**
     * 根据ID查询算法详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询算法详情", description = "根据ID获取算法详细信息")
    public Result<Algorithm> getAlgorithmById(
            @Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("查询算法详情：ID={}", id);
        
        Algorithm algorithm = algorithmService.getById(id);
        if (algorithm == null) {
            return Result.error("算法不存在");
        }
        
        return Result.success(algorithm);
    }

    /**
     * 创建算法
     */
    @PostMapping
    @Operation(summary = "创建算法", description = "新增算法")
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
     * 更新算法
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新算法", description = "根据ID更新算法信息")
    public Result<String> updateAlgorithm(
            @Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id,
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
     * 删除算法
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除算法", description = "根据ID删除算法（软删除）")
    public Result<String> deleteAlgorithm(
            @Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("删除算法：ID={}", id);
        
        boolean success = algorithmService.deleteAlgorithm(id);
        if (success) {
            return Result.success("算法删除成功");
        } else {
            return Result.error("算法删除失败");
        }
    }

    /**
     * 批量删除算法
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除算法", description = "根据ID列表批量删除算法")
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
     * 更新部署状态
     */
    @PutMapping("/{id}/deploy-status")
    @Operation(summary = "更新部署状态", description = "更新算法的部署状态")
    public Result<String> updateDeployStatus(
            @Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "部署状态", example = "deployed") @RequestParam @NotNull String deployStatus) {
        
        log.info("更新算法部署状态：ID={}, Status={}", id, deployStatus);
        
        boolean success = algorithmService.updateDeployStatus(id, deployStatus);
        if (success) {
            return Result.success("部署状态更新成功");
        } else {
            return Result.error("部署状态更新失败");
        }
    }

    /**
     * 批量更新部署状态
     */
    @PutMapping("/batch/deploy-status")
    @Operation(summary = "批量更新部署状态", description = "批量更新算法的部署状态")
    public Result<String> batchUpdateDeployStatus(
            @RequestBody List<Long> ids,
            @Parameter(description = "部署状态", example = "deployed") @RequestParam @NotNull String deployStatus) {
        
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
     * 部署算法到设备
     */
    @PostMapping("/{id}/deploy")
    @Operation(summary = "部署算法到设备", description = "将算法部署到指定设备")
    public Result<String> deployAlgorithmToDevices(
            @Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long algorithmId,
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
     * 算法评估
     */
    @PostMapping("/{algorithmId}/evaluate")
    @Operation(summary = "算法评估", description = "对算法进行性能评估")
    public Result<Map<String, Object>> evaluateAlgorithm(@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long algorithmId) {
        
        log.info("算法评估：AlgorithmId={}", algorithmId);
        
        Map<String, Object> result = algorithmService.evaluateAlgorithm(algorithmId);
        if (result != null) {
            return Result.success(result);
        } else {
            return Result.error("算法评估失败，算法不存在");
        }
    }

    /**
     * 获取算法分类统计
     */
    @GetMapping("/statistics/category")
    @Operation(summary = "获取算法分类统计", description = "获取各分类的算法数量统计")
    public Result<List<Map<String, Object>>> getCategoryStatistics() {
        log.info("获取算法分类统计");
        
        List<Map<String, Object>> statistics = algorithmService.getCategoryStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取算法类型统计
     */
    @GetMapping("/statistics/type")
    @Operation(summary = "获取算法类型统计", description = "获取各类型的算法数量统计")
    public Result<List<Map<String, Object>>> getTypeStatistics() {
        log.info("获取算法类型统计");
        
        List<Map<String, Object>> statistics = algorithmService.getTypeStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取部署状态统计
     */
    @GetMapping("/statistics/deploy-status")
    @Operation(summary = "获取部署状态统计", description = "获取各部署状态的算法数量统计")
    public Result<List<Map<String, Object>>> getDeployStatusStatistics() {
        log.info("获取部署状态统计");
        
        List<Map<String, Object>> statistics = algorithmService.getDeployStatusStatistics();
        return Result.success(statistics);
    }

    /**
     * 统计某仓库下的算法数量
     */
    @GetMapping("/count/repository/{repositoryId}")
    @Operation(summary = "统计某仓库下的算法数量", description = "获取指定仓库的算法数量")
    public Result<Long> countByRepositoryId(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long repositoryId) {
        
        log.info("统计某仓库下的算法数量：RepositoryId={}", repositoryId);
        
        Long count = algorithmService.countByRepositoryId(repositoryId);
        return Result.success(count);
    }
} 