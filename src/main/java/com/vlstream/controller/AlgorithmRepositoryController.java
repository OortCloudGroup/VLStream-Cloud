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
 * Algorithm Repository Controller
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm-repository")
@RequiredArgsConstructor
@Api(tags = "Algorithm Repository Management")
public class AlgorithmRepositoryController {

    private final AlgorithmRepositoryService algorithmRepositoryService;

    /**
     * Page query algorithm repository list
     */
    @GetMapping("/page")
    @Operation(summary = "Query algorithm repository list with pagination", description = "Query algorithm repositories with pagination based on conditions")
    public Result<IPage<AlgorithmRepository>> getRepositoryPage(
            @Parameter(description = "Current page", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Repository name") @RequestParam(required = false) String name,
            @Parameter(description = "Repository type") @RequestParam(required = false) String repositoryType,
            @Parameter(description = "Status") @RequestParam(required = false) String status) {
        
        log.info("分页查询算法仓库，参数：current={}, size={}, name={}, repositoryType={}, status={}", 
                current, size, name, repositoryType, status);
        
        Page<AlgorithmRepository> page = new Page<>(current, size);
        IPage<AlgorithmRepository> result = algorithmRepositoryService.selectRepositoryPage(page, name, repositoryType, status);
        
        return Result.success(result);
    }

    /**
     * Query all enabled algorithm repositories
     */
    @GetMapping("/enabled")
    @Operation(summary = "Query all enabled algorithm repositories", description = "Get all algorithm repositories with enabled status")
    public Result<List<AlgorithmRepository>> getEnabledRepositories() {
        log.info("查询所有启用的算法仓库");
        
        List<AlgorithmRepository> repositories = algorithmRepositoryService.getEnabledRepositories();
        return Result.success(repositories);
    }

    /**
     * Query algorithm repositories by type
     */
    @GetMapping("/type/{repositoryType}")
    @Operation(summary = "Query algorithm repositories by type", description = "Get algorithm repository list by repository type")
    public Result<List<AlgorithmRepository>> getRepositoriesByType(
            @Parameter(description = "Repository type", example = "extended") @PathVariable String repositoryType) {
        
        log.info("根据类型查询算法仓库：{}", repositoryType);
        
        List<AlgorithmRepository> repositories = algorithmRepositoryService.getByRepositoryType(repositoryType);
        return Result.success(repositories);
    }

    /**
     * Query algorithm repository details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Query algorithm repository details", description = "Get algorithm repository details by ID")
    public Result<AlgorithmRepository> getRepositoryById(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("查询算法仓库详情：ID={}", id);
        
        AlgorithmRepository repository = algorithmRepositoryService.getById(id);
        if (repository == null) {
            return Result.error("算法仓库不存在");
        }
        
        return Result.success(repository);
    }

    /**
     * Create algorithm repository
     */
    @PostMapping
    @Operation(summary = "Create algorithm repository", description = "Add new algorithm repository")
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
     * Update algorithm repository
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update algorithm repository", description = "Update algorithm repository information by ID")
    public Result<String> updateRepository(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id,
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
     * Delete algorithm repository
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete algorithm repository", description = "Delete algorithm repository by ID (soft delete)")
    public Result<String> deleteRepository(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("删除算法仓库：ID={}", id);
        
        boolean success = algorithmRepositoryService.deleteRepository(id);
        if (success) {
            return Result.success("算法仓库删除成功");
        } else {
            return Result.error("算法仓库删除失败，基础预置算法库不允许删除");
        }
    }

    /**
     * Batch delete algorithm repositories
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Batch delete algorithm repositories", description = "Batch delete algorithm repositories by ID list")
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
     * Update repository status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update repository status", description = "Enable or disable algorithm repository")
    public Result<String> updateRepositoryStatus(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "New status", example = "enabled") @RequestParam @NotNull String status) {
        
        log.info("更新算法仓库状态：ID={}, Status={}", id, status);
        
        boolean success = algorithmRepositoryService.updateRepositoryStatus(id, status);
        if (success) {
            return Result.success("算法仓库状态更新成功");
        } else {
            return Result.error("算法仓库状态更新失败");
        }
    }

    /**
     * Batch update repository status
     */
    @PutMapping("/batch/status")
    @Operation(summary = "Batch update repository status", description = "Batch enable or disable algorithm repositories")
    public Result<String> batchUpdateRepositoryStatus(
            @RequestBody List<Long> ids,
            @Parameter(description = "New status", example = "enabled") @RequestParam @NotNull String status) {
        
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
     * Count algorithm repositories
     */
    @GetMapping("/count")
    @Operation(summary = "Count algorithm repositories", description = "Get total algorithm repository count")
    public Result<Long> countRepositories() {
        log.info("统计算法仓库数量");
        
        Long count = algorithmRepositoryService.countRepositories();
        return Result.success(count);
    }

    /**
     * Refresh repository algorithm count
     */
    @PutMapping("/{id}/refresh-count")
    @Operation(summary = "Refresh repository algorithm count", description = "Recalculate and update repository algorithm count")
    public Result<String> refreshAlgorithmCount(
            @Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id) {
        
        log.info("刷新算法仓库算法数量：ID={}", id);
        
        algorithmRepositoryService.updateAlgorithmCount(id);
        return Result.success("算法数量刷新成功");
    }
} 