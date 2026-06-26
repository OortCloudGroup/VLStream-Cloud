package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.springblade.vlstream.excel.VlsAlgorithmExcel;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.vo.AlgorithmVO;
import org.springblade.vlstream.service.IVlsAlgorithmService;
import org.springblade.vlstream.wrapper.VlsAlgorithmWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Algorithm table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithm")
@Tag(name = "Algorithm table", description = "Algorithm table interface")
public class VlsAlgorithmController extends BladeController {

	private final IVlsAlgorithmService vlsAlgorithmService;

	/**
	 * Algorithm table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsAlgorithm")
	public R<AlgorithmVO> detail(Algorithm vlsAlgorithm) {
		Algorithm detail = vlsAlgorithmService.getOne(Condition.getQueryWrapper(vlsAlgorithm));
		return R.data(VlsAlgorithmWrapper.build().entityVO(detail));
	}

	/**
	 * Algorithm table paging
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithm")
	public R<IPage<AlgorithmVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithm, Query query) {
		IPage<Algorithm> pages = vlsAlgorithmService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithm, Algorithm.class));
		return R.data(VlsAlgorithmWrapper.build().pageVO(pages));
	}


	/**
	 * Algorithm table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithm")
	public R<IPage<AlgorithmVO>> page(AlgorithmVO vlsAlgorithm, Query query) {
		IPage<AlgorithmVO> pages = vlsAlgorithmService.selectVlsAlgorithmPage(Condition.getPage(query), vlsAlgorithm);
		for (AlgorithmVO algorithm : pages.getRecords()) {
			algorithm.setCategoryName(algorithm.getCategory().getDescription());
		}
		return R.data(pages);
	}

	/**
	 * Add algorithm table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsAlgorithm")
	public R save(@Valid @RequestBody Algorithm vlsAlgorithm) {
		return R.status(vlsAlgorithmService.save(vlsAlgorithm));
	}

	/**
	 * Modify algorithm table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsAlgorithm")
	public R update(@Valid @RequestBody Algorithm vlsAlgorithm) {
		return R.status(vlsAlgorithmService.updateById(vlsAlgorithm));
	}

	/**
	 * Add or modify algorithm table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsAlgorithm")
	public R submit(@Valid @RequestBody Algorithm vlsAlgorithm) {
		return R.status(vlsAlgorithmService.saveOrUpdate(vlsAlgorithm));
	}

	/**
	 * Delete algorithm table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAlgorithm")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsAlgorithm")
	public void exportVlsAlgorithm(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithm, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<Algorithm> queryWrapper = Condition.getQueryWrapper(vlsAlgorithm, Algorithm.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmExcel> list = vlsAlgorithmService.exportVlsAlgorithm(queryWrapper);
		ExcelUtil.export(response, "Algorithm table data" + DateUtil.today(), "Algorithm table database table", list, VlsAlgorithmExcel.class);
	}

	/**
	 * Query algorithm list by repository ID
	 */
	@GetMapping("/repository/{repositoryId}")
	@Operation(summary = "Query algorithm list by repository ID", description = "Get all algorithms under specified repository")
	public R<List<Algorithm>> getAlgorithmsByRepositoryId(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long repositoryId) {

		log.info("Query algorithm list by repository ID: {}", repositoryId);

		List<Algorithm> algorithms = vlsAlgorithmService.getByRepositoryId(repositoryId);
		return R.data(algorithms);
	}

	/**
	 * Query algorithm list by category
	 */
	@GetMapping("/category/{category}")
	@Operation(summary = "Query algorithm list by category", description = "Get all algorithms of specified category")
	public R<List<Algorithm>> getAlgorithmsByCategory(
		@Parameter(description = "Algorithm type", example = "person-detection") @PathVariable String category) {

		log.info("Query algorithm list by category: {}", category);

		List<Algorithm> algorithms = vlsAlgorithmService.getByCategory(category);
		return R.data(algorithms);
	}

	/**
	 * Query algorithm details by ID
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Query Algorithm Details", description = "Get algorithm detailed information by ID")
	public R<Algorithm> getAlgorithmById(
		@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Query algorithm details: ID={}", id);

		Algorithm algorithm = vlsAlgorithmService.getById(id);
		if (algorithm == null) {
			return R.fail("Algorithm does not exist");
		}

		return R.data(algorithm);
	}

	/**
	 * Create algorithm
	 */
	@PostMapping
	@Operation(summary = "Create algorithm", description = "Add algorithm")
	public R<String> createAlgorithm(@Valid @RequestBody Algorithm algorithm) {
		log.info("Create algorithm: {}", algorithm.getName());

		boolean success = vlsAlgorithmService.createAlgorithm(algorithm);
		if (success) {
			return R.success("Algorithm created successfully");
		} else {
			return R.fail("Failed to create algorithm, name may already exist under the same repository");
		}
	}

