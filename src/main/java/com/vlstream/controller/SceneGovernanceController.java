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
 * Scene Governance Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Api(tags = "Scene Governance")
@RestController
@RequestMapping("/api/scene-governance")
@RequiredArgsConstructor
public class SceneGovernanceController {

    private final SceneGovernanceService sceneGovernanceService;

    /**
     * Page query scene governance information
     */
    @ApiOperation("Page query scene governance information")
    @GetMapping("/page")
    public Result<IPage<SceneGovernance>> pageSceneGovernances(
            @ApiParam("Current page") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("Scene name") @RequestParam(required = false) String name,
            @ApiParam("Scene status") @RequestParam(required = false) String status,
            @ApiParam("Start date") @RequestParam(required = false) String startDate,
            @ApiParam("End date") @RequestParam(required = false) String endDate) {
        
        Page<SceneGovernance> page = new Page<>(current, size);
        IPage<SceneGovernance> result = sceneGovernanceService.getSceneGovernancePage(page, name, status, startDate, endDate);
        return Result.success(result);
    }

    /**
     * Query scene governance information by ID
     */
    @ApiOperation("Query scene governance information by ID")
    @GetMapping("/{id}")
    public Result<SceneGovernance> getSceneGovernanceById(@ApiParam("Scene ID") @PathVariable Long id) {
        SceneGovernance sceneGovernance = sceneGovernanceService.getById(id);
        if (sceneGovernance == null) {
            return Result.error("Scene does not exist");
        }
        return Result.success(sceneGovernance);
    }

    /**
     * Query scene governance information by name
     */
    @ApiOperation("Query scene governance information by name")
    @GetMapping("/name/{name}")
    public Result<SceneGovernance> getSceneGovernanceByName(@ApiParam("Scene name") @PathVariable String name) {
        SceneGovernance sceneGovernance = sceneGovernanceService.getByName(name);
        if (sceneGovernance == null) {
            return Result.error("Scene does not exist");
        }
        return Result.success(sceneGovernance);
    }

    /**
     * Add scene governance information
     */
    @ApiOperation("Add scene governance information")
    @PostMapping
    public Result<String> addSceneGovernance(@Valid @RequestBody SceneGovernance sceneGovernance) {
        // Validate scene governance configuration
        Map<String, Object> validateResult = sceneGovernanceService.validateSceneGovernance(sceneGovernance);
        if (!(Boolean) validateResult.get("valid")) {
            return Result.error("Data validation failed: " + validateResult.get("errors"));
        }
        
        boolean success = sceneGovernanceService.addSceneGovernance(sceneGovernance);
        if (success) {
            return Result.success("Add successful");
        } else {
            return Result.error("Add failed");
        }
    }

    /**
     * Update scene governance information
     */
    @ApiOperation("Update scene governance information")
    @PutMapping("/{id}")
    public Result<String> updateSceneGovernance(
            @ApiParam("Scene ID") @PathVariable Long id,
            @RequestBody SceneGovernance sceneGovernance) {
        
        sceneGovernance.setId(id);
        
        // Validate scene governance configuration
        Map<String, Object> validateResult = sceneGovernanceService.validateSceneGovernance(sceneGovernance);
        if (!(Boolean) validateResult.get("valid")) {
            return Result.error("Data validation failed: " + validateResult.get("errors"));
        }
        
        boolean success = sceneGovernanceService.updateSceneGovernance(sceneGovernance);
        if (success) {
            return Result.success("Update successful");
        } else {
            return Result.error("Update failed");
        }
    }

    /**
     * Delete scene governance information
     */
    @ApiOperation("Delete scene governance information")
    @DeleteMapping("/{id}")
    public Result<String> deleteSceneGovernance(@ApiParam("Scene ID") @PathVariable Long id) {
        boolean success = sceneGovernanceService.deleteSceneGovernance(id);
        if (success) {
            return Result.success("Delete successful");
        } else {
            return Result.error("Delete failed");
        }
    }

    /**
     * Batch delete scene governance information
     */
    @ApiOperation("Batch delete scene governance information")
    @DeleteMapping("/batch")
    public Result<String> deleteSceneGovernanceBatch(@ApiParam("Scene ID list") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("Please select scenes to delete");
        }
        
