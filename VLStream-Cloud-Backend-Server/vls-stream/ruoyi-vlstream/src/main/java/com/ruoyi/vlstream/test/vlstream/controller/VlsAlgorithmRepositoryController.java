package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmRepositoryExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmRepositoryVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmRepositoryService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmRepositoryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 算法仓库表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmRepository")
@Tag(name = "算法仓库表", description = "算法仓库表接口")
public class VlsAlgorithmRepositoryController extends BladeController {

	private final IVlsAlgorithmRepositoryService vlsAlgorithmRepositoryService;

	/**
	 * 算法仓库表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAlgorithmRepository")
	public R<AlgorithmRepositoryVO> detail(AlgorithmRepository vlsAlgorithmRepository) {
		AlgorithmRepository detail = vlsAlgorithmRepositoryService.getOne(Condition.getQueryWrapper(vlsAlgorithmRepository));
		return R.data(VlsAlgorithmRepositoryWrapper.build().entityVO(detail));
	}

	/**
	 * 算法仓库表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAlgorithmRepository")
	public R<IPage<AlgorithmRepositoryVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmRepository, Query query) {
		IPage<AlgorithmRepository> pages = vlsAlgorithmRepositoryService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmRepository, AlgorithmRepository.class));
		return R.data(VlsAlgorithmRepositoryWrapper.build().pageVO(pages));
	}


	/**
	 * 算法仓库表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAlgorithmRepository")
	public R<IPage<AlgorithmRepositoryVO>> page(AlgorithmRepositoryVO vlsAlgorithmRepository, Query query) {
		IPage<AlgorithmRepositoryVO> pages = vlsAlgorithmRepositoryService.selectVlsAlgorithmRepositoryPage(Condition.getPage(query), vlsAlgorithmRepository);
		return R.data(pages);
	}

	/**
	 * 算法仓库表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAlgorithmRepository")
	public R save(@Valid @RequestBody AlgorithmRepository vlsAlgorithmRepository) {
		return R.status(vlsAlgorithmRepositoryService.save(vlsAlgorithmRepository));
	}

	/**
	 * 算法仓库表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAlgorithmRepository")
	public R update(@Valid @RequestBody AlgorithmRepository vlsAlgorithmRepository) {
		return R.status(vlsAlgorithmRepositoryService.updateById(vlsAlgorithmRepository));
	}

	/**
	 * 算法仓库表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAlgorithmRepository")
	public R submit(@Valid @RequestBody AlgorithmRepository vlsAlgorithmRepository) {
		return R.status(vlsAlgorithmRepositoryService.saveOrUpdate(vlsAlgorithmRepository));
	}

	/**
	 * 算法仓库表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmRepositoryService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAlgorithmRepository")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAlgorithmRepository")
	public void exportVlsAlgorithmRepository(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmRepository, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmRepository> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmRepository, AlgorithmRepository.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmRepositoryEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmRepositoryEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmRepositoryExcel> list = vlsAlgorithmRepositoryService.exportVlsAlgorithmRepository(queryWrapper);
		ExcelUtil.export(response, "算法仓库表数据" + DateUtil.today(), "算法仓库表数据表", list, VlsAlgorithmRepositoryExcel.class);
	}

	/**
	 * 查询所有启用的算法仓库
	 */
	@GetMapping("/enabled")
	@Operation(summary = "查询所有启用的算法仓库", description = "获取状态为启用的所有算法仓库")
	public R<List<AlgorithmRepository>> getEnabledRepositories() {
		log.info("查询所有启用的算法仓库");

		List<AlgorithmRepository> repositories = vlsAlgorithmRepositoryService.getEnabledRepositories();
		return R.data(repositories);
	}

	/**
	 * 根据类型查询算法仓库
	 */
	@GetMapping("/type/{repositoryType}")
	@Operation(summary = "根据类型查询算法仓库", description = "根据仓库类型获取算法仓库列表")
	public R<List<AlgorithmRepository>> getRepositoriesByType(
		@Parameter(description = "仓库类型", example = "extended") @PathVariable String repositoryType) {

		log.info("根据类型查询算法仓库：{}", repositoryType);

		List<AlgorithmRepository> repositories = vlsAlgorithmRepositoryService.getByRepositoryType(repositoryType);
		return R.data(repositories);
	}

