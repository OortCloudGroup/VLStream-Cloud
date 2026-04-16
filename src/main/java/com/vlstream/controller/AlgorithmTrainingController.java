package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlstream.common.Result;
import com.vlstream.entity.Algorithm;
import com.vlstream.entity.AlgorithmAnnotation;
import com.vlstream.entity.AlgorithmModel;
import com.vlstream.entity.AlgorithmTraining;
import com.vlstream.service.AlgorithmAnnotationService;
import com.vlstream.service.AlgorithmService;
import com.vlstream.service.AlgorithmModelService;
import com.vlstream.service.AlgorithmTrainingService;
import com.vlstream.service.RemoteTrainingService;
import com.vlstream.service.RemoteTrainingService.LogResult;
import com.vlstream.service.RemoteTrainingService.StartResult;
import com.vlstream.service.RemoteTrainingService.TrainingProgress;
import com.vlstream.service.SSHService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Algorithm Training Management Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/algorithm-training")
@RequiredArgsConstructor
@Api(tags = "Algorithm Training Management")
public class AlgorithmTrainingController {

    private final AlgorithmTrainingService algorithmTrainingService;
    private final RemoteTrainingService remoteTrainingService;
    private final AlgorithmAnnotationService algorithmAnnotationService;
    private final AlgorithmService algorithmService;
    private final AlgorithmModelService algorithmModelService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SSHService sshService;

    @Value("${vlstream.ssh.host}")
    private String sshHost;

    @Value("${vlstream.ssh.port}")
    private Integer sshPort;

    @Value("${vlstream.ssh.username}")
    private String sshUsername;

    @Value("${vlstream.ssh.password}")
    private String sshPassword;

