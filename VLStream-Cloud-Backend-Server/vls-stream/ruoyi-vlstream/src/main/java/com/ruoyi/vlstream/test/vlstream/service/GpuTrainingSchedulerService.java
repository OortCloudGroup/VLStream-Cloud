/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsTrainingContainerProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRemoteServersMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight single-node scheduler. GPU 0 is exclusively assigned to one
 * training container; additional jobs remain persisted in queued state.
 */
@Slf4j
@Service
public class GpuTrainingSchedulerService {

	private static final String TYPE_TRAINING = "training";
	private static final String STATUS_QUEUED = "queued";
	private static final String STATUS_STARTING = "starting";
	private static final String STATUS_RUNNING = "running";
	private static final String STATUS_COMPLETED = "completed";
	private static final String STATUS_ERROR = "error";

	@Resource
	private IVlsContainerInstanceService containerInstanceService;
	@Resource
	private IVlsAlgorithmTrainingService algorithmTrainingService;
	@Resource
	private VlsRemoteServersMapper remoteServersMapper;
	@Resource
	private SSHService sshService;
	@Resource
	private VlsSshProperties sshProperties;
	@Resource
	private VlsTrainingContainerProperties properties;
	@Resource
	private RemoteTrainingService remoteTrainingService;
	@Resource
	private ObjectMapper objectMapper;

	private ScheduledExecutorService executor;