	/**
	 * 根据ID查询算法仓库详情
	 */
	@GetMapping("/{id}")
	@Operation(summary = "查询算法仓库详情", description = "根据ID获取算法仓库详细信息")
	public R<AlgorithmRepository> getRepositoryById(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("查询算法仓库详情：ID={}", id);

		AlgorithmRepository repository = vlsAlgorithmRepositoryService.getById(id);
		if (repository == null) {
			return R.fail("算法仓库不存在");
		}

		return R.data(repository);
	}

	/**
	 * 创建算法仓库
	 */
	@PostMapping
	@Operation(summary = "创建算法仓库", description = "新增算法仓库")
	public R<String> createRepository(@Valid @RequestBody AlgorithmRepository repository) {
		log.info("创建算法仓库：{}", repository.getName());

		boolean success = vlsAlgorithmRepositoryService.createRepository(repository);
		if (success) {
			return R.success("算法仓库创建成功");
		} else {
			return R.fail("算法仓库创建失败，名称可能已存在");
		}
	}

	/**
	 * 更新算法仓库
	 */
	@PutMapping("/{id}")
	@Operation(summary = "更新算法仓库", description = "根据ID更新算法仓库信息")
	public R<String> updateRepository(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody AlgorithmRepository repository) {

		log.info("更新算法仓库：ID={}", id);

		repository.setId(id);
		boolean success = vlsAlgorithmRepositoryService.updateRepository(repository);

		if (success) {
			return R.success("算法仓库更新成功");
		} else {
			return R.fail("算法仓库更新失败");
		}
	}

	/**
	 * 删除算法仓库
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "删除算法仓库", description = "根据ID删除算法仓库（软删除）")
	public R<String> deleteRepository(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("删除算法仓库：ID={}", id);

		boolean success = vlsAlgorithmRepositoryService.deleteRepository(id);
		if (success) {
			return R.success("算法仓库删除成功");
		} else {
			return R.fail("算法仓库删除失败，基础预置算法库不允许删除");
		}
	}

	/**
	 * 批量删除算法仓库
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "批量删除算法仓库", description = "根据ID列表批量删除算法仓库")
	public R<String> batchDeleteRepositories(@RequestBody List<Long> ids) {
		log.info("批量删除算法仓库：IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要删除的算法仓库");
		}

		boolean success = vlsAlgorithmRepositoryService.batchDeleteRepositories(ids);
		if (success) {
			return R.success("算法仓库批量删除成功");
		} else {
			return R.fail("算法仓库批量删除失败，部分仓库不允许删除");
		}
	}

	/**
	 * 更新仓库状态
	 */
	@PutMapping("/{id}/status")
	@Operation(summary = "更新仓库状态", description = "启用或禁用算法仓库")
	public R<String> updateRepositoryStatus(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "新状态", example = "enabled") @RequestParam @NotNull String status) {

		log.info("更新算法仓库状态：ID={}, Status={}", id, status);

		boolean success = vlsAlgorithmRepositoryService.updateRepositoryStatus(id, status);
		if (success) {
			return R.success("算法仓库状态更新成功");
		} else {
			return R.fail("算法仓库状态更新失败");
		}
	}

	/**
	 * 批量更新仓库状态
	 */
	@PutMapping("/batch/status")
	@Operation(summary = "批量更新仓库状态", description = "批量启用或禁用算法仓库")
	public R<String> batchUpdateRepositoryStatus(
		@RequestBody List<Long> ids,
		@Parameter(description = "新状态", example = "enabled") @RequestParam @NotNull String status) {

		log.info("批量更新算法仓库状态：IDs={}, Status={}", ids, status);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要更新的算法仓库");
		}

		boolean success = vlsAlgorithmRepositoryService.batchUpdateRepositoryStatus(ids, status);
		if (success) {
			return R.success("算法仓库状态批量更新成功");
		} else {
			return R.fail("算法仓库状态批量更新失败");
		}
	}

	/**
	 * 统计算法仓库数量
	 */
	@GetMapping("/count")
	@Operation(summary = "统计算法仓库数量", description = "获取算法仓库总数")
	public R<Long> countRepositories() {
		log.info("统计算法仓库数量");

		Long count = vlsAlgorithmRepositoryService.countRepositories();
		return R.data(count);
	}

	/**
	 * 刷新仓库算法数量
	 */
	@PutMapping("/{id}/refresh-count")
	@Operation(summary = "刷新仓库算法数量", description = "重新计算并更新仓库的算法数量")
	public R<String> refreshAlgorithmCount(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("刷新算法仓库算法数量：ID={}", id);

		vlsAlgorithmRepositoryService.updateAlgorithmCount(id);
		return R.success("算法数量刷新成功");
	}

}
