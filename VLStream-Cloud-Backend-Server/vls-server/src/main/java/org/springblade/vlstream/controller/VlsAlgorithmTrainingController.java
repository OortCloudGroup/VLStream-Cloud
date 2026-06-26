package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.enums.AlgorithmTrainingStatusEnum;
import org.springblade.vlstream.excel.VlsAlgorithmTrainingExcel;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.vo.AlgorithmTrainingVO;
import org.springblade.vlstream.service.*;
import org.springblade.vlstream.wrapper.VlsAlgorithmTrainingWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Algorithm training task table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@RequestMapping("/vlsAlgorithmTraining")
@Tag(name = "Algorithm training task table", description = "Algorithm training task table interface")
public class VlsAlgorithmTrainingController extends BladeController {

	private static final String RKNN_MODEL_ZOO_PATH = "/data/work/ultralytics_yolov8-main/rknn_model_zoo-main";
	private static final String COCO_SUBSET_FILE_NAME = "coco_subset_20.txt";

	@Resource
	private IVlsAlgorithmTrainingService vlsAlgorithmTrainingService;

	@Resource
	private RemoteTrainingService remoteTrainingService;

	@Resource
	private IVlsAlgorithmAnnotationService algorithmAnnotationService;

	@Resource
	private IVlsAlgorithmService algorithmService;

	@Resource
	private IVlsAlgorithmModelService algorithmModelService;

	@Resource
	private ObjectMapper objectMapper = new ObjectMapper();

	@Resource
	private SSHService sshService;

	@Resource
	private VlsSshProperties sshProperties;

	/**
	 * Algorithm training task table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsAlgorithmTraining")
	public R<AlgorithmTrainingVO> detail(AlgorithmTraining vlsAlgorithmTraining) {
		AlgorithmTraining detail = vlsAlgorithmTrainingService.getOne(Condition.getQueryWrapper(vlsAlgorithmTraining));
		return R.data(VlsAlgorithmTrainingWrapper.build().entityVO(detail));
	}

	/**
	 * Algorithm training task table paging
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmTraining")
	public R<IPage<AlgorithmTrainingVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmTraining, Query query) {
		IPage<AlgorithmTraining> pages = vlsAlgorithmTrainingService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmTraining, AlgorithmTraining.class));
		return R.data(VlsAlgorithmTrainingWrapper.build().pageVO(pages));
	}


	/**
	 * Algorithm training task table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmTraining")
	public R<IPage<AlgorithmTrainingVO>> page(AlgorithmTrainingVO vlsAlgorithmTraining, Query query) {
		IPage<AlgorithmTrainingVO> pages = vlsAlgorithmTrainingService.selectVlsAlgorithmTrainingPage(Condition.getPage(query), vlsAlgorithmTraining);
		for (AlgorithmTrainingVO training : pages.getRecords()) {
			Algorithm algorithm = algorithmService.getById(training.getAlgorithmId());
			training.setAlgorithmName(algorithm.getName());
			training.setTrainType(algorithm.getCategory());
			training.setTargetModel(algorithm.getPtModelFilePath());
		}
		return R.data(pages);
	}

	/**
	 * Add algorithm training task table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsAlgorithmTraining")
	public R save(@Valid @RequestBody AlgorithmTraining vlsAlgorithmTraining) {
		return R.status(vlsAlgorithmTrainingService.save(vlsAlgorithmTraining));
	}

	/**
	 * Modify algorithm training task table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsAlgorithmTraining")
	public R update(@Valid @RequestBody AlgorithmTraining vlsAlgorithmTraining) {
		return R.status(vlsAlgorithmTrainingService.updateById(vlsAlgorithmTraining));
	}

	/**
	 * Add or modify algorithm training task table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsAlgorithmTraining")
	public R submit(@Valid @RequestBody AlgorithmTraining vlsAlgorithmTraining) {
		return R.status(vlsAlgorithmTrainingService.saveOrUpdate(vlsAlgorithmTraining));
	}

	/**
	 * Delete algorithm training task table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmTrainingService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAlgorithmTraining")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsAlgorithmTraining")
	public void exportVlsAlgorithmTraining(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmTraining, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmTraining> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmTraining, AlgorithmTraining.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmTrainingEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmTrainingEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmTrainingExcel> list = vlsAlgorithmTrainingService.exportVlsAlgorithmTraining(queryWrapper);
		ExcelUtil.export(response, "Algorithm training task table data" + DateUtil.today(), "Algorithm training task table database table", list, VlsAlgorithmTrainingExcel.class);
	}

	/**
	 * Query training task details by ID
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Query Training Task Details", description = "Get training task details by ID")
	public R<AlgorithmTraining> getTrainingById(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Query training task details: ID={}", id);

		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("Training task does not exist");
		}

		return R.data(training);
	}

	/**
	 * Add training task
	 */
	@PostMapping
	@Operation(summary = "Create training task", description = "Add training task")
	public R<String> createTraining(@Valid @RequestBody AlgorithmTraining training) {

		training.setTrainStatus(AlgorithmTrainingStatusEnum.pending);
		log.info("Create training task: {}", training);

		int result = vlsAlgorithmTrainingService.insertAlgorithmTraining(training);
		if (result > 0) {
			return R.success("Created successfully");
		} else {
			return R.fail("Creation failed");
		}
	}

