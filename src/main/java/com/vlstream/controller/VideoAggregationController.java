package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.VideoAggregation;
import com.vlstream.service.VideoAggregationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Video Aggregation Configuration Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/video-aggregation")
@Api(tags = "Video Aggregation Management")
public class VideoAggregationController {

    @Autowired
    private VideoAggregationService videoAggregationService;

    @GetMapping("/page")
    @ApiOperation("Page query video aggregation configuration")
    public Result<IPage<VideoAggregation>> getAggregationPage(
            @ApiParam("Page number") @RequestParam(defaultValue = "1") Integer current,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("Aggregation name") @RequestParam(required = false) String aggregationName,
            @ApiParam("Aggregation type") @RequestParam(required = false) Integer aggregationType,
            @ApiParam("Status") @RequestParam(required = false) Integer status) {
        
        Page<VideoAggregation> page = new Page<>(current, size);
        IPage<VideoAggregation> result = videoAggregationService.getAggregationPage(
                page, aggregationName, aggregationType, status);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @ApiOperation("Query aggregation configuration by ID")
    public Result<VideoAggregation> getById(@ApiParam("Aggregation configuration ID") @PathVariable Long id) {
        VideoAggregation aggregation = videoAggregationService.getById(id);
        if (aggregation == null) {
            return Result.error("Aggregation configuration does not exist");
        }
        return Result.success(aggregation);
    }

    @PostMapping
    @ApiOperation("Create aggregation configuration")
    public Result<VideoAggregation> create(@RequestBody VideoAggregation videoAggregation) {
        // Validate configuration
        Map<String, Object> validation = videoAggregationService.validateConfig(videoAggregation);
        if (!(Boolean) validation.get("valid")) {
            return Result.error("Configuration validation failed: " + validation.get("errors"));
        }

        // Set initial status
        videoAggregation.setStatus(0);
        videoAggregation.setEnabled(1);
        
        boolean success = videoAggregationService.save(videoAggregation);
        if (success) {
            return Result.success(videoAggregation);
        }
        return Result.error("Create failed");
    }

    @PutMapping("/{id}")
    @ApiOperation("Update aggregation configuration")
    public Result<VideoAggregation> update(@ApiParam("Aggregation configuration ID") @PathVariable Long id,
                                          @RequestBody VideoAggregation videoAggregation) {
        VideoAggregation existing = videoAggregationService.getById(id);
        if (existing == null) {
            return Result.error("Aggregation configuration does not exist");
        }

        // Validate configuration
        Map<String, Object> validation = videoAggregationService.validateConfig(videoAggregation);
        if (!(Boolean) validation.get("valid")) {
            return Result.error("Configuration validation failed: " + validation.get("errors"));
        }

        videoAggregation.setId(id);
        boolean success = videoAggregationService.updateById(videoAggregation);
        if (success) {
            return Result.success(videoAggregation);
        }
        return Result.error("Update failed");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete aggregation configuration")
    public Result<Void> delete(@ApiParam("Aggregation configuration ID") @PathVariable Long id) {
        VideoAggregation existing = videoAggregationService.getById(id);
        if (existing == null) {
            return Result.error("Aggregation configuration does not exist");
        }

        // If running, stop first
        if (existing.getStatus() == 1) {
            videoAggregationService.stopAggregation(id);
        }

        boolean success = videoAggregationService.removeById(id);
        if (success) {
            return Result.success();
        }
        return Result.error("Delete failed");
    }

    @PostMapping("/{id}/start")
    @ApiOperation("Start video aggregation")
    public Result<Void> startAggregation(@ApiParam("Aggregation configuration ID") @PathVariable Long id) {
        boolean success = videoAggregationService.startAggregation(id);
        if (success) {
            return Result.success();
        }
        return Result.error("Start failed");
    }

    @PostMapping("/{id}/stop")
    @ApiOperation("Stop video aggregation")
    public Result<Void> stopAggregation(@ApiParam("Aggregation configuration ID") @PathVariable Long id) {
        boolean success = videoAggregationService.stopAggregation(id);
        if (success) {
            return Result.success();
        }
        return Result.error("Stop failed");
    }

    @PostMapping("/{id}/restart")
    @ApiOperation("Restart video aggregation")
    public Result<Void> restartAggregation(@ApiParam("Aggregation configuration ID") @PathVariable Long id) {
        boolean success = videoAggregationService.restartAggregation(id);
        if (success) {
            return Result.success();
        }
        return Result.error("Restart failed");
    }

    @PostMapping("/{id}/switch")
    @ApiOperation("Switch screen")
    public Result<Void> switchStream(@ApiParam("Aggregation configuration ID") @PathVariable Long id,
                                    @ApiParam("Target stream ID") @RequestParam Long targetStreamId) {
        boolean success = videoAggregationService.switchStream(id, targetStreamId);
        if (success) {
            return Result.success();
        }
        return Result.error("Screen switch failed");
    }

    @GetMapping("/{id}/status")
    @ApiOperation("Get aggregation status")
    public Result<Map<String, Object>> getAggregationStatus(@ApiParam("Aggregation configuration ID") @PathVariable Long id) {
        Map<String, Object> status = videoAggregationService.getAggregationStatus(id);
        if (!(Boolean) status.get("exists")) {
            return Result.error("Aggregation configuration does not exist");
        }
        return Result.success(status);
    }

    @GetMapping("/statistics")
    @ApiOperation("Get status statistics")
    public Result<Map<String, Object>> getStatusStatistics() {
        Map<String, Object> statistics = videoAggregationService.getStatusStatistics();
        return Result.success(statistics);
    }

    @PostMapping("/batch/start")
    @ApiOperation("Batch start aggregation")
    public Result<Map<String, Object>> batchStart(@RequestBody List<Long> ids) {
        int successCount = videoAggregationService.batchStart(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("failed", ids.size() - successCount);
        return Result.success(result);
    }

    @PostMapping("/batch/stop")
    @ApiOperation("Batch stop aggregation")
    public Result<Map<String, Object>> batchStop(@RequestBody List<Long> ids) {
        int successCount = videoAggregationService.batchStop(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("failed", ids.size() - successCount);
        return Result.success(result);
    }

    @GetMapping("/running")
    @ApiOperation("Get running aggregation list")
    public Result<List<VideoAggregation>> getRunningAggregations() {
        List<VideoAggregation> runningList = videoAggregationService.getRunningAggregations();
        return Result.success(runningList);
    }

    @PostMapping("/validate")
    @ApiOperation("Validate aggregation configuration")
    public Result<Map<String, Object>> validateConfig(@RequestBody VideoAggregation videoAggregation) {
        Map<String, Object> validation = videoAggregationService.validateConfig(videoAggregation);
        return Result.success(validation);
    }
} 