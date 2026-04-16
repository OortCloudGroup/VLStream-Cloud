package com.vlstream.service;

import com.vlstream.entity.AlgorithmTraining;
import com.vlstream.entity.RemoteServer;
import com.vlstream.mapper.RemoteServerMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remote training service class
 */
@Slf4j
@Service
public class RemoteTrainingService {

    @Autowired
    private RemoteServerMapper remoteServerMapper;

    @Autowired
    private SSHService sshService;

    @Autowired
    private AlgorithmTrainingService algorithmTrainingService;

    @Value("${vlstream.ssh.host}")
    private String sshHost;

    @Value("${vlstream.ssh.port}")
    private Integer sshPort;

    @Value("${vlstream.ssh.username}")
    private String sshUsername;

    @Value("${vlstream.ssh.password}")
    private String sshPassword;

    private static final String DEFAULT_LOG_DIR = "logs";

    /**
     * Start remote YOLO training (run in background, save logs to disk)
     */
    public StartResult startTraining(String taskType,
                                     Long taskId,
                                     String datasetPath,
                                     String baseModel,
                                     Integer epochs,
                                     Integer batchSize,
                                     Integer imgSize,
                                     String extraParams) {
        StartResult startResult = new StartResult();
        try {
            RemoteServer server = remoteServerMapper.selectActiveServer();
            if (server == null) {
                startResult.setMessage("未找到可用的训练服务器");
                return startResult;
            }

            String logPath = server.getWorkDir() + "/" + DEFAULT_LOG_DIR + "/training_" + taskId + ".log";
            String task = (taskType == null || taskType.trim().isEmpty()) ? "detect" : taskType.trim();

            StringBuilder cmd = new StringBuilder();
            cmd.append("source ~/.bashrc && ");
            cmd.append("source /data/work/anaconda3/etc/profile.d/conda.sh && ");
            cmd.append("cd ").append(server.getWorkDir()).append(" && ");
            cmd.append("mkdir -p ").append(DEFAULT_LOG_DIR).append(" && ");
            cmd.append("conda activate ").append(server.getCondaEnv()).append(" && ");
            cmd.append("nohup yolo ").append(task).append(" train");
            cmd.append(" data=").append(datasetPath).append("/dataset.yaml");
            cmd.append(" model=").append(baseModel);
            if (epochs != null) {
                cmd.append(" epochs=").append(epochs);
            }
            if (batchSize != null) {
                cmd.append(" batch=").append(batchSize);
            }
            if (imgSize != null) {
                cmd.append(" imgsz=").append(imgSize);
            }
//            if (extraParams != null && !extraParams.trim().isEmpty()) {
//                cmd.append(" ").append(extraParams.trim());
//            }
            cmd.append(" >> ").append(logPath).append(" 2>&1 & echo $!");

            String wrappedCmd = wrapWithBash(cmd.toString());
            log.info(wrappedCmd);
            SSHService.SSHExecutionResult execResult = executeWithFallback(server, wrappedCmd);

            startResult.setCommand(wrappedCmd);
            startResult.setLogPath(logPath);
            startResult.setDatasetPath(datasetPath + "/dataset.yaml");
            startResult.setTrainType(task);

            if (execResult.isSuccess()) {
                String pid = execResult.getOutput() != null ? execResult.getOutput().trim().replaceAll("\\s+", "") : "";
                startResult.setPid(pid);
                startResult.setMessage("训练已启动");
            } else {
                startResult.setMessage("训练启动失败: " + execResult.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("启动远端训练失败: {}", e.getMessage(), e);
            startResult.setMessage("启动远端训练失败: " + e.getMessage());
        }
        return startResult;
    }

    private String wrapWithBash(String command) {
        return String.format("bash -lc \"%s\"", command.replace("\"", "\\\""));
    }

    /**
     * Execute SSH command, retry once with default SSH configuration if authentication fails.
     */
    private SSHService.SSHExecutionResult executeWithFallback(RemoteServer server, String command) {
        String host = server != null ? server.getServerIp() : sshHost;
        Integer port = server != null && server.getServerPort() != null ? server.getServerPort() : sshPort;
        String username = server != null ? server.getUsername() : sshUsername;
        String password = server != null ? decryptPassword(server.getPassword()) : sshPassword;

        SSHService.SSHExecutionResult result = sshService.executeCommand(host, port, username, password, command);
        if (shouldRetryWithDefault(result)) {
            boolean sameAsDefault = java.util.Objects.equals(host, sshHost)
                    && java.util.Objects.equals(port, sshPort)
                    && java.util.Objects.equals(username, sshUsername)
                    && java.util.Objects.equals(password, sshPassword);
            if (!sameAsDefault) {
                log.warn("SSH auth failed for {}@{}:{}, retrying with default ssh config", username, host, port);
                result = sshService.executeCommand(sshHost, sshPort, sshUsername, sshPassword, command);
            }
        }
        return result;
    }

    private boolean shouldRetryWithDefault(SSHService.SSHExecutionResult result) {
        if (result == null || result.isSuccess()) {
            return false;
        }
        String err = result.getErrorMsg();
        if (err == null) {
            return false;
        }
        String lower = err.toLowerCase();
        return lower.contains("auth fail") || lower.contains("authentication") || lower.contains("permission denied");
    }

    /**
     * Process model files after training completion
     */
    private String processTrainingResult(Long taskId, RemoteServer server, String trainType, String taskName) {
        try {
            String taskFolder = (trainType == null || trainType.isEmpty()) ? "detect" : trainType;
            String findResultCommand = String.format(
                    "source ~/.bashrc && source /data/work/anaconda3/etc/profile.d/conda.sh && " +
                            "cd %s && find runs/%s -name 'train*' -type d | sort -V | tail -1",
                    server.getWorkDir(),
                    taskFolder
            );

            SSHService.SSHExecutionResult findResult = executeWithFallback(server, findResultCommand);

            if (findResult.isSuccess() && findResult.getOutput() != null && !findResult.getOutput().trim().isEmpty()) {
                String resultDir = findResult.getOutput().trim();
                log.info("找到最新训练结果目录: {}", resultDir);

                String finalTaskName = (taskName != null && !taskName.isEmpty()) ? taskName : ("training_" + taskId);

                String processModelCommand = String.format(
                        "cd %s && " +
                                "if [ -f %s/weights/best.pt ]; then " +
                                "cp %s/weights/best.pt %s/weights/%s.pt && " +
                                "echo 'Model saved: %s/weights/%s.pt'; " +
                                "else echo 'Model file not found in %s/weights/'; fi",
                        server.getWorkDir(),
                        resultDir,
                        resultDir, resultDir, finalTaskName,
                        resultDir, finalTaskName,
                        resultDir
                );

                SSHService.SSHExecutionResult processResult = executeWithFallback(server, processModelCommand);

                if (processResult.isSuccess()) {
                    String modelPath = resultDir + "/weights/" + finalTaskName + ".pt";
                    AlgorithmTraining updateTraining = new AlgorithmTraining();
                    updateTraining.setId(taskId);
                    updateTraining.setModelOutputPath(modelPath);
                    updateTraining.setTrainStatus("completed");
                    updateTraining.setProgress(100);
                    updateTraining.setEndTime(LocalDateTime.now());
                    algorithmTrainingService.updateAlgorithmTraining(updateTraining);
                    log.info("模型已就绪，路径: {}", modelPath);
                    return modelPath;
                } else {
                    log.error("模型处理失败: {}", processResult.getErrorMsg());
                    return null;
                }

            } else {
                log.warn("未找到训练输出目录");
                return null;
            }

        } catch (Exception e) {
            log.error("处理训练结果失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get training progress
     */
    public LogResult getTrainingLogs(Long taskId, String logPath, String trainType, String taskName, int lines) {
        LogResult logResult = new LogResult();
        try {
            RemoteServer server = remoteServerMapper.selectActiveServer();
            if (server == null) {
                logResult.setMessage("未找到可用的训练服务器");
                return logResult;
            }

            String resolvedLogPath = (logPath == null || logPath.isEmpty())
                    ? server.getWorkDir() + "/" + DEFAULT_LOG_DIR + "/training_" + taskId + ".log"
                    : logPath;
            String tailCmd = wrapWithBash(String.format("tail -n %d %s", lines, resolvedLogPath));
            SSHService.SSHExecutionResult result = executeWithFallback(server, tailCmd);

            logResult.setLogPath(resolvedLogPath);
            if (result.isSuccess()) {
                logResult.setLogContent(result.getOutput());
                TrainingProgress progress = parseTrainingLog(result.getOutput(), taskId);
                logResult.setCurrentEpoch(progress.getCurrentEpoch());
                logResult.setTotalEpoch(progress.getTotalEpochs());
                logResult.setProgress(progress.getPercentage());
                logResult.setCompleted(progress.isCompleted());

                if (logResult.isCompleted()) {
                    String modelPath = processTrainingResult(taskId, server, trainType, taskName);
                    logResult.setModelPath(modelPath);
                    AlgorithmTraining update = new AlgorithmTraining();
                    update.setId(taskId);
                    update.setTrainStatus("completed");
                    update.setProgress(100);
                    update.setEndTime(LocalDateTime.now());
                    if (modelPath != null) {
                        update.setModelOutputPath(modelPath);
                    }
                    algorithmTrainingService.updateAlgorithmTraining(update);
                    logResult.setStatus("completed");
                } else if (logResult.getProgress() != null) {
                    AlgorithmTraining update = new AlgorithmTraining();
                    update.setId(taskId);
                    update.setProgress(logResult.getProgress());
                    algorithmTrainingService.updateAlgorithmTraining(update);
                }
            } else {
                logResult.setMessage("读取日志失败: " + result.getErrorMsg());
                logResult.setLogContent(result.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("处理训练结果失败: {}", e.getMessage(), e);
            logResult.setMessage("处理训练结果失败：" + e.getMessage());
        }
        return logResult;
    }

    /**
     * Stop training task
     */
    public boolean stopTraining(Long taskId, String logPath) {
        try {
            RemoteServer server = remoteServerMapper.selectActiveServer();
            if (server == null) {
                return false;
            }
            String resolvedLogPath = (logPath == null || logPath.isEmpty())
                    ? server.getWorkDir() + "/" + DEFAULT_LOG_DIR + "/training_" + taskId + ".log"
                    : logPath;
            String stopCmd = wrapWithBash(
                    "ps -ef | grep '" + resolvedLogPath + "' | grep -v grep | awk '{print $2}' | xargs -r kill -9"
            );
            SSHService.SSHExecutionResult result = executeWithFallback(server, stopCmd);
            log.info("停止训练任务{}: {}", taskId, result.getOutput());
            return result.isSuccess();
        } catch (Exception e) {
            log.error("处理训练结果失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get training progress
     */
    public TrainingProgress getProgress(Long taskId, String logPath) {
        try {
            RemoteServer server = remoteServerMapper.selectActiveServer();
            if (server == null) {
                log.error("未找到训练服务器");
                return null;
            }
            String resolvedLogPath = (logPath == null || logPath.isEmpty())
                    ? server.getWorkDir() + "/" + DEFAULT_LOG_DIR + "/training_" + taskId + ".log"
                    : logPath;
            String command = wrapWithBash("tail -n 50 " + resolvedLogPath);
            SSHService.SSHExecutionResult result = executeWithFallback(server, command);

            if (result.isSuccess()) {
                return parseTrainingLog(result.getOutput(), taskId);
            } else {
                log.error("获取训练进度失败: {}", result.getErrorMsg());
                return null;
            }
        } catch (Exception e) {
            log.error("处理训练结果失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private TrainingProgress parseTrainingLog(String logContent, Long taskId) {
        TrainingProgress progress = new TrainingProgress();
        progress.setTaskId(taskId);

        Pattern epochPattern = Pattern.compile("(?i)epoch\\s+\\d+/\\d+");
        Matcher epochMatcher = epochPattern.matcher(logContent);
        if (epochMatcher.find()) {
            int currentEpoch = Integer.parseInt(epochMatcher.group(1));
            int totalEpochs = Integer.parseInt(epochMatcher.group(2));
            progress.setCurrentEpoch(currentEpoch);
            progress.setTotalEpochs(totalEpochs);
            int percentage = (int) Math.round((double) currentEpoch / totalEpochs * 100);
            progress.setPercentage(percentage);
        }

        if (logContent.contains("Training complete") || logContent.contains("Results saved") || logContent.contains("best.pt")) {
            progress.setCompleted(true);
            progress.setPercentage(100);
        }

        Pattern lossPattern = Pattern.compile("loss[:=]\\s*(\\d+\\.?\\d*)");
        Matcher lossMatcher = lossPattern.matcher(logContent);
        if (lossMatcher.find()) {
            try {
                progress.setLatestLoss(Float.parseFloat(lossMatcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return progress;
    }

    @PostConstruct
    public void initDefaultServer() {
        try {
            // 先尝试创建表（如果不存在）
            try {
                remoteServerMapper.createTableIfNotExists();
                log.info("远程服务器配置表检查完成");
            } catch (Exception e) {
                log.warn("创建远程服务器配置表失败: {}", e.getMessage());
            }

            // 如果没有配置服务器，则添加默认服务器配置
            if (remoteServerMapper.count() == 0) {
                RemoteServer server = new RemoteServer();
                server.setServerName("YOLOv8训练服务器");
                server.setServerIp(sshHost);
                server.setServerPort(sshPort);
                server.setUsername(sshUsername);
                server.setPassword(encryptPassword(sshPassword));
                server.setCondaEnv("yolo8");
                server.setWorkDir("/data/work/ultralytics_yolov8-main");
                server.setStatus(1);
                remoteServerMapper.insertRemoteServer(server);
                log.info("初始化默认服务器配置成功");
            } else {
                log.info("远程服务器配置已存在，跳过初始化");
            }
        } catch (Exception e) {
            // 不阻止应用启动，只记录警告
            log.warn("初始化默认服务器配置失败，跳过此步骤: {}", e.getMessage());
        }
    }

    /**
     * Encrypt password (should use more secure encryption in actual implementation)
     */
    private String encryptPassword(String password) {
        return Base64.getEncoder().encodeToString(password.getBytes());
    }

    /**
     * Decrypt password
     */
    private String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(encryptedPassword));
        } catch (IllegalArgumentException e) {
            // 如果不是Base64编码，则直接返回原始值，避免因格式问题导致认证失败
            log.warn("Remote server password is not Base64 encoded, using raw value.");
            return encryptedPassword;
        }
    }

    /**
     * Training progress class
     */
    @Data
    public static class StartResult {
        private String logPath;
        private String command;
        private String pid;
        private String datasetPath;
        private String modelPath;
        private String trainType;
        private String message;
    }

    /**
     * Training progress class
     */
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
    }

    @Data
    public static class TrainingProgress {
        private Long taskId;
        private Integer currentEpoch;
        private Integer totalEpochs;
        private Integer percentage;
        private Float latestLoss;
        private boolean completed;
    }
} 
