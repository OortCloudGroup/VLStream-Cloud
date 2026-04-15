package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.SceneGovernance;
import com.vlstream.service.SceneGovernanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 场景治理控制器
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Api(tags = "场景治理")
@RestController
@RequestMapping("/api/scene-governance")
@RequiredArgsConstructor
public class SceneGovernanceController {

    private final SceneGovernanceService sceneGovernanceService;

    /**
     * 分页查询场景治理信息
     */
    @ApiOperation("分页查询场景治理信息")
    @GetMapping("/page")
    public Result<IPage<SceneGovernance>> pageSceneGovernances(
            @ApiParam("当前页") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("场景名称") @RequestParam(required = false) String name,
            @ApiParam("场景状态") @RequestParam(required = false) String status,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate) {
        
        Page<SceneGovernance> page = new Page<>(current, size);
        IPage<SceneGovernance> result = sceneGovernanceService.getSceneGovernancePage(page, name, status, startDate, endDate);
        return Result.success(result);
    }

    /**
     * 根据ID查询场景治理信息
     */
    @ApiOperation("根据ID查询场景治理信息")
    @GetMapping("/{id}")
    public Result<SceneGovernance> getSceneGovernanceById(@ApiParam("场景ID") @PathVariable Long id) {
        SceneGovernance sceneGovernance = sceneGovernanceService.getById(id);
        if (sceneGovernance == null) {
            return Result.error("场景不存在");
        }
        return Result.success(sceneGovernance);
    }

    /**
     * 根据名称查询场景治理信息
     */
    @ApiOperation("根据名称查询场景治理信息")
    @GetMapping("/name/{name}")
    public Result<SceneGovernance> getSceneGovernanceByName(@ApiParam("场景名称") @PathVariable String name) {
        SceneGovernance sceneGovernance = sceneGovernanceService.getByName(name);
        if (sceneGovernance == null) {
            return Result.error("场景不存在");
        }
        return Result.success(sceneGovernance);
    }

    /**
     * 新增场景治理信息
     */
    @ApiOperation("新增场景治理信息")
    @PostMapping
    public Result<String> addSceneGovernance(@Valid @RequestBody SceneGovernance sceneGovernance) {
        // 验证场景治理配置
        Map<String, Object> validateResult = sceneGovernanceService.validateSceneGovernance(sceneGovernance);
        if (!(Boolean) validateResult.get("valid")) {
            return Result.error("数据验证失败: " + validateResult.get("errors"));
        }
        
        boolean success = sceneGovernanceService.addSceneGovernance(sceneGovernance);
        if (success) {
            return Result.success("新增成功");
        } else {
            return Result.error("新增失败");
        }
    }