	/**
	 * Modify training task
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Update training task", description = "Update training task information by ID")
	public R<String> updateTraining(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody AlgorithmTraining training) {

		log.info("Update training task: ID={}, data={}", id, training);

		training.setId(id);
		int result = vlsAlgorithmTrainingService.updateAlgorithmTraining(training);
		if (result > 0) {
			return R.success("Update successful");
		} else {
			return R.fail("Update failed");
		}
	}

	/**
	 * Update training status
	 */
	@PutMapping("/{id}/status")
	@Operation(summary = "Update training status", description = "Update the status of the specified training task")
	public R<String> updateTrainingStatus(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id,
		@RequestBody Map<String, String> statusUpdate) {

		String trainStatus = statusUpdate.get("trainStatus");
		log.info("Update training status: ID={}, status={}", id, trainStatus);

		try {
			// Get existing training tasks
			AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
			if (training == null) {
				return R.fail("Training task does not exist");
			}

			// Update status
			training.setTrainStatus(AlgorithmTrainingStatusEnum.of(trainStatus));
			int result = vlsAlgorithmTrainingService.updateAlgorithmTraining(training);

			if (result > 0) {
				log.info("Training status updated successfully: ID={}, new status={}", id, trainStatus);
				return R.success("Status updated successfully");
			} else {
				return R.fail("Failed to update status");
			}
		} catch (Exception e) {
			log.error("Failed to update training status: ID={}, error={}", id, e.getMessage());
			return R.fail("Exception updating status:" + e.getMessage());
		}
	}

