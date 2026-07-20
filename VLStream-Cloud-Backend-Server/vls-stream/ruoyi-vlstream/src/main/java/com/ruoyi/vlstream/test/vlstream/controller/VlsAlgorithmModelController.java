/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;

import org.springblade.core.tool.api.R;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmModelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmModelService;
import com.ruoyi.vlstream.test.vlstream.service.ModelFileDownloadService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmModelWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 算法模型表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmModel")
@Tag(name = "算法模型表", description = "算法模型表接口")
public class VlsAlgorithmModelController extends BladeController {

	private final IVlsAlgorithmModelService vlsAlgorithmModelService;
	private final ModelFileDownloadService modelFileDownloadService;

	/**
	 * 算法模型表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAlgorithmModel")
	public R<AlgorithmModelVO> detail(AlgorithmModel vlsAlgorithmModel) {
		AlgorithmModel detail = vlsAlgorithmModelService.getOne(Condition.getQueryWrapper(vlsAlgorithmModel));
		return R.data(VlsAlgorithmModelWrapper.build().entityVO(detail));
	}

	/**
	 * 算法模型表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAlgorithmModel")
	public R<IPage<AlgorithmModelVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmModel, Query query) {
		IPage<AlgorithmModel> pages = vlsAlgorithmModelService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmModel, AlgorithmModel.class));
		return R.data(VlsAlgorithmModelWrapper.build().pageVO(pages));
	}


	/**
	 * 算法模型表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAlgorithmModel")
	public R<IPage<AlgorithmModelVO>> page(AlgorithmModelVO vlsAlgorithmModel, Query query) {
		IPage<AlgorithmModelVO> pages = vlsAlgorithmModelService.selectVlsAlgorithmModelPage(Condition.getPage(query), vlsAlgorithmModel);
		return R.data(pages);
	}

	/**
	 * 算法模型表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAlgorithmModel")
	public R save(@Valid @RequestBody AlgorithmModel vlsAlgorithmModel) {
		return R.status(vlsAlgorithmModelService.save(vlsAlgorithmModel));
	}

	/**
	 * 算法模型表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAlgorithmModel")
	public R update(@Valid @RequestBody AlgorithmModel vlsAlgorithmModel) {
		return R.status(vlsAlgorithmModelService.updateById(vlsAlgorithmModel));
	}

	/**
	 * 算法模型表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAlgorithmModel")
	public R submit(@Valid @RequestBody AlgorithmModel vlsAlgorithmModel) {
		return R.status(vlsAlgorithmModelService.saveOrUpdate(vlsAlgorithmModel));
	}

	/**
	 * 算法模型表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmModelService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAlgorithmModel")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAlgorithmModel")
	public void exportVlsAlgorithmModel(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmModel, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmModel> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmModel, AlgorithmModel.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmModelEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmModelEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmModelExcel> list = vlsAlgorithmModelService.exportVlsAlgorithmModel(queryWrapper);
		ExcelUtil.export(response, "算法模型表数据" + DateUtil.today(), "算法模型表数据表", list, VlsAlgorithmModelExcel.class);
	}

	@Operation(description = "根据ID查询算法模型详情")
	@GetMapping("/{id}")
	public R<AlgorithmModel> getModelById(@PathVariable Long id) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.getModelById(id);
			if (model == null) {
				return R.fail("模型不存在");
			}
			return R.data(model);
		} catch (Exception e) {
			log.error("查询算法模型详情失败", e);
			return R.fail("查询算法模型详情失败：" + e.getMessage());
		}
	}

	@Operation(description = "创建算法模型")
	@PostMapping("/create")
	public R<AlgorithmModel> createModel(@Valid @RequestBody AlgorithmModelVO createDTO) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.createModel(createDTO);
			return R.data(model);
		} catch (Exception e) {
			log.error("创建算法模型失败", e);
			return R.fail("创建算法模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "删除算法模型")
	@DeleteMapping("/{id}")
	public R<Boolean> deleteModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.deleteModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("删除算法模型失败", e);
			return R.fail("删除算法模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "批量删除算法模型")
	@DeleteMapping("/batch")
	public R<Boolean> batchDeleteModel(@RequestBody List<Long> ids) {
		try {
			boolean success = vlsAlgorithmModelService.batchDeleteModel(ids);
			return R.data(success);
		} catch (Exception e) {
			log.error("批量删除算法模型失败", e);
			return R.fail("批量删除算法模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "根据算法ID查询模型列表")
	@GetMapping("/algorithm/{algorithmId}")
	public R<List<AlgorithmModel>> getModelsByAlgorithmId(@PathVariable Long algorithmId) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getModelsByAlgorithmId(algorithmId);
			return R.data(models);
		} catch (Exception e) {
			log.error("根据算法ID查询模型列表失败", e);
			return R.fail("根据算法ID查询模型列表失败：" + e.getMessage());
		}
	}

	@Operation(description = "根据训练任务ID查询模型列表")
	@GetMapping("/training/{trainingId}")
	public R<List<AlgorithmModel>> getModelsByTrainingId(@PathVariable Long trainingId) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getModelsByTrainingId(trainingId);
			return R.data(models);
		} catch (Exception e) {
			log.error("根据训练任务ID查询模型列表失败", e);
			return R.fail("根据训练任务ID查询模型列表失败：" + e.getMessage());
		}
	}

	@Operation(description = "根据状态查询模型列表")
	@GetMapping("/status/{status}")
	public R<List<AlgorithmModel>> getModelsByStatus(@PathVariable String status) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getModelsByStatus(status);
			return R.data(models);
		} catch (Exception e) {
			log.error("根据状态查询模型列表失败", e);
			return R.fail("根据状态查询模型列表失败：" + e.getMessage());
		}
	}

	@Operation(description = "发布模型")
	@PostMapping("/publish/{id}")
	public R<Boolean> publishModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.publishModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("发布模型失败", e);
			return R.fail("发布模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "撤销发布模型")
	@PostMapping("/unpublish/{id}")
	public R<Boolean> unpublishModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.unpublishModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("撤销发布模型失败", e);
			return R.fail("撤销发布模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "批量发布模型")
	@PostMapping("/batch-publish")
	public R<Boolean> batchPublishModel(@RequestBody List<Long> ids) {
		try {
			boolean success = vlsAlgorithmModelService.batchPublishModel(ids);
			return R.data(success);
		} catch (Exception e) {
			log.error("批量发布模型失败", e);
			return R.fail("批量发布模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "下载模型")
	@GetMapping("/download/{id}")
	public R<String> downloadModel(@PathVariable Long id) {
		try {
			String filePath = vlsAlgorithmModelService.downloadModel(id);
			return R.success(filePath);
		} catch (Exception e) {
			log.error("下载模型失败", e);
			return R.fail("下载模型失败：" + e.getMessage());
		}
	}

	/**
	 * 按模型表 ID 下载已入库模型的指定格式文件。
	 */
	@Operation(description = "下载模型文件")
	@GetMapping("/{id}/download-file")
	public void downloadModelFile(@PathVariable Long id,
		@RequestParam(defaultValue = "pt") String type, HttpServletResponse response) {
		try {
			modelFileDownloadService.downloadModel(id, type, response);
		} catch (IllegalArgumentException e) {
			writeDownloadError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		} catch (java.io.FileNotFoundException e) {
			writeDownloadError(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
		} catch (Exception e) {
			log.error("下载模型文件失败", e);
			writeDownloadError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Download failed: " + e.getMessage());
		}
	}

	/**
	 * 在响应尚未提交时写入下载失败原因。
	 */
	private void writeDownloadError(HttpServletResponse response, int status, String message) {
		try {
			if (!response.isCommitted()) {
				response.setStatus(status);
				response.setContentType("text/plain;charset=UTF-8");
				response.getWriter().write(message);
			}
		} catch (Exception ex) {
			log.error("写入下载错误响应失败", ex);
		}
	}

	@Operation(description = "部署模型")
	@PostMapping("/deploy/{id}")
	public R<Boolean> deployModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.deployModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("部署模型失败", e);
			return R.fail("部署模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "检查模型名称和版本是否存在")
	@GetMapping("/check-name-version")
	public R<Boolean> checkModelNameAndVersion(@RequestParam String modelName, @RequestParam Integer version, @RequestParam(required = false) Long excludeId) {
		try {
			boolean exists = vlsAlgorithmModelService.checkModelNameAndVersion(modelName, version, excludeId);
			return R.data(exists);
		} catch (Exception e) {
			log.error("检查模型名称和版本失败", e);
			return R.fail("检查模型名称和版本失败：" + e.getMessage());
		}
	}

	@Operation(description = "根据算法ID和版本查询模型")
	@GetMapping("/algorithm/{algorithmId}/version/{version}")
	public R<AlgorithmModel> getModelByAlgorithmIdAndVersion(@PathVariable Long algorithmId, @PathVariable Integer version) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.getModelByAlgorithmIdAndVersion(algorithmId, version);
			return R.data(model);
		} catch (Exception e) {
			log.error("根据算法ID和版本查询模型失败", e);
			return R.fail("根据算法ID和版本查询模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "获取算法下最新版本的模型")
	@GetMapping("/algorithm/{algorithmId}/latest")
	public R<AlgorithmModel> getLatestModelByAlgorithmId(@PathVariable Long algorithmId) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.getLatestModelByAlgorithmId(algorithmId);
			return R.data(model);
		} catch (Exception e) {
			log.error("获取算法下最新版本的模型失败", e);
			return R.fail("获取算法下最新版本的模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "查询热门模型")
	@GetMapping("/popular")
	public R<List<AlgorithmModel>> getPopularModels(@RequestParam(defaultValue = "10") Integer limit) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getPopularModels(limit);
			return R.data(models);
		} catch (Exception e) {
			log.error("查询热门模型失败", e);
			return R.fail("查询热门模型失败：" + e.getMessage());
		}
	}

	@Operation(description = "根据创建人查询模型数量")
	@GetMapping("/count/creator/{createdBy}")
	public R<Long> countModelsByCreatedBy(@PathVariable Long createdBy) {
		try {
			Long count = vlsAlgorithmModelService.countModelsByCreatedBy(createdBy);
			return R.data(count);
		} catch (Exception e) {
			log.error("根据创建人查询模型数量失败", e);
			return R.fail("根据创建人查询模型数量失败：" + e.getMessage());
		}
	}

	@Operation(description = "获取算法模型的总大小")
	@GetMapping("/total-size")
	public R<Long> getTotalModelSize() {
		try {
			Long totalSize = vlsAlgorithmModelService.getTotalModelSize();
			return R.data(totalSize);
		} catch (Exception e) {
			log.error("获取算法模型的总大小失败", e);
			return R.fail("获取算法模型的总大小失败：" + e.getMessage());
		}
	}

}
