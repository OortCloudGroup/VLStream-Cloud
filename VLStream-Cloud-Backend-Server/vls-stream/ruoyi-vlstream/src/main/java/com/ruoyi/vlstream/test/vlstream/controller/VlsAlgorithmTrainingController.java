/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmTrainingExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;
import com.ruoyi.vlstream.test.vlstream.service.*;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmTrainingWrapper;
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
 * 算法训练任务表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@RequestMapping("/vlsAlgorithmTraining")
@Tag(name = "算法训练任务表", description = "算法训练任务表接口")
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
	 * 算法训练任务表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAlgorithmTraining")
	public R<AlgorithmTrainingVO> detail(AlgorithmTraining vlsAlgorithmTraining) {
		AlgorithmTraining detail = vlsAlgorithmTrainingService.getOne(Condition.getQueryWrapper(vlsAlgorithmTraining));
		return R.data(VlsAlgorithmTrainingWrapper.build().entityVO(detail));
	}

	/**
	 * 算法训练任务表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAlgorithmTraining")
	public R<IPage<AlgorithmTrainingVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmTraining, Query query) {
		IPage<AlgorithmTraining> pages = vlsAlgorithmTrainingService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmTraining, AlgorithmTraining.class));
		return R.data(VlsAlgorithmTrainingWrapper.build().pageVO(pages));
	}


	/**
	 * 算法训练任务表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAlgorithmTraining")
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
	 * 算法训练任务表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAlgorithmTraining")
	public R save(@Valid @RequestBody AlgorithmTraining vlsAlgorithmTraining) {
		return R.status(vlsAlgorithmTrainingService.save(vlsAlgorithmTraining));
	}

	/**
	 * 算法训练任务表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAlgorithmTraining")
	public R update(@Valid @RequestBody AlgorithmTraining vlsAlgorithmTraining) {
		return R.status(vlsAlgorithmTrainingService.updateById(vlsAlgorithmTraining));
	}

	/**
	 * 算法训练任务表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAlgorithmTraining")
	public R submit(@Valid @RequestBody AlgorithmTraining vlsAlgorithmTraining) {
		return R.status(vlsAlgorithmTrainingService.saveOrUpdate(vlsAlgorithmTraining));
	}

	/**
	 * 算法训练任务表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmTrainingService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAlgorithmTraining")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAlgorithmTraining")
	public void exportVlsAlgorithmTraining(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmTraining, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmTraining> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmTraining, AlgorithmTraining.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmTrainingEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmTrainingEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmTrainingExcel> list = vlsAlgorithmTrainingService.exportVlsAlgorithmTraining(queryWrapper);
		ExcelUtil.export(response, "算法训练任务表数据" + DateUtil.today(), "算法训练任务表数据表", list, VlsAlgorithmTrainingExcel.class);
	}

	/**
	 * 根据ID查询训练任务详情
	 */
	@GetMapping("/{id}")
	@Operation(summary = "查询训练任务详情", description = "根据ID获取训练任务详细信息")
	public R<AlgorithmTraining> getTrainingById(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("查询训练任务详情：ID={}", id);

		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("训练任务不存在");
		}

		return R.data(training);
	}

	/**
	 * 新增训练任务
	 */
	@PostMapping
	@Operation(summary = "创建训练任务", description = "新增训练任务")
	public R<String> createTraining(@Valid @RequestBody AlgorithmTraining training) {

		training.setTrainStatus(AlgorithmTrainingStatusEnum.pending);
		log.info("创建训练任务：{}", training);

		int result = vlsAlgorithmTrainingService.insertAlgorithmTraining(training);
		if (result > 0) {
			return R.success("创建成功");
		} else {
			return R.fail("创建失败");
		}
	}

	/**
	 * 修改训练任务
	 */
	@PutMapping("/{id}")
	@Operation(summary = "更新训练任务", description = "根据ID更新训练任务信息")
	public R<String> updateTraining(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody AlgorithmTraining training) {

		log.info("更新训练任务：ID={}, 数据={}", id, training);

		training.setId(id);
		int result = vlsAlgorithmTrainingService.updateAlgorithmTraining(training);
		if (result > 0) {
			return R.success("更新成功");
		} else {
			return R.fail("更新失败");
		}
	}

	/**
	 * 更新训练状态
	 */
	@PutMapping("/{id}/status")
	@Operation(summary = "更新训练状态", description = "更新指定训练任务的状态")
	public R<String> updateTrainingStatus(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id,
		@RequestBody Map<String, String> statusUpdate) {

		String trainStatus = statusUpdate.get("trainStatus");
		log.info("更新训练状态：ID={}, 状态={}", id, trainStatus);

		try {
			// 获取现有的训练任务
			AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
			if (training == null) {
				return R.fail("训练任务不存在");
			}

			// 更新状态
			training.setTrainStatus(AlgorithmTrainingStatusEnum.of(trainStatus));
			int result = vlsAlgorithmTrainingService.updateAlgorithmTraining(training);

			if (result > 0) {
				log.info("训练状态更新成功：ID={}, 新状态={}", id, trainStatus);
				return R.success("状态更新成功");
			} else {
				return R.fail("状态更新失败");
			}
		} catch (Exception e) {
			log.error("更新训练状态异常：ID={}, 错误={}", id, e.getMessage());
			return R.fail("状态更新异常：" + e.getMessage());
		}
	}

	/**
	 * 删除训练任务
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "删除训练任务", description = "根据ID删除训练任务")
	public R<String> deleteTraining(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("删除训练任务：ID={}", id);

		int result = vlsAlgorithmTrainingService.deleteAlgorithmTrainingById(id);
		if (result > 0) {
			return R.success("删除成功");
		} else {
			return R.fail("删除失败");
		}
	}

	/**
	 * 批量删除训练任务
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "批量删除训练任务", description = "根据ID列表批量删除训练任务")
	public R<String> batchDeleteTraining(@RequestBody List<Long> ids) {

		log.info("批量删除训练任务：IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要删除的训练任务");
		}

		int result = vlsAlgorithmTrainingService.deleteAlgorithmTrainingByIds(ids.toArray(new Long[0]));
		if (result > 0) {
			return R.success("批量删除成功");
		} else {
			return R.fail("批量删除失败");
		}
	}

	/**
	 * 开始训练任务
	 */
	@PostMapping("/{id}/start")
	@Operation(summary = "开始训练任务", description = "开始指定的训练任务")
	public R<RemoteTrainingService.StartResult> startTraining(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "训练轮次") @RequestParam(defaultValue = "10") Integer epochs,
		@Parameter(description = "数据集id") @RequestParam Long datasetId,
		@Parameter(description = "批大小") @RequestParam(defaultValue = "16") Integer batchSize,
		@Parameter(description = "图像尺寸") @RequestParam(required = false) Integer imgSize,
		@Parameter(description = "额外训练参数") @RequestParam(required = false) String extraParams) {

		log.info("=== 开始训练任务 ===");
		log.info("训练任务ID: {}", id);

		try {
			AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
			if (training == null) {
				return R.fail("训练任务不存在");
			}
			AlgorithmAnnotation annotation = algorithmAnnotationService.getById(datasetId);
			if (annotation == null) {
				return R.fail("数据集不存在");
			}
			if (annotation.getDatasetPath() == null || annotation.getDatasetPath().trim().isEmpty()) {
				return R.fail("未配置有效的数据集路径");
			}

			Algorithm algorithm = training.getAlgorithmId() != null ? algorithmService.getById(training.getAlgorithmId()) : null;
			if (algorithm == null) {
				return R.fail("算法不存在");
			}
			String baseModel = algorithm.getPtModelFilePath();
			if (StringUtils.isBlank(baseModel)) {
				return R.fail("算法基础模型路径为空");
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
				finalEpochs,
				finalBatch,
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

			log.info("训练任务{}已启动，日志路径: {}", id, startResult.getLogPath());
			return R.data(startResult);
		} catch (Exception e) {
			log.error("触发训练任务失败: {}", e.getMessage(), e);
			return R.fail("触发训练任务失败: " + e.getMessage());
		}
	}

	/**
	 * 转换模型
	 */
	@PostMapping("/{id}/convert-model")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "转换模型", description = "把pt模型转换为onnx和rknn")
	public R<String> convertModel(
		@Parameter(description = "模型训练ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("模型训练: id={}", id);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("找不到模型训练");
		}
		String ptModelPath = training.getModelOutputPath();
		if (ptModelPath == null || ptModelPath.isEmpty()) {
			return R.fail("模型路径不能为空");
		}
		String datasetPath = resolveDatasetPath(training);
		AlgorithmTraining trainingSnapshot = training;
		String datasetPathSnapshot = datasetPath;

		log.info("数据集路径：{}", datasetPathSnapshot);
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
					log.info("模型路径更新成功: id={}, onnxPath={}, rknnPath={}, int8RknnPath={}", id, onnxPath, rk3588RknnPath, int8RknnPath);
				} else {
					log.warn("模型路径更新失败: id={}, onnxPath={}, rknnPath={}, int8RknnPath={}", id, onnxPath, rk3588RknnPath, int8RknnPath);
				}
			} catch (Exception exception) {
				log.error("模型转换异常: id={}, error={}", id, exception.getMessage(), exception);
			} finally {
				executor.shutdown();
			}
		});
		convertThread.setName("model-convert-" + id);
		convertThread.start();
		return R.success("模型转换成功");
	}

	/**
	 * 诊断远程服务器conda环境
	 */
	@GetMapping("/diagnose-conda")
	@Operation(summary = "诊断conda环境", description = "检查远程服务器上的conda安装情况")
	public R<String> diagnoseConda() {
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
				sshProperties.getHost(),
				sshProperties.getPort(),
				sshProperties.getUsername(),
				sshProperties.getPassword(),
				diagCmd.toString()
			);

			if (result.isSuccess()) {
				log.info("Conda诊断成功: {}", result.getOutput());
				return R.success("Conda诊断结果:\n" + result.getOutput());
			} else {
				log.error("Conda诊断失败: {}", result.getErrorMsg());
				return R.fail("Conda诊断失败: " + result.getErrorMsg());
			}
		} catch (Exception e) {
			log.error("Conda诊断异常: {}", e.getMessage(), e);
			return R.fail("Conda诊断异常: " + e.getMessage());
		}
	}

	/**
	 * 停止训练任务
	 */
	@PostMapping("/{id}/stop")
	@Operation(summary = "停止训练任务", description = "停止指定的训练任务")
	public R<String> stopTraining(@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("停止训练任务: ID={}", id);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("找不到训练任务");
		}

		boolean stopped = remoteTrainingService.stopTraining(id, training.getLogPath());
		if (stopped) {
			AlgorithmTraining update = new AlgorithmTraining();
			update.setId(id);
			update.setTrainStatus(AlgorithmTrainingStatusEnum.stop);
			update.setEndTime(new Date());
			vlsAlgorithmTrainingService.updateAlgorithmTraining(update);
			return R.success("训练任务已停止");
		}
		return R.fail("训练任务停止失败");
	}

	/**
	 * 获取训练日志
	 */
	@GetMapping("/{id}/logs")
	@Operation(summary = "获取训练状态", description = "获取指定训练任务的日志")
	public R<RemoteTrainingService.LogResult> getTrainingLogs(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id,
		@RequestParam(value = "logPath", required = false) String logPath,
		@RequestParam(value = "lines", defaultValue = "200") Integer lines) {

		log.info("获取训练日志：ID={}?logPath={}", id, logPath);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("找不到训练任务");
		}
		Algorithm algorithm = training.getAlgorithmId() != null ? algorithmService.getById(training.getAlgorithmId()) : null;
		String trainType = algorithm != null && algorithm.getCategory() != null
			? algorithm.getCategory().getCode()
			: "detect";
		RemoteTrainingService.LogResult logResult = remoteTrainingService.getTrainingLogs(
			id,
			logPath,
			trainType,
			training.getTaskName(),
			lines == null ? 200 : lines
		);
		return R.data(logResult);
	}

	/**
	 * 获取训练状态
	 */
	@GetMapping("/{id}/status")
	@Operation(summary = "获取训练状态", description = "获取指定训练任务的状态")
	public R<RemoteTrainingService.TrainingProgress> getTrainingStatus(
		@Parameter(description = "训练任务ID", example = "1") @PathVariable @NotNull Long id,
		@RequestParam(value = "logPath", required = false) String logPath) {

		log.info("获取训练状态：ID={}?logPath={}", id, logPath);
		AlgorithmTraining training = vlsAlgorithmTrainingService.selectAlgorithmTrainingById(id);
		if (training == null) {
			return R.fail("找不到训练任务");
		}
		if (training.getTrainStatus() == AlgorithmTrainingStatusEnum.completed
			|| training.getTrainStatus() == AlgorithmTrainingStatusEnum.failed
			|| training.getTrainStatus() == AlgorithmTrainingStatusEnum.stop) {
			RemoteTrainingService.TrainingProgress persistedProgress = new RemoteTrainingService.TrainingProgress();
			persistedProgress.setTaskId(id);
			persistedProgress.setStatus(training.getTrainStatus().getCode());
			persistedProgress.setCompleted(training.getTrainStatus() == AlgorithmTrainingStatusEnum.completed);
			persistedProgress.setFailed(training.getTrainStatus() == AlgorithmTrainingStatusEnum.failed);
			persistedProgress.setPercentage(training.getProgress());
			persistedProgress.setMessage(training.getErrorMessage());
			return R.data(persistedProgress);
		}
		RemoteTrainingService.TrainingProgress progress = remoteTrainingService.getProgress(id, logPath);
		if (progress == null) {
			progress = new RemoteTrainingService.TrainingProgress();
			progress.setTaskId(id);
			progress.setStatus(training.getTrainStatus() == null
				? AlgorithmTrainingStatusEnum.training.getCode()
				: training.getTrainStatus().getCode());
			progress.setMessage("暂未读取到训练进度");
		} else if (progress.isCompleted()) {
			AlgorithmTraining update = new AlgorithmTraining();
			update.setId(id);
			update.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
			update.setProgress(100);
			update.setEndTime(new Date());
			vlsAlgorithmTrainingService.updateAlgorithmTraining(update);
		} else if (progress.isFailed()) {
			AlgorithmTraining update = new AlgorithmTraining();
			update.setId(id);
			update.setTrainStatus(AlgorithmTrainingStatusEnum.failed);
			update.setErrorMessage(progress.getMessage());
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
			log.warn("模型转换异常: id={}, format={}", trainingId, format);
			return null;
		} catch (ExecutionException executionException) {
			log.warn("模型转换异常: id={}, format={}, error={}", trainingId, format, executionException.getMessage());
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
	@Operation(summary = "下载模型文件", description = "从远程服务器下载训练好的模型文件")
	public void downloadModel(@RequestParam String id, @RequestParam String type, HttpServletResponse response) {
		try {
			log.info("下载模型文件: {}", type);

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
			// 从远程服务器下载文件
			SSHService.SSHExecutionResult result = sshService.executeCommand(
				sshProperties.getHost(),
				sshProperties.getPort(),
				sshProperties.getUsername(),
				sshProperties.getPassword(),
				String.format("base64 %s", downloadPath)
			);

			if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
				// 清理base64内容，移除可能的换行符和其他字符
				String base64Content = result.getOutput().trim().replaceAll("\\s+", "");
				log.info("Base64内容长度: {}", base64Content.length());

				// 解码base64内容
				byte[] fileContent = java.util.Base64.getDecoder().decode(base64Content);

				String fileName = downloadPath.substring(downloadPath.lastIndexOf('/') + 1);
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
						boolean updated = algorithmModelService.update(new AlgorithmModel(), updateWrapper);
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
				response.getWriter().write("Model file not found: " + downloadPath);
				log.error("模型文件不存在: {}", downloadPath);
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
