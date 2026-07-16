/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.config.VlsTrainingProperties;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.domain.RemoteServer;
import com.ruoyi.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.mapper.VlsRemoteServerMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Submits and observes real remote YOLO training jobs through SSH.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteTrainingService {

    private static final String STATUS_TRAINING = "training";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_STOP = "stop";

    private static final Pattern EPOCH_LINE =
        Pattern.compile("(?m)^\\s*(\\d+)\\s*/\\s*(\\d+)\\s+.*$");
    private static final Pattern EPOCH_TEXT =
        Pattern.compile("(?i)epoch\\D+(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern LOSS =
        Pattern.compile("(?i)loss[:=\\s]+([0-9]+(?:\\.[0-9]+)?)");

    private final VlsRemoteServerMapper remoteServerMapper;
    private final VlsAlgorithmTrainingMapper trainingMapper;
    private final SshService sshService;
    private final VlsSshProperties sshProperties;
    private final VlsTrainingProperties trainingProperties;

    public StartResult startTraining(String taskType, Long taskId, String datasetPath, String baseModel,
                                     Integer epochs, Integer batchSize, Integer imgSize, String extraParams) {
        StartResult startResult = new StartResult();
        startResult.setSubmitted(false);
        if (taskId == null) {
            startResult.setMessage("Training task ID is required");
            return startResult;
        }
        if (!StringUtils.hasText(datasetPath)) {
            startResult.setMessage("Dataset path is required");
            return startResult;
        }
        if (!StringUtils.hasText(baseModel)) {
            startResult.setMessage("Base model path is required");
            return startResult;
        }

        RemoteServer server = activeServer();
        if (server == null) {
            startResult.setMessage("No available training server found");
            return startResult;
        }

        String task = StringUtils.hasText(taskType) ? taskType.trim() : "detect";
        String logPath = server.getWorkDir() + "/" + logDir() + "/training_" + taskId + ".log";
        String command = buildTrainingCommand(server, task, logPath, datasetPath, baseModel, epochs, batchSize, imgSize, extraParams);
        String wrappedCommand = wrapWithBash(command);
        SshService.SshExecutionResult result = executeWithFallback(server, wrappedCommand);
        startResult.setCommand(wrappedCommand);
        startResult.setLogPath(logPath);
        startResult.setDatasetPath(datasetPath);
        startResult.setTrainType(task);
        if (result != null && result.isSuccess()) {
            String pid = result.getOutput() == null ? "" : result.getOutput().trim().replaceAll("\\s+", "");
            startResult.setPid(pid);
            startResult.setSubmitted(true);
            startResult.setMessage("Training command submitted to remote server");
            return startResult;
        }

        String error = result == null ? "SSH execution result is null" : result.getErrorMsg();
        markTrainingFailed(taskId, error);
        startResult.setMessage("Training command failed: " + error);
        return startResult;
    }

    public LogResult getTrainingLogs(Long taskId, String logPath, String trainType, String taskName, int lines) {
        LogResult logResult = new LogResult();
        RemoteServer server = activeServer();
        if (server == null) {
            logResult.setMessage("No available training server found");
            return logResult;
        }
        String resolvedLogPath = StringUtils.hasText(logPath)
            ? logPath
            : server.getWorkDir() + "/" + logDir() + "/training_" + taskId + ".log";
        SshService.SshExecutionResult result = executeWithFallback(server,
            wrapWithBash("tail -n " + Math.max(1, lines) + " " + shellQuote(resolvedLogPath)));
        logResult.setLogPath(resolvedLogPath);
        if (result == null || !result.isSuccess()) {
            logResult.setLogContent(result == null ? "" : result.getErrorMsg());
            logResult.setMessage("Failed to read training log: " + (result == null ? "empty result" : result.getErrorMsg()));
            return logResult;
        }

        String logContent = result.getOutput();
        logResult.setLogContent(logContent);
        TrainingProgress progress = parseTrainingLog(logContent, taskId);
        logResult.setCurrentEpoch(progress.getCurrentEpoch());
        logResult.setTotalEpoch(progress.getTotalEpochs());
        logResult.setProgress(progress.getPercentage());
        logResult.setCompleted(progress.isCompleted());
        logResult.setLatestLoss(progress.getLatestLoss());
        if (progress.isCompleted()) {
            String modelPath = processTrainingResult(taskId, server, trainType, taskName);
            logResult.setModelPath(modelPath);
            logResult.setStatus(STATUS_COMPLETED);
        } else if (progress.getPercentage() != null) {
            AlgorithmTraining update = new AlgorithmTraining();
            update.setId(taskId);
            update.setProgress(progress.getPercentage());
            trainingMapper.updateById(update);
        }
        return logResult;
    }

    public TrainingProgress getProgress(Long taskId, String logPath) {
        RemoteServer server = activeServer();
        if (server == null) {
            return null;
        }
        String resolvedLogPath = StringUtils.hasText(logPath)
            ? logPath
            : server.getWorkDir() + "/" + logDir() + "/training_" + taskId + ".log";
        SshService.SshExecutionResult result = executeWithFallback(server,
            wrapWithBash("tail -n 80 " + shellQuote(resolvedLogPath)));
        if (result == null || !result.isSuccess()) {
            return null;
        }
        TrainingProgress progress = parseTrainingLog(result.getOutput(), taskId);
        if (progress.isCompleted()) {
            AlgorithmTraining update = new AlgorithmTraining();
            update.setId(taskId);
            update.setTrainStatus(STATUS_COMPLETED);
            update.setProgress(100);
            update.setEndTime(new Date());
            trainingMapper.updateById(update);
        }
        return progress;
    }

    public boolean stopTraining(Long taskId, String logPath) {
        RemoteServer server = activeServer();
        if (server == null) {
            return false;
        }
        String resolvedLogPath = StringUtils.hasText(logPath)
            ? logPath
            : server.getWorkDir() + "/" + logDir() + "/training_" + taskId + ".log";
        String command = "ps -ef | grep " + shellQuote(resolvedLogPath)
            + " | grep -v grep | awk '{print $2}' | xargs -r kill -9";
        SshService.SshExecutionResult result = executeWithFallback(server, wrapWithBash(command));
        if (result != null && result.isSuccess()) {
            AlgorithmTraining update = new AlgorithmTraining();
            update.setId(taskId);
            update.setTrainStatus(STATUS_STOP);
            update.setEndTime(new Date());
            trainingMapper.updateById(update);
            return true;
        }
        return false;
    }

    public String exportModel(String modelPath, String format) {
        RemoteServer server = activeServer();
        if (server == null || !StringUtils.hasText(modelPath) || !StringUtils.hasText(format)) {
            return null;
        }
        String normalizedFormat = format.trim().toLowerCase();
        String exportPath = resolveExportPath(modelPath, normalizedFormat);
        StringBuilder command = new StringBuilder();
        command.append("source ~/.bashrc && ");
        command.append("source ").append(shellQuote(trainingProperties.getCondaProfile())).append(" && ");
        command.append("cd ").append(shellQuote(server.getWorkDir())).append(" && ");
        if (StringUtils.hasText(server.getCondaEnv())) {
            command.append("conda activate ").append(shellQuote(server.getCondaEnv())).append(" && ");
        }
        command.append("yolo export model=").append(shellQuote(modelPath));
        command.append(" format=").append(shellQuote(normalizedFormat));
        command.append(" && if [ -f ").append(shellQuote(exportPath)).append(" ]; then echo OK; else echo MISSING; exit 1; fi");

        SshService.SshExecutionResult result = executeWithFallback(server, wrapWithBash(command.toString()));
        if (result != null && result.isSuccess()) {
            return exportPath;
        }
        log.warn("Remote model export failed: format={}, path={}, error={}", normalizedFormat, modelPath,
            result == null ? null : result.getErrorMsg());
        return null;
    }

    public byte[] readRemoteFile(String path) {
        RemoteServer server = activeServer();
        if (server == null || !StringUtils.hasText(path)) {
            return null;
        }
        SshService.SshExecutionResult result = executeWithFallback(server, wrapWithBash("base64 " + shellQuote(path)));
        if (result == null || !result.isSuccess() || !StringUtils.hasText(result.getOutput())) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(result.getOutput().replaceAll("\\s+", ""));
        } catch (IllegalArgumentException ex) {
            log.warn("Remote file base64 decode failed: {}", path, ex);
            return null;
        }
    }

    public TrainingProgress parseTrainingLog(String logContent, Long taskId) {
        TrainingProgress progress = new TrainingProgress();
        progress.setTaskId(taskId);
        if (!StringUtils.hasText(logContent)) {
            return progress;
        }
        Matcher matcher = EPOCH_LINE.matcher(logContent);
        while (matcher.find()) {
            setEpoch(progress, matcher.group(1), matcher.group(2));
        }
        Matcher textMatcher = EPOCH_TEXT.matcher(logContent);
        while (textMatcher.find()) {
            setEpoch(progress, textMatcher.group(1), textMatcher.group(2));
        }
        Matcher lossMatcher = LOSS.matcher(logContent);
        while (lossMatcher.find()) {
            try {
                progress.setLatestLoss(Float.valueOf(lossMatcher.group(1)));
            } catch (NumberFormatException ignored) {
                // Ignore malformed loss fragments from non-standard logs.
            }
        }
        if (logContent.contains("Training complete") || logContent.contains("Results saved") || logContent.contains("best.pt")) {
            progress.setCompleted(true);
            progress.setPercentage(100);
        }
        return progress;
    }

    @PostConstruct
    public void initDefaultServer() {
        try {
            remoteServerMapper.createTableIfNotExists();
            Long count = remoteServerMapper.selectCount(new LambdaQueryWrapper<RemoteServer>());
            if (count != null && count > 0L) {
                return;
            }
            RemoteServer server = new RemoteServer();
            server.setServerName("YOLOv8 training server");
            server.setServerIp(sshProperties.getHost());
            server.setServerPort(sshProperties.getPort());
            server.setUsername(sshProperties.getUsername());
            server.setPassword(encryptPassword(sshProperties.getPassword()));
            server.setCondaEnv(trainingProperties.getDefaultCondaEnv());
            server.setWorkDir(trainingProperties.getDefaultWorkDir());
            server.setStatus(1);
            server.setCreateTime(new Date());
            remoteServerMapper.insert(server);
            log.info("Default remote training server initialized: {}@{}:{}", server.getUsername(), server.getServerIp(), server.getServerPort());
        } catch (Exception ex) {
            log.warn("Remote training server initialization skipped: {}", ex.getMessage());
        }
    }

    private String buildTrainingCommand(RemoteServer server, String task, String logPath, String datasetPath, String baseModel,
                                        Integer epochs, Integer batchSize, Integer imgSize, String extraParams) {
        StringBuilder command = new StringBuilder();
        command.append("source ~/.bashrc && ");
        command.append("source ").append(shellQuote(trainingProperties.getCondaProfile())).append(" && ");
        command.append("cd ").append(shellQuote(server.getWorkDir())).append(" && ");
        command.append("mkdir -p ").append(shellQuote(logDir())).append(" && ");
        command.append("rm -f ").append(shellQuote(logPath)).append(" && ");
        command.append("conda activate ").append(shellQuote(server.getCondaEnv())).append(" && ");
        command.append("nohup yolo ").append(shellQuote(task)).append(" train");
        command.append(" data=").append(shellQuote(datasetPath));
        command.append(" model=").append(shellQuote(baseModel));
        if (epochs != null) {
            command.append(" epochs=").append(epochs);
        }
        if (batchSize != null) {
            command.append(" batch=").append(batchSize);
        }
        if (imgSize != null) {
            command.append(" imgsz=").append(imgSize);
        }
        if (StringUtils.hasText(extraParams)) {
            command.append(' ').append(extraParams.trim());
        }
        command.append(" >> ").append(shellQuote(logPath)).append(" 2>&1 & echo $!");
        return command.toString();
    }

    private String processTrainingResult(Long taskId, RemoteServer server, String trainType, String taskName) {
        String taskFolder = StringUtils.hasText(trainType) ? trainType.trim() : "detect";
        String findCommand = "cd " + shellQuote(server.getWorkDir())
            + " && find runs/" + shellQuote(taskFolder) + " -name 'train*' -type d | sort -V | tail -1";
        SshService.SshExecutionResult findResult = executeWithFallback(server, wrapWithBash(findCommand));
        if (findResult == null || !findResult.isSuccess() || !StringUtils.hasText(findResult.getOutput())) {
            return null;
        }

        String resultDir = findResult.getOutput().trim();
        String finalTaskName = StringUtils.hasText(taskName) ? safeFileStem(taskName) : "training_" + taskId;
        String modelPath = server.getWorkDir() + "/" + resultDir + "/weights/" + finalTaskName + ".pt";
        String processCommand = "cd " + shellQuote(server.getWorkDir())
            + " && if [ -f " + shellQuote(resultDir + "/weights/best.pt") + " ]; then "
            + "cp " + shellQuote(resultDir + "/weights/best.pt") + " " + shellQuote(resultDir + "/weights/" + finalTaskName + ".pt")
            + " && echo OK; else echo MISSING; exit 1; fi";
        SshService.SshExecutionResult processResult = executeWithFallback(server, wrapWithBash(processCommand));
        if (processResult == null || !processResult.isSuccess()) {
            return null;
        }

        AlgorithmTraining update = new AlgorithmTraining();
        update.setId(taskId);
        update.setModelOutputPath(modelPath);
        update.setModelPath(modelPath);
        update.setTrainStatus(STATUS_COMPLETED);
        update.setProgress(100);
        update.setEndTime(new Date());
        trainingMapper.updateById(update);
        return modelPath;
    }

    private SshService.SshExecutionResult executeWithFallback(RemoteServer server, String command) {
        String host = server == null ? sshProperties.getHost() : server.getServerIp();
        Integer port = server == null || server.getServerPort() == null ? sshProperties.getPort() : server.getServerPort();
        String username = server == null ? sshProperties.getUsername() : server.getUsername();
        String password = server == null ? sshProperties.getPassword() : decryptPassword(server.getPassword());
        SshService.SshExecutionResult result = sshService.executeCommand(host, port, username, password, command);
        if (shouldRetryWithDefault(result) && server != null && !sameAsDefault(host, port, username, password)) {
            log.warn("SSH auth failed for {}@{}:{}, retrying default configured SSH account", username, host, port);
            return sshService.executeCommand(sshProperties.getHost(), sshProperties.getPort(),
                sshProperties.getUsername(), sshProperties.getPassword(), command);
        }
        return result;
    }

    private RemoteServer activeServer() {
        return remoteServerMapper.selectActiveServer();
    }

    private void markTrainingFailed(Long taskId, String errorMessage) {
        AlgorithmTraining update = new AlgorithmTraining();
        update.setId(taskId);
        update.setTrainStatus(STATUS_FAILED);
        update.setErrorMessage(errorMessage == null ? "Training command failed" : errorMessage);
        update.setEndTime(new Date());
        trainingMapper.updateById(update);
    }

    private boolean shouldRetryWithDefault(SshService.SshExecutionResult result) {
        if (result == null || result.isSuccess() || result.getErrorMsg() == null) {
            return false;
        }
        String error = result.getErrorMsg().toLowerCase();
        return error.contains("auth fail") || error.contains("authentication") || error.contains("permission denied");
    }

    private boolean sameAsDefault(String host, Integer port, String username, String password) {
        return equals(host, sshProperties.getHost())
            && equals(port, sshProperties.getPort())
            && equals(username, sshProperties.getUsername())
            && equals(password, sshProperties.getPassword());
    }

    private String wrapWithBash(String command) {
        return "bash -lc " + shellQuote(command);
    }

    private static String shellQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private String resolveExportPath(String modelPath, String format) {
        if (modelPath.toLowerCase().endsWith(".pt")) {
            return modelPath.substring(0, modelPath.length() - 3) + "." + format;
        }
        return modelPath + "." + format;
    }

    private String safeFileStem(String value) {
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void setEpoch(TrainingProgress progress, String current, String total) {
        try {
            int currentEpoch = Integer.parseInt(current);
            int totalEpochs = Integer.parseInt(total);
            if (totalEpochs > 0) {
                progress.setCurrentEpoch(currentEpoch);
                progress.setTotalEpochs(totalEpochs);
                progress.setPercentage((int) Math.round((double) currentEpoch * 100D / (double) totalEpochs));
            }
        } catch (NumberFormatException ignored) {
            // Ignore non-numeric progress fragments.
        }
    }

    private String logDir() {
        return StringUtils.hasText(trainingProperties.getLogDir()) ? trainingProperties.getLogDir() : "logs";
    }

    private String encryptPassword(String password) {
        return Base64.getEncoder().encodeToString((password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
    }

    private String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(encryptedPassword), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return encryptedPassword;
        }
    }

    private static boolean equals(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    @Data
    public static class StartResult {
        private boolean submitted;
        private String logPath;
        private String command;
        private String pid;
        private String datasetPath;
        private String modelPath;
        private String trainType;
        private String message;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("submitted", submitted);
            map.put("logPath", logPath);
            map.put("command", command);
            map.put("pid", pid);
            map.put("datasetPath", datasetPath);
            map.put("modelPath", modelPath);
            map.put("trainType", trainType);
            map.put("message", message);
            return map;
        }
    }

    @Data
    public static class LogResult {
        private String logPath;
        private String logContent;
        private Integer currentEpoch;
        private Integer totalEpoch;
        private Integer progress;
        private boolean completed;
        private String modelPath;
        private String status;
        private String message;
        private Float latestLoss;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("logPath", logPath);
            map.put("logContent", logContent);
            map.put("currentEpoch", currentEpoch);
            map.put("totalEpoch", totalEpoch);
            map.put("progress", progress);
            map.put("completed", completed);
            map.put("modelPath", modelPath);
            map.put("status", status);
            map.put("message", message);
            map.put("latestLoss", latestLoss);
            return map;
        }
    }

    @Data
    public static class TrainingProgress {
        private Long taskId;
        private Integer currentEpoch;
        private Integer totalEpochs;
        private Integer percentage;
        private Float latestLoss;
        private boolean completed;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("taskId", taskId);
            map.put("currentEpoch", currentEpoch);
            map.put("totalEpochs", totalEpochs);
            map.put("percentage", percentage);
            map.put("latestLoss", latestLoss);
            map.put("completed", completed);
            return map;
        }
    }
}