        boolean success = sceneGovernanceService.deleteSceneGovernanceBatch(ids);
        if (success) {
            return Result.success("Batch delete successful");
        } else {
            return Result.error("Batch delete failed");
        }
    }

    /**
     * Update scene governance status
     */
    @ApiOperation("Update scene governance status")
    @PutMapping("/{id}/status/{status}")
    public Result<String> updateSceneGovernanceStatus(
            @ApiParam("Scene ID") @PathVariable Long id,
            @ApiParam("Scene status") @PathVariable String status) {
        
        if (!"enabled".equals(status) && !"disabled".equals(status)) {
            return Result.error("Invalid status value");
        }
        
        boolean success = sceneGovernanceService.updateSceneGovernanceStatus(id, status);
        if (success) {
            return Result.success("Status update successful");
        } else {
            return Result.error("Status update failed");
        }
    }

    /**
     * Batch update scene governance status
     */
    @ApiOperation("Batch update scene governance status")
    @PutMapping("/status/{status}")
    public Result<String> updateSceneGovernanceStatusBatch(
            @ApiParam("Scene status") @PathVariable String status,
            @ApiParam("Scene ID list") @RequestBody List<Long> ids) {
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("Please select scenes to update status");
        }
        
        if (!"enabled".equals(status) && !"disabled".equals(status)) {
            return Result.error("Invalid status value");
        }
        
        boolean success = sceneGovernanceService.updateSceneGovernanceStatusBatch(ids, status);
        if (success) {
            return Result.success("Batch status update successful");
        } else {
            return Result.error("Batch status update failed");
        }
    }

    /**
     * Enable scene governance
     */
    @ApiOperation("Enable scene governance")
    @PutMapping("/{id}/enable")
    public Result<String> enableSceneGovernance(@ApiParam("Scene ID") @PathVariable Long id) {
        boolean success = sceneGovernanceService.enableSceneGovernance(id);
        if (success) {
            return Result.success("Enable successful");
        } else {
            return Result.error("Enable failed");
        }
    }

    /**
     * Disable scene governance
     */
    @ApiOperation("Disable scene governance")
    @PutMapping("/{id}/disable")
    public Result<String> disableSceneGovernance(@ApiParam("Scene ID") @PathVariable Long id) {
        boolean success = sceneGovernanceService.disableSceneGovernance(id);
        if (success) {
            return Result.success("Disable successful");
        } else {
            return Result.error("Disable failed");
        }
    }

    /**
     * Batch enable scene governance
     */
    @ApiOperation("Batch enable scene governance")
    @PutMapping("/batch/enable")
    public Result<String> enableSceneGovernanceBatch(@ApiParam("Scene ID list") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("Please select scenes to enable");
        }
        
        boolean success = sceneGovernanceService.enableSceneGovernanceBatch(ids);
        if (success) {
            return Result.success("Batch enable successful");
        } else {
            return Result.error("Batch enable failed");
        }
    }

    /**
     * Batch disable scene governance
     */
    @ApiOperation("Batch disable scene governance")
    @PutMapping("/batch/disable")
    public Result<String> disableSceneGovernanceBatch(@ApiParam("Scene ID list") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("Please select scenes to disable");
        }
        
        boolean success = sceneGovernanceService.disableSceneGovernanceBatch(ids);
        if (success) {
            return Result.success("Batch disable successful");
        } else {
            return Result.error("Batch disable failed");
        }
    }

    /**
     * Query scene governance list by status
     */
    @ApiOperation("Query scene governance list by status")
    @GetMapping("/status/{status}")
    public Result<List<SceneGovernance>> getSceneGovernancesByStatus(@ApiParam("Scene status") @PathVariable String status) {
        List<SceneGovernance> sceneGovernances = sceneGovernanceService.getSceneGovernancesByStatus(status);
        return Result.success(sceneGovernances);
    }

    /**
     * Query scene governance list by execution type
     */
    @ApiOperation("Query scene governance list by execution type")
    @GetMapping("/execute-type/{executeType}")
    public Result<List<SceneGovernance>> getSceneGovernancesByExecuteType(@ApiParam("Execution type") @PathVariable String executeType) {
        List<SceneGovernance> sceneGovernances = sceneGovernanceService.getSceneGovernancesByExecuteType(executeType);
        return Result.success(sceneGovernances);
    }

    /**
     * Get scene governance statistics
     */
    @ApiOperation("Get scene governance statistics")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getSceneGovernanceStatistics() {
        Map<String, Object> statistics = sceneGovernanceService.getSceneGovernanceStatistics();
        return Result.success(statistics);
    }

    /**
     * Get all execution type list
     */
    @ApiOperation("Get all execution type list")
    @GetMapping("/execute-types")
    public Result<List<String>> getAllExecuteTypes() {
        List<String> executeTypes = sceneGovernanceService.getAllExecuteTypes();
        return Result.success(executeTypes);
    }

    /**
     * Execute scene governance
     */
    @ApiOperation("Execute scene governance")
    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> executeSceneGovernance(@ApiParam("Scene ID") @PathVariable Long id) {
        Map<String, Object> result = sceneGovernanceService.executeSceneGovernance(id);
        if ((Boolean) result.get("success")) {
            return Result.success("Execution successful", result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * Validate scene governance configuration
     */
    @ApiOperation("Validate scene governance configuration")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateSceneGovernance(@RequestBody SceneGovernance sceneGovernance) {
        Map<String, Object> result = sceneGovernanceService.validateSceneGovernance(sceneGovernance);
        return Result.success(result);
    }

    /**
     * Export scene governance information
     */
    @ApiOperation("Export scene governance information")
    @GetMapping("/export")
    public Result<List<SceneGovernance>> exportSceneGovernances(@RequestParam(required = false) List<Long> sceneIds) {
        List<SceneGovernance> sceneGovernances = sceneGovernanceService.exportSceneGovernances(sceneIds);
        return Result.success(sceneGovernances);
    }

    /**
     * Batch import scene governance
     */
    @ApiOperation("Batch import scene governance")
    @PostMapping("/import")
    public Result<Map<String, Object>> batchImportSceneGovernances(@RequestBody List<SceneGovernance> sceneGovernanceList) {
        if (sceneGovernanceList == null || sceneGovernanceList.isEmpty()) {
            return Result.error("Import data cannot be empty");
        }
        
        Map<String, Object> result = sceneGovernanceService.batchImportSceneGovernances(sceneGovernanceList);
        return Result.success("Import completed", result);
    }

    /**
     * Get scene governance execution history
     */
    @ApiOperation("Get scene governance execution history")
    @GetMapping("/{id}/execute-history")
    public Result<List<Map<String, Object>>> getSceneGovernanceExecuteHistory(@ApiParam("Scene ID") @PathVariable Long id) {
        List<Map<String, Object>> history = sceneGovernanceService.getSceneGovernanceExecuteHistory(id);
        return Result.success(history);
    }

    /**
     * Copy scene governance
     */
    @ApiOperation("Copy scene governance")
    @PostMapping("/{id}/copy")
    public Result<String> copySceneGovernance(
            @ApiParam("Source scene ID") @PathVariable Long id,
            @ApiParam("New scene name") @RequestParam String name) {
        
        if (name == null || name.trim().isEmpty()) {
            return Result.error("New scene name cannot be empty");
        }
        
        boolean success = sceneGovernanceService.copySceneGovernance(id, name.trim());
        if (success) {
            return Result.success("Copy successful");
        } else {
            return Result.error("Copy failed");
        }
    }

    /**
     * Check if scene name exists
     */
    @ApiOperation("Check if scene name exists")
    @GetMapping("/check-name")
    public Result<Boolean> checkSceneNameExists(
            @ApiParam("Scene name") @RequestParam String name,
            @ApiParam("Scene ID (exclude self when editing)") @RequestParam(required = false) Long id) {
        
        boolean exists = sceneGovernanceService.checkSceneNameExists(name, id);
        return Result.success(exists);
    }
} 