	@PostConstruct
	public void initialize() {
		executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "gpu-training-scheduler");
			thread.setDaemon(true);
			return thread;
		});
		executor.scheduleWithFixedDelay(this::scheduleSafely, 1,
			Math.max(1000L, properties.getScheduleIntervalMillis()), TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void shutdown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	public RemoteTrainingService.StartResult enqueue(String taskType,
													 Long taskId,
													 String datasetPath,
													 String baseModel,
													 Integer epochs,
													 Integer batchSize,
													 Integer imgSize) {
		List<ContainerInstance> existing = containerInstanceService.list(
			new LambdaQueryWrapper<ContainerInstance>()
				.eq(ContainerInstance::getTrainingTaskId, taskId)
				.eq(ContainerInstance::getInstanceType, TYPE_TRAINING)
				.in(ContainerInstance::getInstanceStatus, STATUS_QUEUED, STATUS_STARTING, STATUS_RUNNING)
				.orderByDesc(ContainerInstance::getQueueTime)
				.last("limit 1"));
		if (!existing.isEmpty()) {
			RemoteTrainingService.StartResult duplicate = new RemoteTrainingService.StartResult();
			duplicate.setLogPath(existing.get(0).getLogsPath());
			duplicate.setDatasetPath(datasetPath);
			duplicate.setTrainType(taskType);
			duplicate.setMessage("训练任务已在GPU队列中");
			return duplicate;
		}
		RemoteServers server = requireServer();
		String workDir = server.getWorkDir();
		String logPath = workDir + "/logs/training_" + taskId + ".log";
		String containerName = "vls-training-" + taskId;
		String command = buildDockerRunCommand(server, containerName, logPath, taskType,
			datasetPath, baseModel, epochs, batchSize, imgSize);

		ContainerInstance instance = new ContainerInstance();
		instance.setInstanceName(containerName);
		instance.setImageName(properties.getImage());
		instance.setImageType("training");
		instance.setImageTag(properties.getImage());
		instance.setInstanceCount(1);
		instance.setInstanceType(TYPE_TRAINING);
		instance.setCpuLimit(properties.getCpuLimit() + " cores");
		instance.setMemoryLimit(properties.getMemoryLimit());
		instance.setGpuLimit("GPU " + properties.getGpuIndex() + " (exclusive)");
		String resolvedTaskType = isBlank(taskType) ? "detect" : taskType.trim();
		instance.setPortConfig(toJson(singletonMap("trainType", resolvedTaskType)));
		instance.setEnvConfig(toJson(singletonMap("launchCommand", command)));
		Map<String, Object> volumeConfig = new LinkedHashMap<>();
		volumeConfig.put("hostPath", properties.getHostDataDir());
		volumeConfig.put("containerPath", properties.getHostDataDir());
		instance.setVolumeConfig(toJson(volumeConfig));
		instance.setInstanceStatus(STATUS_QUEUED);
		instance.setHealthStatus("unknown");
		instance.setLogsPath(logPath);
		instance.setTrainingTaskId(taskId);
		instance.setServerId(server.getId());
		instance.setServerIp(server.getServerIp());
		instance.setGpuIndex(properties.getGpuIndex());
		instance.setGpuUuid(properties.getGpuUuid());
		instance.setQueueTime(new Date());
		instance.setRestartCount(0);
		if (!containerInstanceService.save(instance)) {
			throw new IllegalStateException("保存GPU排队任务失败");
		}

		RemoteTrainingService.StartResult result = new RemoteTrainingService.StartResult();
		result.setLogPath(logPath);
		result.setDatasetPath(datasetPath);
		result.setTrainType(taskType);
		result.setMessage("训练任务已进入GPU队列");
		return result;
	}

	public Map<String, Object> getResourceSnapshot() {
		String command = "printf 'HOST|'; hostname; "
			+ "printf 'CPU|'; nproc; "
			+ "printf 'MEM|'; awk '/MemTotal/{printf \"%.1f\", $2/1024/1024}' /proc/meminfo; echo; "
			+ "nvidia-smi --query-gpu=index,uuid,name,memory.total,memory.used,utilization.gpu "
			+ "--format=csv,noheader,nounits | sed 's/^/GPU|/'";
		SSHService.SSHExecutionResult result = execute(command);
		if (!result.isSuccess()) {
			throw new IllegalStateException("读取GPU服务器资源失败: " + result.getErrorMsg());
		}
		Map<String, Object> snapshot = new LinkedHashMap<>();
		for (String line : result.getOutput().split("\\r?\\n")) {
			if (line.startsWith("HOST|")) {
				snapshot.put("hostName", line.substring(5).trim());
			} else if (line.startsWith("CPU|")) {
				snapshot.put("cpuCores", Integer.valueOf(line.substring(4).trim()));
			} else if (line.startsWith("MEM|")) {
				snapshot.put("memoryGb", new BigDecimal(line.substring(4).trim()));
			} else if (line.startsWith("GPU|")) {
				String[] values = line.substring(4).split(",\\s*");
				if (values.length >= 6) {
					snapshot.put("gpuIndex", Integer.valueOf(values[0]));
					snapshot.put("gpuUuid", values[1]);
					snapshot.put("gpuName", values[2]);
					snapshot.put("gpuMemoryTotalMb", Integer.valueOf(values[3]));
					snapshot.put("gpuMemoryUsedMb", Integer.valueOf(values[4]));
					snapshot.put("gpuUsage", Integer.valueOf(values[5]));
				}
			}
		}
		snapshot.put("serverIp", sshProperties.getHost());
		snapshot.put("schedulerMode", "single-gpu-exclusive");
		snapshot.put("queueLength", countByStatus(STATUS_QUEUED));
		snapshot.put("busy", countByStatus(STATUS_RUNNING) + countByStatus(STATUS_STARTING) > 0);
		return snapshot;
	}

	public String getContainerLogs(Long id, int lines) {
		ContainerInstance instance = containerInstanceService.getById(id);
		if (instance == null || isBlank(instance.getLogsPath())) {
			throw new IllegalArgumentException("容器任务或日志路径不存在");
		}
		int safeLines = Math.max(1, Math.min(lines, 2000));
		SSHService.SSHExecutionResult result = execute(
			"tail -n " + safeLines + " " + shellQuote(instance.getLogsPath()) + " 2>/dev/null || true");
		if (!result.isSuccess()) {
			throw new IllegalStateException("读取训练日志失败: " + result.getErrorMsg());
		}
		return result.getOutput();
	}

	private void scheduleSafely() {
		try {
			reconcileActiveContainer();
			dispatchNext();
		} catch (Exception exception) {
			log.error("GPU scheduler iteration failed: {}", exception.getMessage(), exception);
		}
	}

	private synchronized void dispatchNext() {
		if (countByStatus(STATUS_RUNNING) + countByStatus(STATUS_STARTING) > 0 || !isGpuIdle()) {
			return;
		}
		List<ContainerInstance> queued = containerInstanceService.list(
			new LambdaQueryWrapper<ContainerInstance>()
				.eq(ContainerInstance::getInstanceType, TYPE_TRAINING)
				.eq(ContainerInstance::getInstanceStatus, STATUS_QUEUED)
				.orderByAsc(ContainerInstance::getQueueTime)
				.last("limit 1"));
		if (queued.isEmpty()) {
			return;
		}
		ContainerInstance instance = queued.get(0);
		updateStatus(instance, STATUS_STARTING, null, null);
		String launchCommand = readJsonText(instance.getEnvConfig(), "launchCommand");
		if (isBlank(launchCommand)) {
			fail(instance, "训练容器启动命令不存在");
			return;
		}
		SSHService.SSHExecutionResult start = execute(launchCommand);
		if (!start.isSuccess() || isBlank(start.getOutput())) {
			fail(instance, "Docker容器启动失败: " + start.getErrorMsg());
			return;
		}
		updateStatus(instance, STATUS_RUNNING, start.getOutput().trim(), null);
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(instance.getTrainingTaskId());
		training.setTrainStatus(AlgorithmTrainingStatusEnum.training);
		training.setStartTime(new Date());
		algorithmTrainingService.updateAlgorithmTraining(training);
	}

	private void reconcileActiveContainer() {
		List<ContainerInstance> active = containerInstanceService.list(
			new LambdaQueryWrapper<ContainerInstance>()
				.eq(ContainerInstance::getInstanceType, TYPE_TRAINING)
				.in(ContainerInstance::getInstanceStatus, STATUS_STARTING, STATUS_RUNNING));
		for (ContainerInstance instance : active) {
			String inspect = "docker inspect -f '{{.State.Status}}|{{.State.ExitCode}}|{{.Id}}' "
				+ shellQuote(instance.getInstanceName()) + " 2>/dev/null || echo missing";
			SSHService.SSHExecutionResult stateResult = execute(inspect);
			String state = stateResult.getOutput() == null ? "" : stateResult.getOutput().trim();
			if (state.startsWith("running|")) {
				String[] fields = state.split("\\|");
				updateStatus(instance, STATUS_RUNNING, fields.length > 2 ? fields[2] : instance.getContainerId(), null);
				updateGpuUsage(instance);
				continue;
			}
			if (state.startsWith("exited|") || state.startsWith("dead|")) {
				String[] fields = state.split("\\|");
				int exitCode = fields.length > 1 ? Integer.parseInt(fields[1]) : 1;
				execute("docker rm -f " + shellQuote(instance.getInstanceName()) + " >/dev/null 2>&1 || true");
				if (exitCode == 0) {
					complete(instance);
				} else {
					fail(instance, "训练容器退出，exitCode=" + exitCode);
				}
			} else if ("missing".equals(state) && STATUS_RUNNING.equals(instance.getInstanceStatus())) {
				fail(instance, "训练容器不存在，可能被外部删除");
			}
		}
	}

	private void complete(ContainerInstance instance) {
		updateStatus(instance, STATUS_COMPLETED, instance.getContainerId(), null);
		AlgorithmTraining task = algorithmTrainingService.getById(instance.getTrainingTaskId());
		RemoteServers server = requireServer();
		String trainType = task == null ? "detect" : resolveTrainType(instance);
		String taskName = task == null ? null : task.getTaskName();
		String modelPath = remoteTrainingService.processTrainingResult(
			instance.getTrainingTaskId(), server, trainType, taskName);
		if (modelPath == null) {
			fail(instance, "训练完成，但未找到best.pt模型文件");
		}
	}

	private void fail(ContainerInstance instance, String message) {
		updateStatus(instance, STATUS_ERROR, instance.getContainerId(), message);
		AlgorithmTraining training = new AlgorithmTraining();
		training.setId(instance.getTrainingTaskId());
		training.setTrainStatus(AlgorithmTrainingStatusEnum.failed);
		training.setErrorMessage(message);
		training.setEndTime(new Date());
		algorithmTrainingService.updateAlgorithmTraining(training);
	}

	private void updateStatus(ContainerInstance source, String status, String containerId, String error) {
		ContainerInstance update = new ContainerInstance();
		update.setId(source.getId());
		update.setInstanceStatus(status);
		update.setHealthStatus(STATUS_RUNNING.equals(status) ? "healthy" : "unknown");
		update.setContainerId(containerId);
		update.setErrorMessage(error);
		if (STATUS_RUNNING.equals(status) && source.getStartTime() == null) {
			update.setStartTime(new Date());
		}
		if (STATUS_COMPLETED.equals(status) || STATUS_ERROR.equals(status)) {
			update.setStopTime(new Date());
		}
		containerInstanceService.updateById(update);
		source.setInstanceStatus(status);
		source.setContainerId(containerId);
	}

	private void updateGpuUsage(ContainerInstance instance) {
		SSHService.SSHExecutionResult usage = execute("nvidia-smi --query-gpu=utilization.gpu "
			+ "--format=csv,noheader,nounits -i " + properties.getGpuIndex());
		if (usage.isSuccess() && !isBlank(usage.getOutput())) {
			try {
				containerInstanceService.updateMonitoringData(instance.getId(), null, null,
					new BigDecimal(usage.getOutput().trim()));
			} catch (NumberFormatException ignored) {
				// Keep the last valid sample.
			}
		}
	}

	private boolean isGpuIdle() {
		SSHService.SSHExecutionResult result = execute(
			"nvidia-smi --query-compute-apps=pid --format=csv,noheader,nounits -i "
				+ properties.getGpuIndex() + " | sed '/^$/d' | wc -l");
		return result.isSuccess() && "0".equals(result.getOutput().trim());
	}

	private long countByStatus(String status) {
		return containerInstanceService.count(new LambdaQueryWrapper<ContainerInstance>()
			.eq(ContainerInstance::getInstanceType, TYPE_TRAINING)
			.eq(ContainerInstance::getInstanceStatus, status));
	}

	private String buildDockerRunCommand(RemoteServers server,
										 String containerName,
										 String logPath,
										 String taskType,
										 String datasetPath,
										 String baseModel,
										 Integer epochs,
										 Integer batchSize,
										 Integer imgSize) {
		String trainType = isBlank(taskType) ? "detect" : taskType.trim();
		String yolo = "/data/work/anaconda3/envs/" + server.getCondaEnv() + "/bin/yolo";
		StringBuilder train = new StringBuilder();
		train.append(yolo).append(" ").append(shellToken(trainType)).append(" train");
		train.append(" data=").append(shellQuote(datasetPath));
		train.append(" model=").append(shellQuote(baseModel));
		if (epochs != null) train.append(" epochs=").append(epochs);
		if (batchSize != null) train.append(" batch=").append(batchSize);
		if (imgSize != null) train.append(" imgsz=").append(imgSize);
		if (properties.getWorkers() != null) train.append(" workers=").append(properties.getWorkers());
		train.append("; rc=$?; if [ $rc -eq 0 ]; then echo 'Training complete'; ")
			.append("else echo 'Training failed'; fi; exit $rc");
		String loggedTraining = "{ " + train + "; } >> " + shellQuote(logPath) + " 2>&1";

		return "mkdir -p " + shellQuote(server.getWorkDir() + "/logs")
			+ " " + shellQuote(properties.getUltralyticsConfigDir())
			+ " && rm -f " + shellQuote(logPath)
			+ " && docker rm -f " + shellQuote(containerName) + " >/dev/null 2>&1 || true; "
			+ "docker run -d --name " + shellQuote(containerName)
			+ " --gpus device=" + properties.getGpuIndex()
			+ " --cpus " + shellToken(properties.getCpuLimit())
			+ " --memory " + shellToken(properties.getMemoryLimit())
			+ " --shm-size " + shellToken(properties.getShmSize())
			+ " --user $(id -u):$(id -g)"
			+ " -e HOME=/tmp -e YOLO_CONFIG_DIR=" + shellQuote(properties.getUltralyticsConfigDir())
			+ " -v " + shellQuote(properties.getHostDataDir() + ":" + properties.getHostDataDir())
			+ " -w " + shellQuote(server.getWorkDir())
			+ " " + shellQuote(properties.getImage())
			+ " /bin/bash -lc " + shellQuote(loggedTraining);
	}

	private String resolveTrainType(ContainerInstance instance) {
		String trainType = readJsonText(instance.getPortConfig(), "trainType");
		return isBlank(trainType) ? "detect" : trainType;
	}

	private Map<String, Object> singletonMap(String key, String value) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put(key, value);
		return result;
	}

	private String toJson(Map<String, Object> value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("序列化训练容器配置失败", exception);
		}
	}

	private String readJsonText(String json, String fieldName) {
		if (isBlank(json)) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(json);
			if (root != null && root.isObject()) {
				JsonNode value = root.get(fieldName);
				return value == null || value.isNull() ? null : value.asText();
			}
			return root != null && root.isTextual() ? root.asText() : null;
		} catch (JsonProcessingException exception) {
			log.warn("Invalid scheduler JSON config, field={}", fieldName);
			return null;
		}
	}

	private RemoteServers requireServer() {
		RemoteServers server = remoteServersMapper.selectActiveServer();
		if (server == null) {
			throw new IllegalStateException("未找到启用的GPU训练服务器");
		}
		return server;
	}

	private SSHService.SSHExecutionResult execute(String command) {
		return sshService.executeCommand(sshProperties.getHost(), sshProperties.getPort(),
			sshProperties.getUsername(), sshProperties.getPassword(), command);
	}

	private String shellQuote(String value) {
		if (value == null) return "''";
		return "'" + value.replace("'", "'\"'\"'") + "'";
	}

	private String shellToken(String value) {
		if (value == null || !value.matches("[A-Za-z0-9._:/+-]+")) {
			throw new IllegalArgumentException("非法命令参数");
		}
		return value;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