	/**
	 * Delete training task
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete training task", description = "Delete training task by ID")
	public R<String> deleteTraining(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Delete training task: ID={}", id);

		int result = vlsAlgorithmTrainingService.deleteAlgorithmTrainingById(id);
		if (result > 0) {
			return R.success("Deleted successfully");
		} else {
			return R.fail("Delete failed");
		}
	}

	/**
	 * Batch delete training tasks
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "Batch delete training tasks", description = "Batch delete training tasks by ID list")
	public R<String> batchDeleteTraining(@RequestBody List<Long> ids) {

		log.info("Batch delete training tasks: IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select training tasks to delete");
		}

		int result = vlsAlgorithmTrainingService.deleteAlgorithmTrainingByIds(ids.toArray(new Long[0]));
		if (result > 0) {
			return R.success("Batch delete succeeded");
		} else {
			return R.fail("Batch delete failed");
		}
	}

	/**
	 * Start training task
	 */
	@PostMapping("/{id}/start")
	@Operation(summary = "Start training task", description = "Start designated training task")
	public R<RemoteTrainingService.StartResult> startTraining(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "Training Epochs") @RequestParam(defaultValue = "10") Integer epochs,
		@Parameter(description = "Dataset ID") @RequestParam Long datasetId,
		@Parameter(description = "Batch size") @RequestParam(defaultValue = "16") Integer batchSize,
		@Parameter(description = "Image size") @RequestParam(required = false) Integer imgSize,
		@Parameter(description = "Extra training parameters") @RequestParam(required = false) String extraParams) {

		log.info("=== Start training task ===");
		log.info("Training Task ID: {}", id);

		try {
			AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
			if (training == null) {
				return R.fail("Training task does not exist");
			}
			AlgorithmAnnotation annotation = algorithmAnnotationService.getById(datasetId);
			if (annotation == null) {
				return R.fail("Dataset does not exist");
			}
			if (annotation.getDatasetPath() == null || annotation.getDatasetPath().trim().isEmpty()) {
				return R.fail("No valid dataset path configured");
			}

			Algorithm algorithm = training.getAlgorithmId() != null ? algorithmService.getById(training.getAlgorithmId()) : null;
			if (algorithm == null) {
				return R.fail("Algorithm does not exist");
			}
			String baseModel = algorithm.getPtModelFilePath();
			if (StringUtils.isBlank(baseModel)) {
				return R.fail("Algorithm base model path is empty");
			}

			Map<String, Object> config = parseConfigParams(training.getConfigParams());
			Integer finalEpochs = epochs != null ? epochs : getIntFromConfig(config, "epochs", training.getEpochTotal(), 100);
			Integer finalBatch = batchSize != null ? batchSize : getIntFromConfig(config, "batchSize", null, 16);
			Integer finalImgSize = imgSize != null ? imgSize : getIntFromConfig(config, "imgsz", getIntFromConfig(config, "resolution", null, null), 640);
			String mergedExtraParams = buildExtraParams(config, extraParams);

			RemoteTrainingService.StartResult startResult = remoteTrainingService.startTraining(
				algorithm.getCategory().getCode(),
				id,
				annotation.getDatasetPath(),
				baseModel,
				finalBatch,
				finalEpochs,
				finalImgSize,
				mergedExtraParams
			);

			AlgorithmTraining update = new AlgorithmTraining();
			update.setId(id);
			update.setTrainStatus(AlgorithmTrainingStatusEnum.training);
			update.setStartTime(new Date());
			update.setEpochTotal(finalEpochs);
			update.setProgress(0);
			update.setLogPath(startResult.getLogPath());
			vlsAlgorithmTrainingService.updateAlgorithmTraining(update);

			log.info("Training task {} has started, log path: {}", id, startResult.getLogPath());
			return R.data(startResult);
		} catch (Exception e) {
			log.error("Failed to trigger training task: {}", e.getMessage(), e);
			return R.fail("Failed to trigger training task:" + e.getMessage());
		}
	}

	/**
	 * Convert model
	 */
	@PostMapping("/{id}/convert-model")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "Convert model", description = "Convert pt model to onnx and rknn")
	public R<String> convertModel(
		@Parameter(description = "Model training ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Model training: id={}", id);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("Model training not found");
		}
		String ptModelPath = training.getModelOutputPath();
		if (ptModelPath == null || ptModelPath.isEmpty()) {
			return R.fail("Model path cannot be empty");
		}
		String datasetPath = resolveDatasetPath(training);
		AlgorithmTraining trainingSnapshot = training;
		String datasetPathSnapshot = datasetPath;

		log.info("Dataset path: {}", datasetPathSnapshot);
		Thread convertThread = new Thread(() -> {
			ExecutorService executor = Executors.newFixedThreadPool(2);
			try {
				Future<String> onnxFuture = executor.submit(() -> remoteTrainingService.exportModel(ptModelPath, "onnx"));
				Future<String> rknnFuture = executor.submit(() -> remoteTrainingService.exportModel(ptModelPath, "rknn"));
				String onnxPath = getConvertResult(onnxFuture, id, "onnx");
				String rknnPath = getConvertResult(rknnFuture, id, "rknn");
				String synsetDatasetPath = datasetPathSnapshot;
				if (synsetDatasetPath == null || synsetDatasetPath.trim().isEmpty()) {
					synsetDatasetPath = resolveDatasetPath(trainingSnapshot);
				}
				String int8RknnPath = null;
				if (onnxPath != null && !onnxPath.isEmpty()) {
					if (synsetDatasetPath == null || synsetDatasetPath.trim().isEmpty()) {
						log.warn("Dataset path is empty, skip synset generation: id={}", id);
					} else {
						remoteTrainingService.generateOnnxSynsetFile(id, onnxPath, synsetDatasetPath);
					}
					String datasetDir = resolveDatasetDirectory(synsetDatasetPath);
					if (datasetDir == null || datasetDir.trim().isEmpty()) {
						log.warn("Dataset directory is empty, skip int8 conversion: id={}, datasetPath={}", id, synsetDatasetPath);
					} else {
						String cocoSubsetPath = datasetDir + "/" + COCO_SUBSET_FILE_NAME;
						String int8OutputPath = buildInt8RknnOutputPath(onnxPath);
						if (int8OutputPath == null || int8OutputPath.trim().isEmpty()) {
							log.warn("Int8 output path is empty, skip conversion: id={}, onnxPath={}", id, onnxPath);
						} else {
							String convertCommand = String.format("cd \"%s\" && python3 convert.py --model \"%s\" --dataset \"%s\" --platform rk3588 --output \"%s\"",
								RKNN_MODEL_ZOO_PATH, onnxPath, cocoSubsetPath, int8OutputPath);
							SSHService.SSHExecutionResult convertResult = sshService.executeCommand(
								sshProperties.getHost(),
								sshProperties.getPort(),
								sshProperties.getUsername(),
								sshProperties.getPassword(),
								convertCommand
							);
							boolean int8FileExists = convertResult.isSuccess() && checkRemoteFileExists(int8OutputPath);
							if (int8FileExists) {
								int8RknnPath = int8OutputPath;
								log.info("Int8 RKNN conversion completed: id={}, path={}", id, int8RknnPath);
								if (convertResult.getErrorMsg() != null && !convertResult.getErrorMsg().trim().isEmpty()) {
									log.info("Int8 RKNN conversion stderr captured: id={}, errorLength={}", id, convertResult.getErrorMsg().length());
								}
							} else {
								log.warn("Int8 RKNN conversion failed: id={}, error={}", id, convertResult.getErrorMsg());
							}
						}
					}
				}
				String rk3588RknnPath = null;
				if (rknnPath != null && !rknnPath.isEmpty()) {
					rk3588RknnPath = rknnPath.replace(".rknn", "-rk3588.rknn");
				}
				AlgorithmTraining update = new AlgorithmTraining();
				update.setId(id);
				update.setOnnxModelOutputPath(onnxPath);
				update.setRknnModelOutputPath(rk3588RknnPath);
				update.setInt8RknnModelOutputPath(int8RknnPath);
				int updateResult = vlsAlgorithmTrainingService.updateAlgorithmTraining(update);
				if (updateResult > 0) {
					log.info("Model path updated successfully: id={}, onnxPath={}, rknnPath={}, int8RknnPath={}", id, onnxPath, rk3588RknnPath, int8RknnPath);
				} else {
					log.warn("Failed to update model path: id={}, onnxPath={}, rknnPath={}, int8RknnPath={}", id, onnxPath, rk3588RknnPath, int8RknnPath);
				}
			} catch (Exception exception) {
				log.error("Model conversion exception: id={}, error={}", id, exception.getMessage(), exception);
			} finally {
				executor.shutdown();
			}
		});
		convertThread.setName("model-convert-" + id);
		convertThread.start();
		return R.success("Model conversion succeeded");
	}

	/**
	 * Diagnose remote server conda environment
	 */
	@GetMapping("/diagnose-conda")
	@Operation(summary = "Diagnose conda environment", description = "Check conda installation status on remote server")
	public R<String> diagnoseConda() {
		try {
			// Build Diagnosis Command
			StringBuilder diagCmd = new StringBuilder();
			diagCmd.append("echo '=== Environment Diagnosis ===' &&");
			diagCmd.append("echo 'PATH: '$PATH && ");
			diagCmd.append("echo '=== Find conda ===' &&");
			diagCmd.append("which conda 2>/dev/null || echo 'conda not in PATH' && ");
			diagCmd.append("find /home -name 'conda' -type f 2>/dev/null | head -5 && ");
			diagCmd.append("find /opt -name 'conda' -type f 2>/dev/null | head -5 && ");
			diagCmd.append("find /usr -name 'conda' -type f 2>/dev/null | head -5 && ");
			diagCmd.append("echo '=== Find conda.sh ===' &&");
			diagCmd.append("find /home -name 'conda.sh' -type f 2>/dev/null | head -5 && ");
			diagCmd.append("find /opt -name 'conda.sh' -type f 2>/dev/null | head -5 && ");
			diagCmd.append("echo '=== Find yolo ===' &&");
			diagCmd.append("which yolo 2>/dev/null || echo 'yolo not in PATH' && ");
			diagCmd.append("find /home -name 'yolo' -type f 2>/dev/null | head -3 && ");
			diagCmd.append("echo '=== Python Environment ===' &&");
			diagCmd.append("which python 2>/dev/null || echo 'python not found' && ");
			diagCmd.append("which python3 2>/dev/null || echo 'python3 not found' && ");
			diagCmd.append("echo '=== Completed ==='");

			SSHService.SSHExecutionResult result = sshService.executeCommand(
				sshProperties.getHost(),
				sshProperties.getPort(),
				sshProperties.getUsername(),
				sshProperties.getPassword(),
				diagCmd.toString()
			);

			if (result.isSuccess()) {
				log.info("Conda diagnosis succeeded: {}", result.getOutput());
				return R.success("Conda diagnosis result:\n" + result.getOutput());
			} else {
				log.error("Conda diagnosis failed: {}", result.getErrorMsg());
				return R.fail("Conda diagnosis failed:" + result.getErrorMsg());
			}
		} catch (Exception e) {
			log.error("Conda diagnosis anomaly: {}", e.getMessage(), e);
			return R.fail("Conda diagnosis anomaly:" + e.getMessage());
		}
	}

	/**
	 * Stop training task
	 */
	@PostMapping("/{id}/stop")
	@Operation(summary = "Stop training task", description = "Stop specified training task")
	public R<String> stopTraining(@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Stop training task: ID={}", id);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("Training task not found");
		}

		boolean stopped = remoteTrainingService.stopTraining(id, training.getLogPath());
		if (stopped) {
			AlgorithmTraining update = new AlgorithmTraining();
			update.setId(id);
			update.setTrainStatus(AlgorithmTrainingStatusEnum.stop);
			update.setEndTime(new Date());
			vlsAlgorithmTrainingService.updateAlgorithmTraining(update);
			return R.success("Training task has stopped");
		}
		return R.fail("Failed to stop training task");
	}

	/**
	 * Get training logs
	 */
	@GetMapping("/{id}/logs")
	@Operation(summary = "Get training status", description = "Get logs of specified training task")
	public R<RemoteTrainingService.LogResult> getTrainingLogs(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id,
		@RequestParam(value = "logPath", required = false) String logPath,
		@RequestParam(value = "lines", defaultValue = "200") Integer lines) {

		log.info("Get training logs: ID={}?logPath={}", id, logPath);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("Training task not found");
		}
		Algorithm algorithm = training.getAlgorithmId() != null ? algorithmService.getById(training.getAlgorithmId()) : null;
		RemoteTrainingService.LogResult logResult = remoteTrainingService.getTrainingLogs(
			id,
			logPath,
			algorithm.getCategory().getCode(),
			training.getTaskName(),
			lines == null ? 200 : lines
		);
		return R.data(logResult);
	}

	/**
	 * Get training status
	 */
	@GetMapping("/{id}/status")
	@Operation(summary = "Get training status", description = "Get status of specified training task")
	public R<RemoteTrainingService.TrainingProgress> getTrainingStatus(
		@Parameter(description = "Training Task ID", example = "1") @PathVariable @NotNull Long id,
		@RequestParam(value = "logPath", required = false) String logPath) {

		log.info("Get training status: ID={}?logPath={}", id, logPath);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("Training task not found");
		}
		RemoteTrainingService.TrainingProgress progress = remoteTrainingService.getProgress(id, logPath);
		if (progress != null && progress.isCompleted()) {
			AlgorithmTraining update = new AlgorithmTraining();
			update.setId(id);
			update.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
			update.setProgress(100);
			update.setEndTime(new Date());
			vlsAlgorithmTrainingService.updateAlgorithmTraining(update);
		}
		return R.data(progress);
	}

	private String getConvertResult(Future<String> convertFuture, Long trainingId, String format) {
		if (convertFuture == null) {
			return null;
		}
		try {
			return convertFuture.get();
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			log.warn("Model conversion exception: id={}, format={}", trainingId, format);
			return null;
		} catch (ExecutionException executionException) {
			log.warn("Model conversion exception: id={}, format={}, error={}", trainingId, format, executionException.getMessage());
			return null;
		}
	}

	private String resolveDatasetDirectory(String datasetPath) {
		if (datasetPath == null || datasetPath.trim().isEmpty()) {
			return null;
		}
		String normalized = datasetPath.trim();
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		String lowerPath = normalized.toLowerCase(java.util.Locale.ROOT);
		if (lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")) {
			int lastSlash = normalized.lastIndexOf('/');
			if (lastSlash <= 0) {
				return null;
			}
			return normalized.substring(0, lastSlash);
		}
		return normalized;
	}

	private String buildInt8RknnOutputPath(String onnxPath) {
		if (onnxPath == null || onnxPath.trim().isEmpty()) {
			return null;
		}
		String normalized = onnxPath.trim();
		int lastSlash = normalized.lastIndexOf('/');
		String dir = lastSlash >= 0 ? normalized.substring(0, lastSlash) : "";
		String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
		int dotIndex = fileName.lastIndexOf('.');
		String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
		String targetName = baseName + "-rk3588-int8.rknn";
		return dir.isEmpty() ? targetName : dir + "/" + targetName;
	}

	private boolean checkRemoteFileExists(String remotePath) {
		if (remotePath == null || remotePath.trim().isEmpty()) {
			return false;
		}
		String normalizedPath = remotePath.trim().replace("\"", "\\\"");
		String checkCommand = String.format("if [ -f \"%s\" ]; then echo OK; else echo MISSING; fi", normalizedPath);
		SSHService.SSHExecutionResult checkResult = sshService.executeCommand(
			sshProperties.getHost(),
			sshProperties.getPort(),
			sshProperties.getUsername(),
			sshProperties.getPassword(),
			checkCommand
		);
		if (!checkResult.isSuccess()) {
			log.warn("Remote file check failed: path={}, error={}", remotePath, checkResult.getErrorMsg());
			return false;
		}
		String output = checkResult.getOutput();
		return output != null && output.trim().equalsIgnoreCase("OK");
	}

	private Map<String, Object> parseConfigParams(String configJson) {
		if (configJson == null || configJson.trim().isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			log.warn("Parse exception: {}", e.getMessage());
			return Collections.emptyMap();
		}
	}

	/**
	 * Get configuration
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

	private String resolveDatasetPath(AlgorithmTraining training) {
		if (training == null || training.getDatasetId() == null) {
			return null;
		}
		AlgorithmAnnotation annotation = algorithmAnnotationService.getById(training.getDatasetId());
		if (annotation == null) {
			return null;
		}
		String datasetPath = annotation.getDatasetPath();
		if (datasetPath == null || datasetPath.trim().isEmpty()) {
			return null;
		}
		return datasetPath.trim();
	}

	@GetMapping("/download-model")
	@Operation(summary = "Download model file", description = "Download trained model file from remote server")
	public void downloadModel(@RequestParam String id, @RequestParam String type, HttpServletResponse response) {
		try {
			log.info("Download model file: {}", type);

			AlgorithmModel algorithmModel = algorithmModelService.getById(id);
			if (algorithmModel == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("Model not found: " + id);
				return;
			}

			String downloadPath = algorithmModel.getModelPath();
			if (type.equals("onnx")) {
				downloadPath = algorithmModel.getOnnxModelPath();
			} else if (type.equals("rknn")) {
				downloadPath = algorithmModel.getRknnModelPath();
			} else if (type.equals("int8-rknn")) {
				downloadPath = algorithmModel.getInt8RknnModelOutputPath();
			}
			if (StringUtils.isBlank(downloadPath)) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("Model file path is empty");
				return;
			}
			// Download file from remote server
			SSHService.SSHExecutionResult result = sshService.executeCommand(
				sshProperties.getHost(),
				sshProperties.getPort(),
				sshProperties.getUsername(),
				sshProperties.getPassword(),
				String.format("base64 %s", downloadPath)
			);

			if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
				// Clean base64 content, removing possible line breaks and other characters
				String base64Content = result.getOutput().trim().replaceAll("\\s+", "");
				log.info("Base64 content length: {}", base64Content.length());

				// Decode base64 content
				byte[] fileContent = java.util.Base64.getDecoder().decode(base64Content);

				String fileName = downloadPath.substring(downloadPath.lastIndexOf('/') + 1);
				response.setContentType("application/octet-stream");

				String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
				response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
				response.setContentLength(fileContent.length);

				// Write response
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

				log.info("Model file downloaded successfully: {}", fileName);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.getWriter().write("Model file not found: " + downloadPath);
				log.error("Model file does not exist: {}", downloadPath);
			}

		} catch (Exception e) {
			log.error("Failed to download model file: {}", e.getMessage(), e);
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().write("Download failed: " + e.getMessage());
			} catch (Exception ex) {
				log.error("Failed to write error response: {}", ex.getMessage());
			}
		}
	}

}
