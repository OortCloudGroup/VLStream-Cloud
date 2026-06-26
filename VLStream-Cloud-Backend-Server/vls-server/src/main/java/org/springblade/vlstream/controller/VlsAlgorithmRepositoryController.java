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
import org.springblade.vlstream.excel.VlsAlgorithmRepositoryExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;
import org.springblade.vlstream.pojo.vo.AlgorithmRepositoryVO;
import org.springblade.vlstream.service.IVlsAlgorithmRepositoryService;
import org.springblade.vlstream.wrapper.VlsAlgorithmRepositoryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Algorithm repository table Controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmRepository")
@Tag(name = "Algorithm repository table", description = "Algorithm repository table interface")
public class VlsAlgorithmRepositoryController extends BladeController {

	private final IVlsAlgorithmRepositoryService vlsAlgorithmRepositoryService;

	/**
	 * Algorithm repository table Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsAlgorithmRepository")
	public R<AlgorithmRepositoryVO> detail(AlgorithmRepository vlsAlgorithmRepository) {
		AlgorithmRepository detail = vlsAlgorithmRepositoryService.getOne(Condition.getQueryWrapper(vlsAlgorithmRepository));
		return R.data(VlsAlgorithmRepositoryWrapper.build().entityVO(detail));
	}

	/**
	 * Page algorithm repository table
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmRepository")
	public R<IPage<AlgorithmRepositoryVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmRepository, Query query) {
		IPage<AlgorithmRepository> pages = vlsAlgorithmRepositoryService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmRepository, AlgorithmRepository.class));
		return R.data(VlsAlgorithmRepositoryWrapper.build().pageVO(pages));
	}


	/**
	 * Algorithm repository table Custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmRepository")
	public R<IPage<AlgorithmRepositoryVO>> page(AlgorithmRepositoryVO vlsAlgorithmRepository, Query query) {
		IPage<AlgorithmRepositoryVO> pages = vlsAlgorithmRepositoryService.selectVlsAlgorithmRepositoryPage(Condition.getPage(query), vlsAlgorithmRepository);
		return R.data(pages);
	}

	/**
	 * Add algorithm repository table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsAlgorithmRepository")
	public R save(@Valid @RequestBody AlgorithmRepository vlsAlgorithmRepository) {
		return R.status(vlsAlgorithmRepositoryService.save(vlsAlgorithmRepository));
	}

	/**
	 * Modify algorithm repository table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsAlgorithmRepository")
	public R update(@Valid @RequestBody AlgorithmRepository vlsAlgorithmRepository) {
		return R.status(vlsAlgorithmRepositoryService.updateById(vlsAlgorithmRepository));
	}

	/**
	 * Add or modify algorithm repository table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsAlgorithmRepository")
	public R submit(@Valid @RequestBody AlgorithmRepository vlsAlgorithmRepository) {
		return R.status(vlsAlgorithmRepositoryService.saveOrUpdate(vlsAlgorithmRepository));
	}

	/**
	 * Delete algorithm repository table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmRepositoryService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAlgorithmRepository")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsAlgorithmRepository")
	public void exportVlsAlgorithmRepository(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmRepository, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmRepository> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmRepository, AlgorithmRepository.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmRepositoryEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmRepositoryEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmRepositoryExcel> list = vlsAlgorithmRepositoryService.exportVlsAlgorithmRepository(queryWrapper);
		ExcelUtil.export(response, "Algorithm repository table data" + DateUtil.today(), "Algorithm repository table data table", list, VlsAlgorithmRepositoryExcel.class);
	}

	/**
	 * Query All Enabled Algorithm Repositories
	 */
	@GetMapping("/enabled")
	@Operation(summary = "Query All Enabled Algorithm Repositories", description = "Get all algorithm repositories with status enabled")
	public R<List<AlgorithmRepository>> getEnabledRepositories() {
		log.info("Query All Enabled Algorithm Repositories");

		List<AlgorithmRepository> repositories = vlsAlgorithmRepositoryService.getEnabledRepositories();
		return R.data(repositories);
	}

	/**
	 * Query algorithm repository by type
	 */
	@GetMapping("/type/{repositoryType}")
	@Operation(summary = "Query algorithm repository by type", description = "Get algorithm repository list by repository type")
	public R<List<AlgorithmRepository>> getRepositoriesByType(
		@Parameter(description = "Repository type", example = "extended") @PathVariable String repositoryType) {

		log.info("Query algorithm repository by type: {}", repositoryType);

		List<AlgorithmRepository> repositories = vlsAlgorithmRepositoryService.getByRepositoryType(repositoryType);
		return R.data(repositories);
	}

