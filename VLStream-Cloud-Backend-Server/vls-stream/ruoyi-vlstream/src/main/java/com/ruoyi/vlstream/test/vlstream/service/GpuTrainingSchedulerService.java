/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsTrainingContainerProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsContainerInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRemoteServersMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
	private VlsContainerInstanceMapper containerInstanceMapper;
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
	private ApplicationEventPublisher applicationEventPublisher;
	@Resource
	private ObjectMapper objectMapper;
	@Resource
	@org.springframework.context.annotation.Lazy
	private com.ruoyi.vlstream.test.vlstream.data.SmartAnnotationWorker smartAnnotationWorker;
	private static final String TYPE_SMART_ANNOTATION = "smart_annotation";

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
		String runName = "task_" + taskId + "_" + java.util.UUID.randomUUID().toString().replace("-", "");
		String runDirectory = workDir + "/runs/vls/" + runName;
		AlgorithmTraining task = algorithmTrainingService.getById(taskId);
		if (task == null) throw new IllegalStateException("训练任务不存在");
		try {
			Map<String,Object> config = isBlank(task.getConfigParams()) ? new LinkedHashMap<>()
				: objectMapper.readValue(task.getConfigParams(), new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {});
			config.put("runDirectory", runDirectory);
			config.put("trainType", isBlank(taskType) ? "detect" : taskType.trim());
			AlgorithmTraining run = new AlgorithmTraining(); run.setId(taskId); run.setConfigParams(objectMapper.writeValueAsString(config));
			if (algorithmTrainingService.updateAlgorithmTraining(run) <= 0) throw new IllegalStateException("保存本轮训练目录失败");
		} catch (java.io.IOException e) { throw new IllegalStateException("训练配置无法解析", e); }
		String logPath = workDir + "/logs/" + runName + ".log";
		String containerName = "vls-training-" + taskId;
		String command = buildDockerRunCommand(server, containerName, logPath, taskType,
			datasetPath, baseModel, epochs, batchSize, imgSize, runName);

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

	/** Shares the durable FIFO and exclusive GPU with formal training. No caller-supplied command. */
	public void enqueueSmartAnnotation(Long roundId) {
		if (containerInstanceService.count(new LambdaQueryWrapper<ContainerInstance>()
			.eq(ContainerInstance::getTrainingTaskId, roundId).eq(ContainerInstance::getInstanceType, TYPE_SMART_ANNOTATION)
			.in(ContainerInstance::getInstanceStatus, STATUS_QUEUED, STATUS_STARTING, STATUS_RUNNING)) > 0) return;
		RemoteServers server = requireServer();
		ContainerInstance instance = new ContainerInstance();
		instance.setTenantId(TenantContextHolder.getTenantId());
		instance.setInstanceName("vls-annotation-" + roundId + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
		instance.setInstanceType(TYPE_SMART_ANNOTATION); instance.setImageType(TYPE_SMART_ANNOTATION);
		instance.setImageName(properties.getImage()); instance.setImageTag(properties.getImage()); instance.setInstanceCount(1);
		instance.setTrainingTaskId(roundId); instance.setServerId(server.getId()); instance.setServerIp(server.getServerIp());
		instance.setGpuIndex(properties.getGpuIndex()); instance.setGpuUuid(properties.getGpuUuid()); instance.setGpuLimit("GPU " + properties.getGpuIndex() + " (exclusive)");
		instance.setInstanceStatus(STATUS_QUEUED); instance.setHealthStatus("unknown"); instance.setQueueTime(new Date()); instance.setRestartCount(0);
		if (!containerInstanceService.save(instance)) throw new IllegalStateException("智能标注任务入队失败");
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
			if (!reconcileActiveContainers()) {
				dispatchNext();
			}
		} catch (Exception exception) {
			log.error("GPU scheduler iteration failed: {}", exception.getMessage(), exception);
		}
	}

	private synchronized void dispatchNext() {
		ContainerInstance instance = containerInstanceMapper.selectNextQueuedTrainingForScheduler();
		if (instance == null) return;
		if (TYPE_SMART_ANNOTATION.equals(instance.getInstanceType()) && withTenant(instance, () -> smartAnnotationWorker.cancelRequested(instance))) {
			withTenant(instance, () -> { smartAnnotationWorker.cancelled(instance); updateStatus(instance, "cancelled", null, null); });
			return;
		}
		if (!isGpuIdle()) {
			return;
		}
		withTenant(instance, () -> {
			updateStatus(instance, STATUS_STARTING, null, null);
			if (TYPE_SMART_ANNOTATION.equals(instance.getInstanceType())) {
				try {
					String command = smartAnnotationWorker.prepare(instance);
					if (smartAnnotationWorker.cancelRequested(instance)) { smartAnnotationWorker.cancelled(instance); updateStatus(instance, "cancelled", null, null); return; }
					SSHService.SSHExecutionResult start = execute(command);
					if (!start.isSuccess() || isBlank(start.getOutput())) throw new IllegalStateException("智能标注容器启动失败");
					updateStatus(instance, STATUS_RUNNING, start.getOutput().trim(), null);
					smartAnnotationWorker.started(instance);
				} catch (Exception ex) { fail(instance, ex.getMessage() == null ? "智能标注准备失败" : ex.getMessage()); }
				return;
			}
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
		});
	}

	private boolean reconcileActiveContainers() {
		List<ContainerInstance> active = containerInstanceMapper.selectActiveTrainingForScheduler();
		boolean busy = false;
		for (ContainerInstance instance : active) {
			busy = withTenant(instance, () -> reconcileActiveContainer(instance)) || busy;
		}
		return busy;
	}

	private boolean reconcileActiveContainer(ContainerInstance instance) {
		String inspect = "docker inspect -f '{{.State.Status}}|{{.State.ExitCode}}|{{.Id}}' "
			+ shellQuote(instance.getInstanceName()) + " 2>/dev/null || echo missing";
		SSHService.SSHExecutionResult stateResult = execute(inspect);
		if (!stateResult.isSuccess()) return true; // A connection failure is not evidence of a missing process.
		String state = stateResult.getOutput() == null ? "" : stateResult.getOutput().trim();
		if (TYPE_SMART_ANNOTATION.equals(instance.getInstanceType()) && smartAnnotationWorker.cancelRequested(instance)) {
			if (state.startsWith("running|") && !execute("docker stop -t 10 " + shellQuote(instance.getInstanceName())).isSuccess()) return true;
			if (!state.startsWith("running|") && !state.startsWith("exited|") && !state.startsWith("dead|") && !"missing".equals(state)) return true;
			smartAnnotationWorker.cancelled(instance); updateStatus(instance, "cancelled", instance.getContainerId(), null);
			execute("docker rm " + shellQuote(instance.getInstanceName()) + " >/dev/null 2>&1 || true");
			return false;
		}
		if (state.startsWith("running|")) {
			String[] fields = state.split("\\|");
			updateStatus(instance, STATUS_RUNNING, fields.length > 2 ? fields[2] : instance.getContainerId(), null);
			updateGpuUsage(instance);
			if (TYPE_SMART_ANNOTATION.equals(instance.getInstanceType())) smartAnnotationWorker.refreshProgress(instance);
			return true;
		}
		if (state.startsWith("exited|") || state.startsWith("dead|")) {
			String[] fields = state.split("\\|");
			int exitCode = fields.length > 1 ? Integer.parseInt(fields[1]) : 1;
			try {
				if (exitCode == 0) complete(instance);
				else fail(instance, "计算任务容器退出，exitCode=" + exitCode + "，请查看本轮日志");
			} catch (Exception ex) {
				if (!TYPE_SMART_ANNOTATION.equals(instance.getInstanceType())) throw ex;
				fail(instance, ex.getMessage() == null ? "智能标注结果处理失败" : ex.getMessage());
			}
			execute("docker rm -f " + shellQuote(instance.getInstanceName()) + " >/dev/null 2>&1 || true");
		} else if ("missing".equals(state) && STATUS_STARTING.equals(instance.getInstanceStatus()) && TYPE_SMART_ANNOTATION.equals(instance.getInstanceType())) {
			fail(instance, "智能标注准备被中断，请重试任务");
			return false;
		} else if ("missing".equals(state) && STATUS_RUNNING.equals(instance.getInstanceStatus())) {
			fail(instance, "训练容器不存在，可能被外部删除");
			return false;
		}
		return STATUS_STARTING.equals(instance.getInstanceStatus());
	}

	private void withTenant(ContainerInstance instance, Runnable operation) {
		withTenant(instance, () -> {
			operation.run();
			return null;
		});
	}

	private <T> T withTenant(ContainerInstance instance, TenantOperation<T> operation) {
		if (instance == null || isBlank(instance.getTenantId())) {
			throw new IllegalStateException("GPU训练任务缺少租户标识");
		}
		String previousTenant = TenantContextHolder.getTenantId();
		TenantContextHolder.setTenantId(instance.getTenantId());
		try {
			return operation.execute();
		} finally {
			TenantContextHolder.setTenantId(previousTenant);
		}
	}

	private interface TenantOperation<T> {
		T execute();
	}

	private void complete(ContainerInstance instance) {
		if (TYPE_SMART_ANNOTATION.equals(instance.getInstanceType())) {
			smartAnnotationWorker.complete(instance);
			updateStatus(instance, STATUS_COMPLETED, instance.getContainerId(), null);
			return;
		}
		updateStatus(instance, STATUS_COMPLETED, instance.getContainerId(), null);
		AlgorithmTraining task = algorithmTrainingService.getById(instance.getTrainingTaskId());
		RemoteServers server = requireServer();
		String trainType = task == null ? "detect" : resolveTrainType(instance);
		String taskName = task == null ? null : task.getTaskName();
		String modelPath = remoteTrainingService.processTrainingResult(
			instance.getTrainingTaskId(), server, trainType, taskName);
		if (modelPath == null) {
			fail(instance, "训练完成，但未找到best.pt模型文件");
			return;
		}
		applicationEventPublisher.publishEvent(
			new TrainingModelReadyEvent(instance.getTrainingTaskId(), modelPath));
	}

	private void fail(ContainerInstance instance, String message) {
		updateStatus(instance, STATUS_ERROR, instance.getContainerId(), message);
		if (TYPE_SMART_ANNOTATION.equals(instance.getInstanceType())) {
			smartAnnotationWorker.failed(instance, message);
			return;
		}
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
			.in(ContainerInstance::getInstanceType, TYPE_TRAINING, TYPE_SMART_ANNOTATION)
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
									 Integer imgSize, String runName) {
		String trainType = isBlank(taskType) ? "detect" : taskType.trim();
		String yolo = "/data/work/anaconda3/envs/" + server.getCondaEnv() + "/bin/yolo";
		StringBuilder train = new StringBuilder();
		train.append(yolo).append(" ").append(shellToken(trainType)).append(" train");
		train.append(" data=").append(shellQuote(datasetPath));
		train.append(" model=").append(shellQuote("@preset/detect".equals(baseModel) ? "yolov8m.pt" : baseModel));
		if (epochs != null) train.append(" epochs=").append(epochs);
		if (batchSize != null) train.append(" batch=").append(batchSize);
		if (imgSize != null) train.append(" imgsz=").append(imgSize);
		train.append(" project=").append(shellQuote(server.getWorkDir() + "/runs/vls"));
		train.append(" name=").append(shellQuote(runName)).append(" exist_ok=False");
		if (properties.getWorkers() != null) train.append(" workers=").append(properties.getWorkers());
		String directory = datasetPath.substring(0, datasetPath.lastIndexOf('/'));
		String python = "/data/work/anaconda3/envs/" + server.getCondaEnv() + "/bin/python";
		String typed = shellQuote(python) + " " + shellQuote(directory + "/run_training.py")
			+ " --dataset " + shellQuote(datasetPath) + " --model " + shellQuote(baseModel)
			+ " --output " + shellQuote(server.getWorkDir() + "/runs/vls/" + runName)
			+ " --epochs " + (epochs == null ? 10 : epochs) + " --batch " + (batchSize == null ? 4 : batchSize)
			+ " --size " + (imgSize == null ? 640 : imgSize) + " --workers " + (properties.getWorkers() == null ? 2 : properties.getWorkers());
		String legacy = train.toString(); train.setLength(0);
		if ("detect".equals(trainType)) train.append("if [ -f ").append(shellQuote(directory + "/run_training.py")).append(" ]; then ").append(typed).append("; else ").append(legacy).append("; fi");
		else train.append(typed);
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
			+ " -e VLS_MODEL_CACHE=" + shellQuote(properties.getHostDataDir() + "/vls-model-cache")
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