    /**
     * 更新场景治理信息
     */
    @ApiOperation("更新场景治理信息")
    @PutMapping("/{id}")
    public Result<String> updateSceneGovernance(
            @ApiParam("场景ID") @PathVariable Long id,
            @RequestBody SceneGovernance sceneGovernance) {
        
        sceneGovernance.setId(id);
        
        // 验证场景治理配置
        Map<String, Object> validateResult = sceneGovernanceService.validateSceneGovernance(sceneGovernance);
        if (!(Boolean) validateResult.get("valid")) {
            return Result.error("数据验证失败: " + validateResult.get("errors"));
        }
        
        boolean success = sceneGovernanceService.updateSceneGovernance(sceneGovernance);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 删除场景治理信息
     */
    @ApiOperation("删除场景治理信息")
    @DeleteMapping("/{id}")
    public Result<String> deleteSceneGovernance(@ApiParam("场景ID") @PathVariable Long id) {
        boolean success = sceneGovernanceService.deleteSceneGovernance(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 批量删除场景治理信息
     */
    @ApiOperation("批量删除场景治理信息")
    @DeleteMapping("/batch")
    public Result<String> deleteSceneGovernanceBatch(@ApiParam("场景ID列表") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的场景");
        }
        
        boolean success = sceneGovernanceService.deleteSceneGovernanceBatch(ids);
        if (success) {
            return Result.success("批量删除成功");
        } else {
            return Result.error("批量删除失败");
        }
    }

    /**
     * 更新场景治理状态
     */
    @ApiOperation("更新场景治理状态")
    @PutMapping("/{id}/status/{status}")
    public Result<String> updateSceneGovernanceStatus(
            @ApiParam("场景ID") @PathVariable Long id,
            @ApiParam("场景状态") @PathVariable String status) {
        
        if (!"enabled".equals(status) && !"disabled".equals(status)) {
            return Result.error("状态值无效");
        }
        
        boolean success = sceneGovernanceService.updateSceneGovernanceStatus(id, status);
        if (success) {
            return Result.success("状态更新成功");
        } else {
            return Result.error("状态更新失败");
        }
    }

    /**
     * 批量更新场景治理状态
     */
    @ApiOperation("批量更新场景治理状态")
    @PutMapping("/status/{status}")
    public Result<String> updateSceneGovernanceStatusBatch(
            @ApiParam("场景状态") @PathVariable String status,
            @ApiParam("场景ID列表") @RequestBody List<Long> ids) {
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要更新状态的场景");
        }
        
        if (!"enabled".equals(status) && !"disabled".equals(status)) {
            return Result.error("状态值无效");
        }
        
        boolean success = sceneGovernanceService.updateSceneGovernanceStatusBatch(ids, status);
        if (success) {
            return Result.success("批量状态更新成功");
        } else {
            return Result.error("批量状态更新失败");
        }
    }

    /**
     * 启用场景治理
     */
    @ApiOperation("启用场景治理")
    @PutMapping("/{id}/enable")
    public Result<String> enableSceneGovernance(@ApiParam("场景ID") @PathVariable Long id) {
        boolean success = sceneGovernanceService.enableSceneGovernance(id);
        if (success) {
            return Result.success("启用成功");
        } else {
            return Result.error("启用失败");
        }
    }

    /**
     * 禁用场景治理
     */
    @ApiOperation("禁用场景治理")
    @PutMapping("/{id}/disable")
    public Result<String> disableSceneGovernance(@ApiParam("场景ID") @PathVariable Long id) {
        boolean success = sceneGovernanceService.disableSceneGovernance(id);
        if (success) {
            return Result.success("禁用成功");
        } else {
            return Result.error("禁用失败");
        }
    }

    /**
     * 批量启用场景治理
     */
    @ApiOperation("批量启用场景治理")
    @PutMapping("/batch/enable")
    public Result<String> enableSceneGovernanceBatch(@ApiParam("场景ID列表") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要启用的场景");
        }
        
        boolean success = sceneGovernanceService.enableSceneGovernanceBatch(ids);
        if (success) {
            return Result.success("批量启用成功");
        } else {
            return Result.error("批量启用失败");
        }
    }

    /**
     * 批量禁用场景治理
     */
    @ApiOperation("批量禁用场景治理")
    @PutMapping("/batch/disable")
    public Result<String> disableSceneGovernanceBatch(@ApiParam("场景ID列表") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要禁用的场景");
        }
        