	/**
	 * Query algorithm repository details by ID
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Query Algorithm Repository Details", description = "Get algorithm repository detailed information by ID")
	public R<AlgorithmRepository> getRepositoryById(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Query algorithm repository details: ID={}", id);

		AlgorithmRepository repository = vlsAlgorithmRepositoryService.getById(id);
		if (repository == null) {
			return R.fail("Algorithm repository does not exist");
		}

		return R.data(repository);
	}

	/**
	 * Create algorithm repository
	 */
	@PostMapping
	@Operation(summary = "Create algorithm repository", description = "Add algorithm repository")
	public R<String> createRepository(@Valid @RequestBody AlgorithmRepository repository) {
		log.info("Create algorithm repository: {}", repository.getName());

		boolean success = vlsAlgorithmRepositoryService.createRepository(repository);
		if (success) {
			return R.success("Algorithm repository created successfully");
		} else {
			return R.fail("Failed to create algorithm repository, name may already exist");
		}
	}

	/**
	 * Update algorithm repository
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Update algorithm repository", description = "Update algorithm repository information by ID")
	public R<String> updateRepository(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody AlgorithmRepository repository) {

		log.info("Update algorithm repository: ID={}", id);

		repository.setId(id);
		boolean success = vlsAlgorithmRepositoryService.updateRepository(repository);

		if (success) {
			return R.success("Algorithm repository updated successfully");
		} else {
			return R.fail("Failed to update algorithm repository");
		}
	}

	/**
	 * Delete algorithm repository
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete algorithm repository", description = "Delete algorithm repository by ID (soft delete)")
	public R<String> deleteRepository(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Delete algorithm repository: ID={}", id);

		boolean success = vlsAlgorithmRepositoryService.deleteRepository(id);
		if (success) {
			return R.success("Algorithm repository deleted successfully");
		} else {
			return R.fail("Failed to delete algorithm repository, basic preset algorithm repository is not allowed to be deleted");
		}
	}

	/**
	 * Batch delete algorithm repositories
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "Batch delete algorithm repositories", description = "Batch delete algorithm repositories by ID list")
	public R<String> batchDeleteRepositories(@RequestBody List<Long> ids) {
		log.info("Batch delete algorithm repositories: IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select algorithm repositories to delete");
		}

		boolean success = vlsAlgorithmRepositoryService.batchDeleteRepositories(ids);
		if (success) {
			return R.success("Algorithm repositories batch deleted successfully");
		} else {
			return R.fail("Batch deletion of algorithm repositories failed, some repositories are not allowed to be deleted");
		}
	}

	/**
	 * Update repository status
	 */
	@PutMapping("/{id}/status")
	@Operation(summary = "Update repository status", description = "Enable or disable algorithm repository")
	public R<String> updateRepositoryStatus(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "New status", example = "enabled") @RequestParam @NotNull String status) {

		log.info("Update algorithm repository status: ID={}, Status={}", id, status);

		boolean success = vlsAlgorithmRepositoryService.updateRepositoryStatus(id, status);
		if (success) {
			return R.success("Algorithm repository status updated successfully");
		} else {
			return R.fail("Failed to update algorithm repository status");
		}
	}

	/**
	 * Batch update repository status
	 */
	@PutMapping("/batch/status")
	@Operation(summary = "Batch update repository status", description = "Batch enable or disable algorithm repositories")
	public R<String> batchUpdateRepositoryStatus(
		@RequestBody List<Long> ids,
		@Parameter(description = "New status", example = "enabled") @RequestParam @NotNull String status) {

		log.info("Batch update algorithm repository status: IDs={}, Status={}", ids, status);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select algorithm repositories to update");
		}

		boolean success = vlsAlgorithmRepositoryService.batchUpdateRepositoryStatus(ids, status);
		if (success) {
			return R.success("Algorithm repository status batch updated successfully");
		} else {
			return R.fail("Batch update of algorithm repository status failed");
		}
	}

	/**
	 * Count algorithm repositories
	 */
	@GetMapping("/count")
	@Operation(summary = "Count algorithm repositories", description = "Get total count of algorithm repositories")
	public R<Long> countRepositories() {
		log.info("Count algorithm repositories");

		Long count = vlsAlgorithmRepositoryService.countRepositories();
		return R.data(count);
	}

	/**
	 * Refresh algorithm quantity in the repository
	 */
	@PutMapping("/{id}/refresh-count")
	@Operation(summary = "Refresh algorithm quantity in the repository", description = "Recalculate and update the number of algorithms in the repository")
	public R<String> refreshAlgorithmCount(
		@Parameter(description = "Repository ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Refresh algorithm quantity in the algorithm repository: ID={}", id);

		vlsAlgorithmRepositoryService.updateAlgorithmCount(id);
		return R.success("Algorithm count refreshed successfully");
	}

}