	/**
	 * Update algorithm
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Update algorithm", description = "Update algorithm information by ID")
	public R<String> updateAlgorithm(
		@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody Algorithm algorithm) {

		log.info("Update algorithm: ID={}", id);

		algorithm.setId(id);
		boolean success = vlsAlgorithmService.updateAlgorithm(algorithm);

		if (success) {
			return R.success("Algorithm update successfully");
		} else {
			return R.fail("Algorithm update failed");
		}
	}

	/**
	 * Delete algorithm
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete algorithm", description = "Delete algorithm by ID (soft delete)")
	public R<String> deleteAlgorithm(
		@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Delete algorithm: ID={}", id);

		boolean success = vlsAlgorithmService.deleteAlgorithm(id);
		if (success) {
			return R.success("Algorithm deleted successfully");
		} else {
			return R.fail("Failed to delete algorithm");
		}
	}

	/**
	 * Batch delete algorithms
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "Batch delete algorithms", description = "Batch delete algorithms by ID list")
	public R<String> batchDeleteAlgorithms(@RequestBody List<Long> ids) {
		log.info("Batch delete algorithms: IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select algorithms to delete");
		}

		boolean success = vlsAlgorithmService.batchDeleteAlgorithms(ids);
		if (success) {
			return R.success("Algorithms batch deleted successfully");
		} else {
			return R.fail("Batch deletion of algorithms failed");
		}
	}

	/**
	 * Update deployment status
	 */
	@PutMapping("/{id}/deploy-status")
	@Operation(summary = "Update deployment status", description = "Update algorithm deployment status")
	public R<String> updateDeployStatus(
		@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "Deployment status", example = "deployed") @RequestParam @NotNull String deployStatus) {

		log.info("Update algorithm deployment status: ID={}, Status={}", id, deployStatus);

		boolean success = vlsAlgorithmService.updateDeployStatus(id, deployStatus);
		if (success) {
			return R.success("Update of deployment status successful");
		} else {
			return R.fail("Update of deployment status failed");
		}
	}

	/**
	 * Batch update deployment status
	 */
	@PutMapping("/batch/deploy-status")
	@Operation(summary = "Batch update deployment status", description = "Batch update the deployment status of algorithms")
	public R<String> batchUpdateDeployStatus(
		@RequestBody List<Long> ids,
		@Parameter(description = "Deployment status", example = "deployed") @RequestParam @NotNull String deployStatus) {

		log.info("Batch update algorithm deployment status: IDs={}, Status={}", ids, deployStatus);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select algorithms to update");
		}

		boolean success = vlsAlgorithmService.batchUpdateDeployStatus(ids, deployStatus);
		if (success) {
			return R.success("Batch update of deployment status successful");
		} else {
			return R.fail("Batch update of deployment status failed");
		}
	}

	/**
	 * Deploy algorithm to device
	 */
	@PostMapping("/{id}/deploy")
	@Operation(summary = "Deploy algorithm to device", description = "Deploy algorithm to designated device")
	public R<String> deployAlgorithmToDevices(
		@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long algorithmId,
		@RequestBody List<Long> deviceIds) {

		log.info("Deploy algorithm to device: AlgorithmId={}, DeviceIds={}", algorithmId, deviceIds);

		if (deviceIds == null || deviceIds.isEmpty()) {
			return R.fail("Please select devices to deploy");
		}

		boolean success = vlsAlgorithmService.deployAlgorithmToDevices(algorithmId, deviceIds);
		if (success) {
			return R.success("Algorithm deployment succeeded");
		} else {
			return R.fail("Algorithm deployment failed");
		}
	}

	/**
	 * Algorithm evaluation
	 */
	@PostMapping("/{algorithmId}/evaluate")
	@Operation(summary = "Algorithm evaluation", description = "Evaluate algorithm performance")
	public R<Map<String, Object>> evaluateAlgorithm(@Parameter(description = "Algorithm ID", example = "1") @PathVariable @NotNull Long algorithmId) {

		log.info("Algorithm evaluation: AlgorithmId={}", algorithmId);

		Map<String, Object> result = vlsAlgorithmService.evaluateAlgorithm(algorithmId);
		if (result != null) {
			return R.data(result);
		} else {
			return R.fail("Algorithm evaluation failed: algorithm does not exist");
		}
	}

	/**
	 * Get algorithm category statistics
	 */
	@GetMapping("/statistics/category")
	@Operation(summary = "Get algorithm category statistics", description = "Get algorithm count statistics for each category")
	public R<List<Map<String, Object>>> getCategoryStatistics() {
		log.info("Get algorithm category statistics");

		List<Map<String, Object>> statistics = vlsAlgorithmService.getCategoryStatistics();
		return R.data(statistics);
	}

	/**
	 * Get algorithm type statistics
	 */
	@GetMapping("/statistics/type")
	@Operation(summary = "Get algorithm type statistics", description = "Get algorithm count statistics for each type")
	public R<List<Map<String, Object>>> getTypeStatistics() {
		log.info("Get algorithm type statistics");

		List<Map<String, Object>> statistics = vlsAlgorithmService.getTypeStatistics();
		return R.data(statistics);
	}

	/**
	 * Get deployment status statistics
	 */
	@GetMapping("/statistics/deploy-status")
	@Operation(summary = "Get deployment status statistics", description = "Get algorithm count statistics for each deployment status")
	public R<List<Map<String, Object>>> getDeployStatusStatistics() {
		log.info("Get deployment status statistics");

		List<Map<String, Object>> statistics = vlsAlgorithmService.getDeployStatusStatistics();
		return R.data(statistics);
	}

	/**
	 * Count algorithms under a repository
	 */
	@GetMapping("/count/repository/{repositoryId}")
	@Operation(summary = "Count algorithms under a repository", description = "Get algorithm count of specified repository")
	public R<Long> countByRepositoryId(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long repositoryId) {

		log.info("Count algorithms under a repository: RepositoryId={}", repositoryId);

		Long count = vlsAlgorithmService.countByRepositoryId(repositoryId);
		return R.data(count);
	}

}