        boolean success = sceneGovernanceService.disableSceneGovernanceBatch(ids);
        if (success) {
            return Result.success("批量禁用成功");
        } else {
            return Result.error("批量禁用失败");
        }
    }

    /**
     * 根据状态查询场景治理列表
     */
    @ApiOperation("根据状态查询场景治理列表")
    @GetMapping("/status/{status}")
    public Result<List<SceneGovernance>> getSceneGovernancesByStatus(@ApiParam("场景状态") @PathVariable String status) {
        List<SceneGovernance> sceneGovernances = sceneGovernanceService.getSceneGovernancesByStatus(status);
        return Result.success(sceneGovernances);
    }

    /**
     * 根据执行类型查询场景治理列表
     */
    @ApiOperation("根据执行类型查询场景治理列表")
    @GetMapping("/execute-type/{executeType}")
    public Result<List<SceneGovernance>> getSceneGovernancesByExecuteType(@ApiParam("执行类型") @PathVariable String executeType) {
        List<SceneGovernance> sceneGovernances = sceneGovernanceService.getSceneGovernancesByExecuteType(executeType);
        return Result.success(sceneGovernances);
    }

    /**
     * 获取场景治理统计信息
     */
    @ApiOperation("获取场景治理统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getSceneGovernanceStatistics() {
        Map<String, Object> statistics = sceneGovernanceService.getSceneGovernanceStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取所有执行类型列表
     */
    @ApiOperation("获取所有执行类型列表")
    @GetMapping("/execute-types")
    public Result<List<String>> getAllExecuteTypes() {
        List<String> executeTypes = sceneGovernanceService.getAllExecuteTypes();
        return Result.success(executeTypes);
    }

    /**
     * 执行场景治理
     */
    @ApiOperation("执行场景治理")
    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> executeSceneGovernance(@ApiParam("场景ID") @PathVariable Long id) {
        Map<String, Object> result = sceneGovernanceService.executeSceneGovernance(id);
        if ((Boolean) result.get("success")) {
            return Result.success("执行成功", result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 验证场景治理配置
     */
    @ApiOperation("验证场景治理配置")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateSceneGovernance(@RequestBody SceneGovernance sceneGovernance) {
        Map<String, Object> result = sceneGovernanceService.validateSceneGovernance(sceneGovernance);
        return Result.success(result);
    }

    /**
     * 导出场景治理信息
     */
    @ApiOperation("导出场景治理信息")
    @GetMapping("/export")
    public Result<List<SceneGovernance>> exportSceneGovernances(@RequestParam(required = false) List<Long> sceneIds) {
        List<SceneGovernance> sceneGovernances = sceneGovernanceService.exportSceneGovernances(sceneIds);
        return Result.success(sceneGovernances);
    }

    /**
     * 批量导入场景治理
     */
    @ApiOperation("批量导入场景治理")
    @PostMapping("/import")
    public Result<Map<String, Object>> batchImportSceneGovernances(@RequestBody List<SceneGovernance> sceneGovernanceList) {
        if (sceneGovernanceList == null || sceneGovernanceList.isEmpty()) {
            return Result.error("导入数据不能为空");
        }
        
        Map<String, Object> result = sceneGovernanceService.batchImportSceneGovernances(sceneGovernanceList);
        return Result.success("导入完成", result);
    }

    /**
     * 获取场景治理执行历史
     */
    @ApiOperation("获取场景治理执行历史")
    @GetMapping("/{id}/execute-history")
    public Result<List<Map<String, Object>>> getSceneGovernanceExecuteHistory(@ApiParam("场景ID") @PathVariable Long id) {
        List<Map<String, Object>> history = sceneGovernanceService.getSceneGovernanceExecuteHistory(id);
        return Result.success(history);
    }

    /**
     * 复制场景治理
     */
    @ApiOperation("复制场景治理")
    @PostMapping("/{id}/copy")
    public Result<String> copySceneGovernance(
            @ApiParam("源场景ID") @PathVariable Long id,
            @ApiParam("新场景名称") @RequestParam String name) {
        
        if (name == null || name.trim().isEmpty()) {
            return Result.error("新场景名称不能为空");
        }
        
        boolean success = sceneGovernanceService.copySceneGovernance(id, name.trim());
        if (success) {
            return Result.success("复制成功");
        } else {
            return Result.error("复制失败");
        }
    }

    /**
     * 检查场景名称是否存在
     */
    @ApiOperation("检查场景名称是否存在")
    @GetMapping("/check-name")
    public Result<Boolean> checkSceneNameExists(
            @ApiParam("场景名称") @RequestParam String name,
            @ApiParam("场景ID（编辑时排除自己）") @RequestParam(required = false) Long id) {
        
        boolean exists = sceneGovernanceService.checkSceneNameExists(name, id);
        return Result.success(exists);
    }
} 