    /**
     * Page query training task list
     */
    @GetMapping("/page")
    @Operation(summary = "Query training task list with pagination", description = "Query training tasks with pagination based on conditions")
    public Result<IPage<AlgorithmTraining>> getTrainingPage(
            @Parameter(description = "Current page", example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Task name") @RequestParam(required = false) String taskName,
            @Parameter(description = "Training type") @RequestParam(required = false) String trainType) {

        log.info("分页查询训练任务列表，参数：current={}, size={}, taskName={}, trainType={}",
                current, size, taskName, trainType);

        // 使用现有的方法，暂时返回所有数据
        List<AlgorithmTraining> list = algorithmTrainingService.selectAlgorithmTrainingList(new AlgorithmTraining());
        for (AlgorithmTraining training : list) {
            Algorithm algorithm = algorithmService.getById(training.getAlgorithmId());
            training.setAlgorithmName(algorithm.getName());
            training.setTrainType(algorithm.getCategory());
            training.setTargetModel(algorithm.getModelFilePath());
        }
        Page<AlgorithmTraining> page = new Page<>(current, size);
        page.setRecords(list);
        page.setTotal(list.size());

        return Result.success(page);
    }

    /**
     * Query training task details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Query training task details", description = "Get training task details by ID")
    public Result<AlgorithmTraining> getTrainingById(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id) {

        log.info("查询训练任务详情：ID={}", id);

        AlgorithmTraining training = algorithmTrainingService.selectAlgorithmTrainingById(id);
        if (training == null) {
            return Result.error("训练任务不存在");
        }

        return Result.success(training);
    }

    /**
     * Add training task
     */
    @PostMapping
    @Operation(summary = "Create training task", description = "Add new training task")
    public Result<String> createTraining(@Valid @RequestBody AlgorithmTraining training) {

        training.setTrainStatus("pending");
        log.info("创建训练任务：{}", training);

        int result = algorithmTrainingService.insertAlgorithmTraining(training);
        if (result > 0) {
            return Result.success("创建成功");
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * Modify training task
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update training task", description = "Update training task information by ID")
    public Result<String> updateTraining(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id,
            @Valid @RequestBody AlgorithmTraining training) {

        log.info("更新训练任务：ID={}, 数据={}", id, training);

        training.setId(id);
        int result = algorithmTrainingService.updateAlgorithmTraining(training);
        if (result > 0) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * Update training status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update training status", description = "Update status of specified training task")
    public Result<String> updateTrainingStatus(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id,
            @RequestBody Map<String, String> statusUpdate) {

        String trainStatus = statusUpdate.get("trainStatus");
        log.info("更新训练状态：ID={}, 状态={}", id, trainStatus);

        try {
            // 获取现有的训练任务
            AlgorithmTraining training = algorithmTrainingService.selectAlgorithmTrainingById(id);
            if (training == null) {
                return Result.error("训练任务不存在");
            }

            // 更新状态
            training.setTrainStatus(trainStatus);
            int result = algorithmTrainingService.updateAlgorithmTraining(training);

            if (result > 0) {
                log.info("训练状态更新成功：ID={}, 新状态={}", id, trainStatus);
                return Result.success("状态更新成功");
            } else {
                return Result.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新训练状态异常：ID={}, 错误={}", id, e.getMessage());
            return Result.error("状态更新异常：" + e.getMessage());
        }
    }

    /**
     * Delete training task
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete training task", description = "Delete training task by ID")
    public Result<String> deleteTraining(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id) {

        log.info("删除训练任务：ID={}", id);

        int result = algorithmTrainingService.deleteAlgorithmTrainingById(id);
        if (result > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * Batch delete training tasks
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Batch delete training tasks", description = "Batch delete training tasks by ID list")
    public Result<String> batchDeleteTraining(@RequestBody List<Long> ids) {

        log.info("批量删除训练任务：IDs={}", ids);

        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的训练任务");
        }

        int result = algorithmTrainingService.deleteAlgorithmTrainingByIds(ids.toArray(new Long[0]));
        if (result > 0) {
            return Result.success("批量删除成功");
        } else {
            return Result.error("批量删除失败");
        }
    }

    /**
     * Start training task
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "Start training task", description = "Start specified training task")
    public Result<StartResult> startTraining(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id,
            @Parameter(description = "Dataset path") @RequestParam(required = false) String datasetPath,
            @Parameter(description = "Training epochs") @RequestParam(defaultValue = "10") Integer epochs,
            @Parameter(description = "Batch size") @RequestParam(defaultValue = "16") Integer batchSize,
            @Parameter(description = "Image size") @RequestParam(required = false) Integer imgSize,
            @Parameter(description = "Extra training parameters") @RequestParam(required = false) String extraParams) {

        log.info("=== 开始训练任务 ===");
        log.info("训练任务ID: {}", id);
        log.info("外部传入数据集路径: {}", datasetPath);

        try {
            AlgorithmTraining training = algorithmTrainingService.selectAlgorithmTrainingById(id);
            if (training == null) {
                return Result.error("训练任务不存在");
            }
            String resolvedDatasetPath = datasetPath;
            if ((resolvedDatasetPath == null || resolvedDatasetPath.trim().isEmpty()) && training.getDatasetId() != null) {
                AlgorithmAnnotation annotation = algorithmAnnotationService.getById(training.getDatasetId());
                if (annotation != null && annotation.getDatasetPath() != null) {
                    resolvedDatasetPath = annotation.getDatasetPath();
                }
            }
            if (resolvedDatasetPath == null || resolvedDatasetPath.trim().isEmpty()) {
                return Result.error("未配置有效的数据集路径");
            }

            Algorithm algorithm = training.getAlgorithmId() != null ? algorithmService.getById(training.getAlgorithmId()) : null;
            String baseModel = algorithm != null ? "/data/work/ultralytics_yolov8-main/" + algorithm.getModelFilePath() : null;
            if (baseModel == null || baseModel.trim().isEmpty()) {
                baseModel = "/data/work/ultralytics_yolov8-main/algorithm_models/yolov8m.pt";
            }

            Map<String, Object> config = parseConfigParams(training.getConfigParams());
            Integer finalEpochs = epochs != null ? epochs : getIntFromConfig(config, "epochs", training.getEpochTotal(), 100);
            Integer finalBatch = batchSize != null ? batchSize : getIntFromConfig(config, "batchSize", null, 16);
            Integer finalImgSize = imgSize != null ? imgSize : getIntFromConfig(config, "imgsz", getIntFromConfig(config, "resolution", null, null), 640);
            String mergedExtraParams = buildExtraParams(config, extraParams);

            StartResult startResult = remoteTrainingService.startTraining(
                    algorithm.getCategory(),
                    id,
                    resolvedDatasetPath,
                    baseModel,
                    finalEpochs,
                    finalBatch,
                    finalImgSize,
                    mergedExtraParams
            );

            AlgorithmTraining update = new AlgorithmTraining();
            update.setId(id);
            update.setTrainStatus("training");
            update.setStartTime(java.time.LocalDateTime.now());
            update.setEpochTotal(finalEpochs);
            update.setProgress(0);
            update.setLogPath(startResult.getLogPath());
            algorithmTrainingService.updateAlgorithmTraining(update);

            log.info("训练任务{}已启动，日志路径: {}", id, startResult.getLogPath());
            return Result.success(startResult);
        } catch (Exception e) {
            log.error("触发训练任务失败: {}", e.getMessage(), e);
            return Result.error("触发训练任务失败: " + e.getMessage());
        }
    }

    /**
     * Check model files on remote server
     */
    @GetMapping("/check-remote-files")
    @Operation(summary = "Check remote server files", description = "Check model files on remote server")
    public Result<String> checkRemoteFiles() {
        try {
            // 使用SSH服务检查远程文件
            SSHService.SSHExecutionResult result = sshService.executeCommand(
                    sshHost,
                    sshPort,
                    sshUsername,
                    sshPassword,
                    "find /data/work/ultralytics_yolov8-main -name '*.pt' -type f -ls"
            );

            if (result.isSuccess()) {
                log.info("远程文件检查成功: {}", result.getOutput());
                return Result.success("远程文件检查成功: " + result.getOutput());
            } else {
                log.error("远程文件检查失败: {}", result.getErrorMsg());
                return Result.error("远程文件检查失败: " + result.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("检查远程文件异常: {}", e.getMessage(), e);
            return Result.error("检查远程文件异常: " + e.getMessage());
        }
    }

    /**
     * Diagnose remote server conda environment
     */
    @GetMapping("/diagnose-conda")
    @Operation(summary = "Diagnose conda environment", description = "Check conda installation on remote server")
    public Result<String> diagnoseConda() {
        try {
            // 构建诊断命令
            StringBuilder diagCmd = new StringBuilder();
            diagCmd.append("echo '=== 环境诊断 ===' && ");
            diagCmd.append("echo 'PATH: '$PATH && ");
            diagCmd.append("echo '=== 查找conda ===' && ");
            diagCmd.append("which conda 2>/dev/null || echo 'conda not in PATH' && ");
            diagCmd.append("find /home -name 'conda' -type f 2>/dev/null | head -5 && ");
            diagCmd.append("find /opt -name 'conda' -type f 2>/dev/null | head -5 && ");
            diagCmd.append("find /usr -name 'conda' -type f 2>/dev/null | head -5 && ");
            diagCmd.append("echo '=== 查找conda.sh ===' && ");
            diagCmd.append("find /home -name 'conda.sh' -type f 2>/dev/null | head -5 && ");
            diagCmd.append("find /opt -name 'conda.sh' -type f 2>/dev/null | head -5 && ");
            diagCmd.append("echo '=== 查找yolo ===' && ");
            diagCmd.append("which yolo 2>/dev/null || echo 'yolo not in PATH' && ");
            diagCmd.append("find /home -name 'yolo' -type f 2>/dev/null | head -3 && ");
            diagCmd.append("echo '=== Python环境 ===' && ");
            diagCmd.append("which python 2>/dev/null || echo 'python not found' && ");
            diagCmd.append("which python3 2>/dev/null || echo 'python3 not found' && ");
            diagCmd.append("echo '=== 完成 ==='");

            SSHService.SSHExecutionResult result = sshService.executeCommand(
                    sshHost,
                    sshPort,
                    sshUsername,
                    sshPassword,
                    diagCmd.toString()
            );

            if (result.isSuccess()) {
                log.info("Conda诊断成功: {}", result.getOutput());
                return Result.success("Conda诊断结果:\n" + result.getOutput());
            } else {
                log.error("Conda诊断失败: {}", result.getErrorMsg());
                return Result.error("Conda诊断失败: " + result.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("Conda诊断异常: {}", e.getMessage(), e);
            return Result.error("Conda诊断异常: " + e.getMessage());
        }
    }

    /**
     * Stop training task
     */
    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop training task", description = "Stop specified training task")
    public Result<String> stopTraining(@Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id) {

        log.info("停止训练任务: ID={}", id);
        AlgorithmTraining training = algorithmTrainingService.selectAlgorithmTrainingById(id);
        if (training == null) {
            return Result.error("找不到训练任务");
        }

        boolean stopped = remoteTrainingService.stopTraining(id, training.getLogPath());
        if (stopped) {
            AlgorithmTraining update = new AlgorithmTraining();
            update.setId(id);
            update.setTrainStatus("stopped");
            update.setEndTime(java.time.LocalDateTime.now());
            algorithmTrainingService.updateAlgorithmTraining(update);
            return Result.success("训练任务已停止");
        }
        return Result.error("训练任务停止失败");
    }

    /**
     * Get training logs
     */
    @GetMapping("/{id}/logs")
    @Operation(summary = "Get training logs", description = "Get logs of specified training task")
    public Result<LogResult> getTrainingLogs(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id,
            @RequestParam(value = "logPath", required = false) String logPath,
            @RequestParam(value = "lines", defaultValue = "200") Integer lines) {

        log.info("获取训练日志：ID={}?logPath={}", id, logPath);
        AlgorithmTraining training = algorithmTrainingService.selectAlgorithmTrainingById(id);
        if (training == null) {
            return Result.error("找不到训练任务");
        }
        Algorithm algorithm = training.getAlgorithmId() != null ? algorithmService.getById(training.getAlgorithmId()) : null;
        LogResult logResult = remoteTrainingService.getTrainingLogs(
                id,
                logPath,
                algorithm.getCategory(),
                training.getTaskName(),
                lines == null ? 200 : lines
        );
        return Result.success(logResult);
    }

    /**
     * Get training status
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "Get training status", description = "Get status of specified training task")
    public Result<TrainingProgress> getTrainingStatus(
            @Parameter(description = "Training task ID", example = "1") @PathVariable @NotNull Long id,
            @RequestParam(value = "logPath", required = false) String logPath) {

        log.info("获取训练状态：ID={}?logPath={}", id, logPath);
        AlgorithmTraining training = algorithmTrainingService.selectAlgorithmTrainingById(id);
        if (training == null) {
            return Result.error("找不到训练任务");
        }
        TrainingProgress progress = remoteTrainingService.getProgress(id, logPath);
        if (progress != null && progress.isCompleted()) {
            AlgorithmTraining update = new AlgorithmTraining();
            update.setId(id);
            update.setTrainStatus("completed");
            update.setProgress(100);
            update.setEndTime(java.time.LocalDateTime.now());
            algorithmTrainingService.updateAlgorithmTraining(update);
        }
        return Result.success(progress);
    }

    /**
     * 下载训练模型文件
     */
    private Map<String, Object> parseConfigParams(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("解析异常: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取配置
     */
    private Integer getIntFromConfig(Map<String, Object> config, String key, Integer fallback, Integer defaultValue) {
        if (config != null && config.containsKey(key)) {
            Object val = config.get(key);
            try {
                return Integer.parseInt(String.valueOf(val));
            } catch (Exception ignored) {
            }
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultValue;
    }

    private String buildExtraParams(Map<String, Object> config, String extraParams) {
        StringBuilder sb = new StringBuilder();
        if (extraParams != null && !extraParams.trim().isEmpty()) {
            sb.append(extraParams.trim());
        }
        if (config != null) {
            config.forEach((k, v) -> {
                if (v == null) {
                    return;
                }
                String key = k.toLowerCase();
                if (key.equals("epochs") || key.equals("batch") || key.equals("batchsize") || key.equals("imgsz") || key.equals("resolution") || key.equals("datasetid") || key.equals("datastrategy") || key.equals("autopublish") || key.equals("customvalidation")) {
                    return;
                }
                sb.append(' ').append(k).append('=').append(String.valueOf(v));
            });
        }
        return sb.toString().trim();
    }

    @GetMapping("/download-model")
    @Operation(summary = "Download model file", description = "Download trained model file from remote server")
    public void downloadModel(@RequestParam(required = false) String id, @RequestParam String path, HttpServletResponse response) {
        try {
            log.info("下载模型文件: {}", path);

            // 从远程服务器下载文件
            SSHService.SSHExecutionResult result = sshService.executeCommand(
                    sshHost,
                    sshPort,
                    sshUsername,
                    sshPassword,
                    String.format("cd /data/work/ultralytics_yolov8-main && base64 %s", path)
            );

            if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
                // 清理base64内容，移除可能的换行符和其他字符
                String base64Content = result.getOutput().trim().replaceAll("\\s+", "");
                log.info("Base64内容长度: {}", base64Content.length());

                // 解码base64内容
                byte[] fileContent = java.util.Base64.getDecoder().decode(base64Content);

                String fileName = path.substring(path.lastIndexOf('/') + 1);
                response.setContentType("application/octet-stream");

                String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
                response.setContentLength(fileContent.length);

                // 写入响应
                response.getOutputStream().write(fileContent);
                response.getOutputStream().flush();

                if (id != null && !id.trim().isEmpty()) {
                    try {
                        Long modelId = Long.valueOf(id.trim());
                        UpdateWrapper<AlgorithmModel> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", modelId)
                                .setSql("download_count = download_count + 1");
                        boolean updated = algorithmModelService.update(updateWrapper);
                        if (!updated) {
                            log.warn("Failed to increment download count, modelId={}", modelId);
                        }
                    } catch (NumberFormatException ex) {
                        log.warn("Invalid model id for download count: {}", id);
                    } catch (Exception ex) {
                        log.warn("Failed to increment download count, modelId={}, error={}", id, ex.getMessage());
                    }
                }

                log.info("模型文件下载成功: {}", fileName);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Model file not found: " + path);
                log.error("模型文件不存在: {}", path);
            }

        } catch (Exception e) {
            log.error("下载模型文件失败: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Download failed: " + e.getMessage());
            } catch (Exception ex) {
                log.error("写入错误响应失败: {}", ex.getMessage());
            }
        }
    }
}
