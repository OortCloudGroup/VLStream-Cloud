package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.vlstream.excel.VlsAlgorithmModelExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.vo.AlgorithmModelVO;
import org.springblade.vlstream.service.IVlsAlgorithmModelService;
import org.springblade.vlstream.wrapper.VlsAlgorithmModelWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Algorithm model table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmModel")
@Tag(name = "Algorithm model table", description = "Algorithm model table interface")
public class VlsAlgorithmModelController extends BladeController {

	private final IVlsAlgorithmModelService vlsAlgorithmModelService;

	/**
	 * Algorithm model table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsAlgorithmModel")
	public R<AlgorithmModelVO> detail(AlgorithmModel vlsAlgorithmModel) {
		AlgorithmModel detail = vlsAlgorithmModelService.getOne(Condition.getQueryWrapper(vlsAlgorithmModel));
		return R.data(VlsAlgorithmModelWrapper.build().entityVO(detail));
	}

	/**
	 * Algorithm model table paging
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmModel")
	public R<IPage<AlgorithmModelVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmModel, Query query) {
		IPage<AlgorithmModel> pages = vlsAlgorithmModelService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmModel, AlgorithmModel.class));
		return R.data(VlsAlgorithmModelWrapper.build().pageVO(pages));
	}


	/**
	 * Algorithm model table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmModel")
	public R<IPage<AlgorithmModelVO>> page(AlgorithmModelVO vlsAlgorithmModel, Query query) {
		IPage<AlgorithmModelVO> pages = vlsAlgorithmModelService.selectVlsAlgorithmModelPage(Condition.getPage(query), vlsAlgorithmModel);
		return R.data(pages);
	}

	/**
	 * Add algorithm model table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsAlgorithmModel")
	public R save(@Valid @RequestBody AlgorithmModel vlsAlgorithmModel) {
		return R.status(vlsAlgorithmModelService.save(vlsAlgorithmModel));
	}

	/**
	 * Modify algorithm model table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsAlgorithmModel")
	public R update(@Valid @RequestBody AlgorithmModel vlsAlgorithmModel) {
		return R.status(vlsAlgorithmModelService.updateById(vlsAlgorithmModel));
	}

	/**
	 * Add or modify algorithm model table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsAlgorithmModel")
	public R submit(@Valid @RequestBody AlgorithmModel vlsAlgorithmModel) {
		return R.status(vlsAlgorithmModelService.saveOrUpdate(vlsAlgorithmModel));
	}

	/**
	 * Delete algorithm model table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmModelService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAlgorithmModel")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsAlgorithmModel")
	public void exportVlsAlgorithmModel(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmModel, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmModel> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmModel, AlgorithmModel.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmModelEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmModelEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmModelExcel> list = vlsAlgorithmModelService.exportVlsAlgorithmModel(queryWrapper);
		ExcelUtil.export(response, "Algorithm model table data" + DateUtil.today(), "Algorithm model table database table", list, VlsAlgorithmModelExcel.class);
	}

	@Operation(description = "Query algorithm model details by ID")
	@GetMapping("/{id}")
	public R<AlgorithmModel> getModelById(@PathVariable Long id) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.getModelById(id);
			if (model == null) {
				return R.fail("Model does not exist");
			}
			return R.data(model);
		} catch (Exception e) {
			log.error("Failed to query algorithm model details", e);
			return R.fail("Failed to query algorithm model details:" + e.getMessage());
		}
	}

	@Operation(description = "Create algorithm model")
	@PostMapping("/create")
	public R<AlgorithmModel> createModel(@Valid @RequestBody AlgorithmModelVO createDTO) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.createModel(createDTO);
			return R.data(model);
		} catch (Exception e) {
			log.error("Failed to create algorithm model", e);
			return R.fail("Failed to create algorithm model:" + e.getMessage());
		}
	}

	@Operation(description = "Delete algorithm model")
	@DeleteMapping("/{id}")
	public R<Boolean> deleteModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.deleteModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to delete algorithm model", e);
			return R.fail("Failed to delete algorithm model:" + e.getMessage());
		}
	}

	@Operation(description = "Batch delete algorithm models")
	@DeleteMapping("/batch")
	public R<Boolean> batchDeleteModel(@RequestBody List<Long> ids) {
		try {
			boolean success = vlsAlgorithmModelService.batchDeleteModel(ids);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to batch delete algorithm models", e);
			return R.fail("Failed to batch delete algorithm models:" + e.getMessage());
		}
	}

	@Operation(description = "Query model list by algorithm ID")
	@GetMapping("/algorithm/{algorithmId}")
	public R<List<AlgorithmModel>> getModelsByAlgorithmId(@PathVariable Long algorithmId) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getModelsByAlgorithmId(algorithmId);
			return R.data(models);
		} catch (Exception e) {
			log.error("Failed to query model list by algorithm ID", e);
			return R.fail("Failed to query model list by algorithm ID:" + e.getMessage());
		}
	}

	@Operation(description = "Query model list by training task ID")
	@GetMapping("/training/{trainingId}")
	public R<List<AlgorithmModel>> getModelsByTrainingId(@PathVariable Long trainingId) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getModelsByTrainingId(trainingId);
			return R.data(models);
		} catch (Exception e) {
			log.error("Failed to query model list by training task ID", e);
			return R.fail("Failed to query model list by training task ID:" + e.getMessage());
		}
	}

	@Operation(description = "Query model list by status")
	@GetMapping("/status/{status}")
	public R<List<AlgorithmModel>> getModelsByStatus(@PathVariable String status) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getModelsByStatus(status);
			return R.data(models);
		} catch (Exception e) {
			log.error("Failed to query model list by status", e);
			return R.fail("Failed to query model list by status:" + e.getMessage());
		}
	}

	@Operation(description = "Publish model")
	@PostMapping("/publish/{id}")
	public R<Boolean> publishModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.publishModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to publish model", e);
			return R.fail("Failed to publish model:" + e.getMessage());
		}
	}

	@Operation(description = "Unpublish model")
	@PostMapping("/unpublish/{id}")
	public R<Boolean> unpublishModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.unpublishModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to unpublish model", e);
			return R.fail("Failed to unpublish model:" + e.getMessage());
		}
	}

	@Operation(description = "Batch publish models")
	@PostMapping("/batch-publish")
	public R<Boolean> batchPublishModel(@RequestBody List<Long> ids) {
		try {
			boolean success = vlsAlgorithmModelService.batchPublishModel(ids);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to batch publish models", e);
			return R.fail("Failed to batch publish models:" + e.getMessage());
		}
	}

	@Operation(description = "Download model")
	@GetMapping("/download/{id}")
	public R<String> downloadModel(@PathVariable Long id) {
		try {
			String filePath = vlsAlgorithmModelService.downloadModel(id);
			return R.success(filePath);
		} catch (Exception e) {
			log.error("Failed to download model", e);
			return R.fail("Failed to download model:" + e.getMessage());
		}
	}

	@Operation(description = "Deploy model")
	@PostMapping("/deploy/{id}")
	public R<Boolean> deployModel(@PathVariable Long id) {
		try {
			boolean success = vlsAlgorithmModelService.deployModel(id);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to deploy model", e);
			return R.fail("Failed to deploy model:" + e.getMessage());
		}
	}

	@Operation(description = "Check if model name and version exist")
	@GetMapping("/check-name-version")
	public R<Boolean> checkModelNameAndVersion(@RequestParam String modelName, @RequestParam Integer version, @RequestParam(required = false) Long excludeId) {
		try {
			boolean exists = vlsAlgorithmModelService.checkModelNameAndVersion(modelName, version, excludeId);
			return R.data(exists);
		} catch (Exception e) {
			log.error("Failed to check model name and version", e);
			return R.fail("Failed to check model name and version:" + e.getMessage());
		}
	}

	@Operation(description = "Query model by algorithm ID and version")
	@GetMapping("/algorithm/{algorithmId}/version/{version}")
	public R<AlgorithmModel> getModelByAlgorithmIdAndVersion(@PathVariable Long algorithmId, @PathVariable Integer version) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.getModelByAlgorithmIdAndVersion(algorithmId, version);
			return R.data(model);
		} catch (Exception e) {
			log.error("Failed to query model by algorithm ID and version", e);
			return R.fail("Failed to query model by algorithm ID and version:" + e.getMessage());
		}
	}

	@Operation(description = "Get the latest version model under the algorithm")
	@GetMapping("/algorithm/{algorithmId}/latest")
	public R<AlgorithmModel> getLatestModelByAlgorithmId(@PathVariable Long algorithmId) {
		try {
			AlgorithmModel model = vlsAlgorithmModelService.getLatestModelByAlgorithmId(algorithmId);
			return R.data(model);
		} catch (Exception e) {
			log.error("Failed to get the latest version model under the algorithm", e);
			return R.fail("Failed to get the latest version model under the algorithm:" + e.getMessage());
		}
	}

	@Operation(description = "Query Popular Models")
	@GetMapping("/popular")
	public R<List<AlgorithmModel>> getPopularModels(@RequestParam(defaultValue = "10") Integer limit) {
		try {
			List<AlgorithmModel> models = vlsAlgorithmModelService.getPopularModels(limit);
			return R.data(models);
		} catch (Exception e) {
			log.error("Failed to query popular models", e);
			return R.fail("Failed to query popular models:" + e.getMessage());
		}
	}

	@Operation(description = "Query model count by creator")
	@GetMapping("/count/creator/{createdBy}")
	public R<Long> countModelsByCreatedBy(@PathVariable Long createdBy) {
		try {
			Long count = vlsAlgorithmModelService.countModelsByCreatedBy(createdBy);
			return R.data(count);
		} catch (Exception e) {
			log.error("Failed to query model count by creator", e);
			return R.fail("Failed to query model count by creator:" + e.getMessage());
		}
	}

	@Operation(description = "Get total size of algorithm model")
	@GetMapping("/total-size")
	public R<Long> getTotalModelSize() {
		try {
			Long totalSize = vlsAlgorithmModelService.getTotalModelSize();
			return R.data(totalSize);
		} catch (Exception e) {
			log.error("Failed to get total size of algorithm model", e);
			return R.fail("Failed to get total size of algorithm model:" + e.getMessage());
		}
	}

}
