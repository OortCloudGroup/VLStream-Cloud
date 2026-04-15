package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.AlgorithmRepository;
import com.vlstream.service.AlgorithmRepositoryService;
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

/**
 * 算法仓库控制器
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm-repository")
@RequiredArgsConstructor
@Api(tags = "算法仓库管理")
public class AlgorithmRepositoryController {

    private final AlgorithmRepositoryService algorithmRepositoryService;

    /**
     * 分页查询算法仓库列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询算法仓库列表", description = "根据条件分页查询算法仓库")
    public Result<IPage<AlgorithmRepository>> getRepositoryPage(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "仓库名称") @RequestParam(required = false) String name,
            @Parameter(description = "仓库类型") @RequestParam(required = false) String repositoryType,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        
        log.info("分页查询算法仓库，参数：current={}, size={}, name={}, repositoryType={}, status={}", 
                current, size, name, repositoryType, status);
        
        Page<AlgorithmRepository> page = new Page<>(current, size);
        IPage<AlgorithmRepository> result = algorithmRepositoryService.selectRepositoryPage(page, name, repositoryType, status);
        
        return Result.success(result);
    }

    /**
     * 查询所有启用的算法仓库
     */
    @GetMapping("/enabled")
    @Operation(summary = "查询所有启用的算法仓库", description = "获取状态为启用的所有算法仓库")
    public Result<List<AlgorithmRepository>> getEnabledRepositories() {
        log.info("查询所有启用的算法仓库");
        
        List<AlgorithmRepository> repositories = algorithmRepositoryService.getEnabledRepositories();
        return Result.success(repositories);
    }

    /**
     * 根据类型查询算法仓库
     */
    @GetMapping("/type/{repositoryType}")
    @Operation(summary = "根据类型查询算法仓库", description = "根据仓库类型获取算法仓库列表")
    public Result<List<AlgorithmRepository>> getRepositoriesByType(
            @Parameter(description = "仓库类型", example = "extended") @PathVariable String repositoryType) {
        
        log.info("根据类型查询算法仓库：{}", repositoryType);
        
        List<AlgorithmRepository> repositories = algorithmRepositoryService.getByRepositoryType(repositoryType);
        return Result.success(repositories);
    }

    /**
     * 根据ID查询算法仓库详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询算法仓库详情", description = "根据ID获取算法仓库详细信息")
    public Result<AlgorithmRepository> getRepositoryById(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("查询算法仓库详情：ID={}", id);
        
        AlgorithmRepository repository = algorithmRepositoryService.getById(id);
        if (repository == null) {
            return Result.error("算法仓库不存在");
        }
        
        return Result.success(repository);
    }

    /**
     * 创建算法仓库
     */
    @PostMapping
    @Operation(summary = "创建算法仓库", description = "新增算法仓库")
    public Result<String> createRepository(@Valid @RequestBody AlgorithmRepository repository) {
        log.info("创建算法仓库：{}", repository.getName());
        
        boolean success = algorithmRepositoryService.createRepository(repository);
        if (success) {
            return Result.success("算法仓库创建成功");
        } else {
            return Result.error("算法仓库创建失败，名称可能已存在");
        }
    }

    /**
     * 更新算法仓库
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新算法仓库", description = "根据ID更新算法仓库信息")
    public Result<String> updateRepository(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id,
            @Valid @RequestBody AlgorithmRepository repository) {
        
        log.info("更新算法仓库：ID={}", id);
        
        repository.setId(id);
        boolean success = algorithmRepositoryService.updateRepository(repository);
        
        if (success) {
            return Result.success("算法仓库更新成功");
        } else {
            return Result.error("算法仓库更新失败");
        }
    }

    /**
     * 删除算法仓库
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除算法仓库", description = "根据ID删除算法仓库（软删除）")
    public Result<String> deleteRepository(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("删除算法仓库：ID={}", id);
        
        boolean success = algorithmRepositoryService.deleteRepository(id);
        if (success) {
            return Result.success("算法仓库删除成功");
        } else {
            return Result.error("算法仓库删除失败，基础预置算法库不允许删除");
        }
    }

    /**
     * 批量删除算法仓库
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除算法仓库", description = "根据ID列表批量删除算法仓库")
    public Result<String> batchDeleteRepositories(@RequestBody List<Long> ids) {
        log.info("批量删除算法仓库：IDs={}", ids);
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的算法仓库");
        }
        
        boolean success = algorithmRepositoryService.batchDeleteRepositories(ids);
        if (success) {
            return Result.success("算法仓库批量删除成功");
        } else {
            return Result.error("算法仓库批量删除失败，部分仓库不允许删除");
        }
    }

    /**
     * 更新仓库状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新仓库状态", description = "启用或禁用算法仓库")
    public Result<String> updateRepositoryStatus(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "新状态", example = "enabled") @RequestParam @NotNull String status) {
        
        log.info("更新算法仓库状态：ID={}, Status={}", id, status);
        
        boolean success = algorithmRepositoryService.updateRepositoryStatus(id, status);
        if (success) {
            return Result.success("算法仓库状态更新成功");
        } else {
            return Result.error("算法仓库状态更新失败");
        }
    }

    /**
     * 批量更新仓库状态
     */
    @PutMapping("/batch/status")
    @Operation(summary = "批量更新仓库状态", description = "批量启用或禁用算法仓库")
    public Result<String> batchUpdateRepositoryStatus(
            @RequestBody List<Long> ids,
            @Parameter(description = "新状态", example = "enabled") @RequestParam @NotNull String status) {
        
        log.info("批量更新算法仓库状态：IDs={}, Status={}", ids, status);
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要更新的算法仓库");
        }
        
        boolean success = algorithmRepositoryService.batchUpdateRepositoryStatus(ids, status);
        if (success) {
            return Result.success("算法仓库状态批量更新成功");
        } else {
            return Result.error("算法仓库状态批量更新失败");
        }
    }

    /**
     * 统计算法仓库数量
     */
    @GetMapping("/count")
    @Operation(summary = "统计算法仓库数量", description = "获取算法仓库总数")
    public Result<Long> countRepositories() {
        log.info("统计算法仓库数量");
        
        Long count = algorithmRepositoryService.countRepositories();
        return Result.success(count);
    }

    /**
     * 刷新仓库算法数量
     */
    @PutMapping("/{id}/refresh-count")
    @Operation(summary = "刷新仓库算法数量", description = "重新计算并更新仓库的算法数量")
    public Result<String> refreshAlgorithmCount(
            @Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("刷新算法仓库算法数量：ID={}", id);
        
        algorithmRepositoryService.updateAlgorithmCount(id);
        return Result.success("算法数量刷新成功");
    }
} 