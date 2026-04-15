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
 * 视频汇聚配置控制器
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/video-aggregation")
@Api(tags = "视频汇聚管理")
public class VideoAggregationController {

    @Autowired
    private VideoAggregationService videoAggregationService;

    @GetMapping("/page")
    @ApiOperation("分页查询视频汇聚配置")
    public Result<IPage<VideoAggregation>> getAggregationPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer current,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("汇聚名称") @RequestParam(required = false) String aggregationName,
            @ApiParam("汇聚类型") @RequestParam(required = false) Integer aggregationType,
            @ApiParam("状态") @RequestParam(required = false) Integer status) {
        
        Page<VideoAggregation> page = new Page<>(current, size);
        IPage<VideoAggregation> result = videoAggregationService.getAggregationPage(
                page, aggregationName, aggregationType, status);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询汇聚配置")
    public Result<VideoAggregation> getById(@ApiParam("汇聚配置ID") @PathVariable Long id) {
        VideoAggregation aggregation = videoAggregationService.getById(id);
        if (aggregation == null) {
            return Result.error("汇聚配置不存在");
        }
        return Result.success(aggregation);
    }

    @PostMapping
    @ApiOperation("创建汇聚配置")
    public Result<VideoAggregation> create(@RequestBody VideoAggregation videoAggregation) {
        // 验证配置
        Map<String, Object> validation = videoAggregationService.validateConfig(videoAggregation);
        if (!(Boolean) validation.get("valid")) {
            return Result.error("配置验证失败：" + validation.get("errors"));
        }

        // 设置初始状态
        videoAggregation.setStatus(0);
        videoAggregation.setEnabled(1);
        
        boolean success = videoAggregationService.save(videoAggregation);
        if (success) {
            return Result.success(videoAggregation);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/{id}")
    @ApiOperation("更新汇聚配置")
    public Result<VideoAggregation> update(@ApiParam("汇聚配置ID") @PathVariable Long id,
                                          @RequestBody VideoAggregation videoAggregation) {
        VideoAggregation existing = videoAggregationService.getById(id);
        if (existing == null) {
            return Result.error("汇聚配置不存在");
        }

        // 验证配置
        Map<String, Object> validation = videoAggregationService.validateConfig(videoAggregation);
        if (!(Boolean) validation.get("valid")) {
            return Result.error("配置验证失败：" + validation.get("errors"));
        }

        videoAggregation.setId(id);
        boolean success = videoAggregationService.updateById(videoAggregation);
        if (success) {
            return Result.success(videoAggregation);
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除汇聚配置")
    public Result<Void> delete(@ApiParam("汇聚配置ID") @PathVariable Long id) {
        VideoAggregation existing = videoAggregationService.getById(id);
        if (existing == null) {
            return Result.error("汇聚配置不存在");
        }

        // 如果正在运行，先停止
        if (existing.getStatus() == 1) {
            videoAggregationService.stopAggregation(id);
        }

        boolean success = videoAggregationService.removeById(id);
        if (success) {
            return Result.success();
        }
        return Result.error("删除失败");
    }

    @PostMapping("/{id}/start")
    @ApiOperation("启动视频汇聚")
    public Result<Void> startAggregation(@ApiParam("汇聚配置ID") @PathVariable Long id) {
        boolean success = videoAggregationService.startAggregation(id);
        if (success) {
            return Result.success();
        }
        return Result.error("启动失败");
    }

    @PostMapping("/{id}/stop")
    @ApiOperation("停止视频汇聚")
    public Result<Void> stopAggregation(@ApiParam("汇聚配置ID") @PathVariable Long id) {
        boolean success = videoAggregationService.stopAggregation(id);
        if (success) {
            return Result.success();
        }
        return Result.error("停止失败");
    }

    @PostMapping("/{id}/restart")
    @ApiOperation("重启视频汇聚")
    public Result<Void> restartAggregation(@ApiParam("汇聚配置ID") @PathVariable Long id) {
        boolean success = videoAggregationService.restartAggregation(id);
        if (success) {
            return Result.success();
        }
        return Result.error("重启失败");
    }

    @PostMapping("/{id}/switch")
    @ApiOperation("切换画面")
    public Result<Void> switchStream(@ApiParam("汇聚配置ID") @PathVariable Long id,
                                    @ApiParam("目标流ID") @RequestParam Long targetStreamId) {
        boolean success = videoAggregationService.switchStream(id, targetStreamId);
        if (success) {
            return Result.success();
        }
        return Result.error("画面切换失败");
    }

    @GetMapping("/{id}/status")
    @ApiOperation("获取汇聚状态")
    public Result<Map<String, Object>> getAggregationStatus(@ApiParam("汇聚配置ID") @PathVariable Long id) {
        Map<String, Object> status = videoAggregationService.getAggregationStatus(id);
        if (!(Boolean) status.get("exists")) {
            return Result.error("汇聚配置不存在");
        }
        return Result.success(status);
    }

    @GetMapping("/statistics")
    @ApiOperation("获取状态统计")
    public Result<Map<String, Object>> getStatusStatistics() {
        Map<String, Object> statistics = videoAggregationService.getStatusStatistics();
        return Result.success(statistics);
    }

    @PostMapping("/batch/start")
    @ApiOperation("批量启动汇聚")
    public Result<Map<String, Object>> batchStart(@RequestBody List<Long> ids) {
        int successCount = videoAggregationService.batchStart(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("failed", ids.size() - successCount);
        return Result.success(result);
    }

    @PostMapping("/batch/stop")
    @ApiOperation("批量停止汇聚")
    public Result<Map<String, Object>> batchStop(@RequestBody List<Long> ids) {
        int successCount = videoAggregationService.batchStop(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", successCount);
        result.put("failed", ids.size() - successCount);
        return Result.success(result);
    }

    @GetMapping("/running")
    @ApiOperation("获取运行中的汇聚列表")
    public Result<List<VideoAggregation>> getRunningAggregations() {
        List<VideoAggregation> runningList = videoAggregationService.getRunningAggregations();
        return Result.success(runningList);
    }

    @PostMapping("/validate")
    @ApiOperation("验证汇聚配置")
    public Result<Map<String, Object>> validateConfig(@RequestBody VideoAggregation videoAggregation) {
        Map<String, Object> validation = videoAggregationService.validateConfig(videoAggregation);
        return Result.success(validation);
    }
} 