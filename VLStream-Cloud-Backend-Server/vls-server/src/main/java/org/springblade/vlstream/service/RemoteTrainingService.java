package org.springblade.vlstream.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.constant.CommonConstant;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.enums.AlgorithmTrainingStatusEnum;
import org.springblade.vlstream.mapper.VlsRemoteServersMapper;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.entity.RemoteServers;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Remote training service class
 */
@Slf4j
@Service
public class RemoteTrainingService {

	@Resource
	private VlsRemoteServersMapper remoteServerMapper;

	@Resource
	private SSHService sshService;

	@Resource
	private IVlsAlgorithmTrainingService algorithmTrainingService;

	@Resource
	private VlsSshProperties sshProperties;

	private static final String DEFAULT_LOG_DIR = "logs";

	/**
	 * Start remote YOLO training (run in the background, logs saved to disk)
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
			RemoteServers server = remoteServerMapper.selectActiveServer();
			if (server == null) {
				startResult.setMessage("No available training server found");
				return startResult;
			}

			String logPath = server.getWorkDir() + "/" + DEFAULT_LOG_DIR + "/training_" + taskId + ".log";
			String task = (taskType == null || taskType.trim().isEmpty()) ? "detect" : taskType.trim();

			StringBuilder cmd = new StringBuilder();
			cmd.append("source ~/.bashrc && ");
			cmd.append("source /data/work/anaconda3/etc/profile.d/conda.sh && ");
			cmd.append("cd ").append(server.getWorkDir()).append(" && ");
			cmd.append("mkdir -p ").append(DEFAULT_LOG_DIR).append(" && ");
			cmd.append("rm -f ").append(logPath).append(" && ");
			cmd.append("conda activate ").append(server.getCondaEnv()).append(" && ");
			cmd.append("nohup yolo ").append(task).append(" train");
			cmd.append(" data=").append(datasetPath);
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

			startResult.setCommand(wrappedCmd);
			startResult.setLogPath(logPath);
			startResult.setDatasetPath(datasetPath);
			startResult.setTrainType(task);
			startResult.setMessage("Training command submitted, running in background.");

			submitTrainingAsync(taskId, server, wrappedCmd);
		} catch (Exception e) {
			log.error("Failed to start remote training: {}", e.getMessage(), e);
			startResult.setMessage("Failed to start remote training:" + e.getMessage());
		}
		return startResult;
	}

	private String wrapWithBash(String command) {
		return String.format("bash -lc \"%s\"", command.replace("\"", "\\\""));
	}

	private void submitTrainingAsync(Long taskId, RemoteServers server, String command) {
		Thread trainingThread = new Thread(() -> {
			try {
				SSHService.SSHExecutionResult execResult = executeWithFallback(server, command);
				if (execResult != null && execResult.isSuccess()) {
					String pid = execResult.getOutput() != null ? execResult.getOutput().trim().replaceAll("\\s+", "") : "";
					if (pid != null && !pid.isEmpty()) {
						log.info("Training command submitted: taskId={}, pid={}", taskId, pid);
					} else {
						log.info("Training command submitted without pid: taskId={}", taskId);
					}
					return;
				}
				String errorMessage = execResult == null ? "SSH execution result is null" : execResult.getErrorMsg();
				log.warn("Training command failed: taskId={}, error={}", taskId, errorMessage);
				markTrainingFailed(taskId, errorMessage);
			} catch (Exception exception) {
				log.error("Async training start failed: taskId={}, error={}", taskId, exception.getMessage(), exception);
				markTrainingFailed(taskId, exception.getMessage());
			}
		});
		trainingThread.setName("remote-training-start-" + taskId);
		trainingThread.setDaemon(true);
		trainingThread.start();
	}

	private void markTrainingFailed(Long taskId, String errorMessage) {
		if (taskId == null) {
			return;
		}
		String resolvedError = errorMessage == null ? "Training command failed." : errorMessage;
		AlgorithmTraining update = new AlgorithmTraining();
		update.setId(taskId);
		update.setTrainStatus(AlgorithmTrainingStatusEnum.failed);
		update.setErrorMessage(resolvedError);
		update.setEndTime(new Date());
		algorithmTrainingService.updateAlgorithmTraining(update);
	}

	/**
	 * Execute SSH command; if authentication fails, retry once using the SSH account from the default configuration.
	 */
	private SSHService.SSHExecutionResult executeWithFallback(RemoteServers server, String command) {
		String host = server != null ? server.getServerIp() : sshProperties.getHost();
		Integer port = server != null && server.getServerPort() != null ? server.getServerPort() : sshProperties.getPort();
		String username = server != null ? server.getUsername() : sshProperties.getUsername();
		String password = server != null ? decryptPassword(server.getPassword()) : sshProperties.getPassword();

		SSHService.SSHExecutionResult result = sshService.executeCommand(host, port, username, password, command);
		if (shouldRetryWithDefault(result)) {
			boolean sameAsDefault = Objects.equals(host, sshProperties.getHost())
				&& Objects.equals(port, sshProperties.getPort())
				&& Objects.equals(username, sshProperties.getUsername())
				&& Objects.equals(password, sshProperties.getPassword());
			if (!sameAsDefault) {
				log.warn("SSH auth failed for {}@{}:{}, retrying with default ssh config", username, host, port);
				result = sshService.executeCommand(sshProperties.getHost(), sshProperties.getPort(), sshProperties.getUsername(), sshProperties.getPassword(), command);
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
	 * Process the model file after training is completed
	 */
	private String processTrainingResult(Long taskId, RemoteServers server, String trainType, String taskName) {
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
				log.info("Found the latest training result directory: {}", resultDir);

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
					String modelPath = CommonConstant.BASE_DATASETS_PATH + resultDir + "/weights/" + finalTaskName + ".pt";
					AlgorithmTraining updateTraining = new AlgorithmTraining();
					updateTraining.setId(taskId);
					updateTraining.setModelOutputPath(modelPath);
					updateTraining.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
					updateTraining.setProgress(100);
					updateTraining.setEndTime(new Date());
					algorithmTrainingService.updateAlgorithmTraining(updateTraining);
					log.info("Model is ready, path: {}", modelPath);
					return modelPath;
				} else {
					log.error("Model processing failed: {}", processResult.getErrorMsg());
					return null;
				}

			} else {
				log.warn("Training output directory not found");
				return null;
			}

		} catch (Exception e) {
			log.error("Failed to process training result: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Get training progress
	 */
	public LogResult getTrainingLogs(Long taskId, String logPath, String trainType, String taskName, int lines) {
		LogResult logResult = new LogResult();
		try {
			RemoteServers server = remoteServerMapper.selectActiveServer();
			if (server == null) {
				logResult.setMessage("No available training server found");
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
					update.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
					update.setProgress(100);
					update.setEndTime(new Date());
					if (modelPath != null) {
						update.setModelOutputPath(modelPath);
					}
					algorithmTrainingService.updateAlgorithmTraining(update);
					logResult.setStatus(AlgorithmTrainingStatusEnum.completed.getCode());
				} else if (logResult.getProgress() != null) {
					AlgorithmTraining update = new AlgorithmTraining();
					update.setId(taskId);
					update.setProgress(logResult.getProgress());
					algorithmTrainingService.updateAlgorithmTraining(update);
				}
			} else {
				logResult.setMessage("Failed to read log:" + result.getErrorMsg());
				logResult.setLogContent(result.getErrorMsg());
			}
		} catch (Exception e) {
			log.error("Failed to process training result: {}", e.getMessage(), e);
			logResult.setMessage("Failed to process training result:" + e.getMessage());
		}
		return logResult;
	}

	public String exportModel(String modelPath, String format) {
		RemoteServers server = remoteServerMapper.selectActiveServer();
		if (server == null) {
			log.warn("No active remote server, skip model export: format={}, modelPath={}", format, modelPath);
			return null;
		}
		return exportModel(server, modelPath, format);
	}

	private String exportModel(RemoteServers server, String modelPath, String format) {
		if (server == null) {
			return null;
		}
		if (modelPath == null || modelPath.isEmpty()) {
			return null;
		}
		if (format == null || format.trim().isEmpty()) {
			return null;
		}
		String normalizedFormat = format.trim().toLowerCase();
		String exportPath = resolveExportPath(modelPath, normalizedFormat);
		if (exportPath == null) {
			return null;
		}

		String condaEnv = server.getCondaEnv();
		StringBuilder commandBuilder = new StringBuilder();
		commandBuilder.append("source ~/.bashrc && ");
		commandBuilder.append("source /data/work/anaconda3/etc/profile.d/conda.sh && ");
		commandBuilder.append("cd ").append(server.getWorkDir()).append(" && ");
		if (condaEnv != null && !condaEnv.isEmpty()) {
			commandBuilder.append("conda activate ").append(condaEnv).append(" && ");
		}
		commandBuilder.append("yolo export model=").append(modelPath);
		commandBuilder.append(" format=").append(normalizedFormat);
		commandBuilder.append(" && ");
		commandBuilder.append("if [ -f ").append(exportPath);
		commandBuilder.append(" ]; then echo 'Model exported: ").append(exportPath);
		commandBuilder.append("'; else echo 'Model export failed'; exit 1; fi");

		String wrappedCommand = wrapWithBash(commandBuilder.toString());
		SSHService.SSHExecutionResult exportResult = executeWithFallback(server, wrappedCommand);
		if (exportResult.isSuccess()) {
			return exportPath;
		}
		log.warn("Model export failed: format={}, error={}", normalizedFormat, exportResult.getErrorMsg());
		return null;
	}

	private String resolveExportPath(String modelPath, String format) {
		if (modelPath == null || modelPath.isEmpty()) {
			return null;
		}
		if (format == null || format.isEmpty()) {
			return null;
		}
		String lowerModelPath = modelPath.toLowerCase();
		if (lowerModelPath.endsWith(".pt")) {
			return modelPath.substring(0, modelPath.length() - 3) + "." + format;
		}
		return modelPath + "." + format;
	}

	public void generateOnnxSynsetFile(Long trainingId, String onnxModelPath, String datasetYamlPath) {
		if (onnxModelPath == null || onnxModelPath.trim().isEmpty()) {
			log.warn("Skip synset generation, onnx path is empty: trainingId={}", trainingId);
			return;
		}
		if (datasetYamlPath == null || datasetYamlPath.trim().isEmpty()) {
			log.warn("Skip synset generation, dataset path is empty: trainingId={}, onnxPath={}", trainingId, onnxModelPath);
			return;
		}

		RemoteServers server = remoteServerMapper.selectActiveServer();
		if (server == null) {
			log.warn("No active remote server, skip synset generation: trainingId={}, onnxPath={}", trainingId, onnxModelPath);
			return;
		}

		String yamlContent = readRemoteFile(server, datasetYamlPath);
		if (yamlContent == null || yamlContent.trim().isEmpty()) {
			log.warn("Dataset yaml is empty, skip synset generation: trainingId={}, datasetYamlPath={}", trainingId, datasetYamlPath);
			return;
		}

		List<String> labelNames = parseLabelNamesFromYaml(yamlContent);
		if (labelNames.isEmpty()) {
			log.warn("No label names found in dataset yaml, skip synset generation: trainingId={}, datasetYamlPath={}", trainingId, datasetYamlPath);
			return;
		}

		String synsetContent = buildSynsetContent(labelNames);
		if (synsetContent.isEmpty()) {
			log.warn("Synset content is empty, skip synset generation: trainingId={}, datasetYamlPath={}", trainingId, datasetYamlPath);
			return;
		}

		String synsetPath = resolveSynsetPath(onnxModelPath);
		boolean written = writeRemoteFile(server, synsetPath, synsetContent);
		if (written) {
			log.info("Synset file generated: trainingId={}, synsetPath={}", trainingId, synsetPath);
		} else {
			log.warn("Synset file generation failed: trainingId={}, synsetPath={}", trainingId, synsetPath);
		}
	}

	private String resolveSynsetPath(String onnxModelPath) {
		String trimmed = onnxModelPath.trim();
		int lastSlash = trimmed.lastIndexOf('/');
		if (lastSlash < 0) {
			return CommonConstant.SYNSET_TXT;
		}
		return trimmed.substring(0, lastSlash + 1) + CommonConstant.SYNSET_TXT;
	}

	private String readRemoteFile(RemoteServers server, String filePath) {
		String command = wrapWithBash(String.format("if [ -f '%s' ]; then cat '%s'; fi", filePath, filePath));
		SSHService.SSHExecutionResult result = executeWithFallback(server, command);
		if (result == null || !result.isSuccess()) {
			String errorMessage = result == null ? null : result.getErrorMsg();
			log.warn("Read remote file failed: path={}, error={}", filePath, errorMessage);
			return null;
		}
		return result.getOutput();
	}

	private boolean writeRemoteFile(RemoteServers server, String filePath, String content) {
		if (content == null) {
			return false;
		}
		String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
		String command = wrapWithBash(String.format("printf '%%s' '%s' | base64 -d > '%s'", encodedContent, filePath));
		SSHService.SSHExecutionResult result = executeWithFallback(server, command);
		if (result == null || !result.isSuccess()) {
			String errorMessage = result == null ? null : result.getErrorMsg();
			log.warn("Write remote file failed: path={}, error={}", filePath, errorMessage);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private List<String> parseLabelNamesFromYaml(String yamlContent) {
		if (yamlContent == null || yamlContent.trim().isEmpty()) {
			return Collections.emptyList();
		}
		Yaml yaml = new Yaml();
		Object yamlObject = yaml.load(yamlContent);
		if (!(yamlObject instanceof Map)) {
			return Collections.emptyList();
		}
		Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
		Object namesObject = yamlMap.get("names");
		return parseLabelNames(namesObject);
	}

	private List<String> parseLabelNames(Object namesObject) {
		if (namesObject == null) {
			return Collections.emptyList();
		}
		if (namesObject instanceof List<?>) {
			List<?> rawList = (List<?>) namesObject;
			return rawList.stream()
				.map(item -> item == null ? null : item.toString().trim())
				.filter(value -> value != null && !value.isEmpty())
				.collect(Collectors.toList());
		}
		if (namesObject instanceof Map<?, ?>) {
			Map<?, ?> rawMap = (Map<?, ?>) namesObject;
			Map<Integer, String> sortedMap = new TreeMap<>();
			for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
				Integer index = parseInteger(entry.getKey());
				String labelName = entry.getValue() == null ? null : entry.getValue().toString().trim();
				if (index != null && labelName != null && !labelName.isEmpty()) {
					sortedMap.put(index, labelName);
				}
			}
			List<String> names = new ArrayList<>();
			for (Map.Entry<Integer, String> entry : sortedMap.entrySet()) {
				int index = entry.getKey();
				while (names.size() < index) {
					names.add("class_" + names.size());
				}
				if (names.size() == index) {
					names.add(entry.getValue());
				} else {
					names.set(index, entry.getValue());
				}
			}
			return names;
		}
		String nameValue = namesObject.toString().trim();
		if (nameValue.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> names = new ArrayList<>();
		names.add(nameValue);
		return names;
	}

	private Integer parseInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return Integer.parseInt(((String) value).trim());
			} catch (NumberFormatException ignoredException) {
				return null;
			}
		}
		return null;
	}

	private String buildSynsetContent(List<String> labelNames) {
		return labelNames.stream()
			.filter(name -> name != null && !name.trim().isEmpty())
			.collect(Collectors.joining("\n"));
	}

	public boolean stopTraining(Long taskId, String logPath) {
		try {
			RemoteServers server = remoteServerMapper.selectActiveServer();
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
			log.info("Stop training task {}: {}", taskId, result.getOutput());
			return result.isSuccess();
		} catch (Exception e) {
			log.error("Failed to process training result: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Get training progress
	 */
	public TrainingProgress getProgress(Long taskId, String logPath) {
		try {
			RemoteServers server = remoteServerMapper.selectActiveServer();
			if (server == null) {
				log.error("Training server not found");
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
				log.error("Failed to get training progress: {}", result.getErrorMsg());
				return null;
			}
		} catch (Exception e) {
			log.error("Failed to process training result: {}", e.getMessage(), e);
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
			// Try to create the table first (if it does not exist)
			try {
				remoteServerMapper.createTableIfNotExists();
				log.info("Remote Server Configuration Table check completed");
			} catch (Exception e) {
				log.warn("Failed to create remote server configuration table: {}", e.getMessage());
			}

			// If no server is configured, add default server configuration
			if (remoteServerMapper.count() == 0) {
				RemoteServers server = new RemoteServers();
				server.setServerName("YOLOv8 training server");
				server.setServerIp(sshProperties.getHost());
				server.setServerPort(sshProperties.getPort());
				server.setUsername(sshProperties.getUsername());
				server.setPassword(encryptPassword(sshProperties.getPassword()));
				server.setCondaEnv("yolo8");
				server.setWorkDir("/data/work/ultralytics_yolov8-main/datasets");
				server.setStatus(1);
				remoteServerMapper.insert(server);
				log.info("Default server configuration initialized successfully");
			} else {
				log.info("Remote server configuration already exists, skipping initialization");
			}
		} catch (Exception e) {
			// Do not block application startup, only log warning
			log.warn("Failed to initialize default server configuration, skipping this step: {}", e.getMessage());
		}
	}

	/**
	 * Encrypt password (a more secure encryption method should be used in actual implementation)
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
			// If not Base64 encoded, return original value directly to avoid authentication failure due to formatting issues
			log.warn("Remote server password is not Base64 encoded, using raw value.");
			return encryptedPassword;
		}
	}

	/**
	 * Training Progress Class
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
	 * Training Progress Class
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
