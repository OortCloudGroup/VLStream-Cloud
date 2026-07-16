/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

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
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 算法表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithm")
@Tag(name = "算法表", description = "算法表接口")
public class VlsAlgorithmController extends BladeController {

	private final IVlsAlgorithmService vlsAlgorithmService;

	/**
	 * 算法表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAlgorithm")
	public R<AlgorithmVO> detail(Algorithm vlsAlgorithm) {
		Algorithm detail = vlsAlgorithmService.getOne(Condition.getQueryWrapper(vlsAlgorithm));
		return R.data(VlsAlgorithmWrapper.build().entityVO(detail));
	}

	/**
	 * 算法表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAlgorithm")
	public R<IPage<AlgorithmVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithm, Query query) {
		IPage<Algorithm> pages = vlsAlgorithmService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithm, Algorithm.class));
		return R.data(VlsAlgorithmWrapper.build().pageVO(pages));
	}


	/**
	 * 算法表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAlgorithm")
	public R<IPage<AlgorithmVO>> page(AlgorithmVO vlsAlgorithm, Query query) {
		IPage<AlgorithmVO> pages = vlsAlgorithmService.selectVlsAlgorithmPage(Condition.getPage(query), vlsAlgorithm);
		for (AlgorithmVO algorithm : pages.getRecords()) {
			algorithm.setCategoryName(algorithm.getCategory().getDescription());
		}
		return R.data(pages);
	}

	/**
	 * 算法表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAlgorithm")
	public R save(@Valid @RequestBody Algorithm vlsAlgorithm) {
		return R.status(vlsAlgorithmService.save(vlsAlgorithm));
	}

	/**
	 * 算法表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAlgorithm")
	public R update(@Valid @RequestBody Algorithm vlsAlgorithm) {
		return R.status(vlsAlgorithmService.updateById(vlsAlgorithm));
	}

	/**
	 * 算法表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAlgorithm")
	public R submit(@Valid @RequestBody Algorithm vlsAlgorithm) {
		return R.status(vlsAlgorithmService.saveOrUpdate(vlsAlgorithm));
	}

	/**
	 * 算法表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAlgorithm")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAlgorithm")
	public void exportVlsAlgorithm(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithm, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<Algorithm> queryWrapper = Condition.getQueryWrapper(vlsAlgorithm, Algorithm.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmExcel> list = vlsAlgorithmService.exportVlsAlgorithm(queryWrapper);
		ExcelUtil.export(response, "算法表数据" + DateUtil.today(), "算法表数据表", list, VlsAlgorithmExcel.class);
	}

	/**
	 * 根据仓库ID查询算法列表
	 */
	@GetMapping("/repository/{repositoryId}")
	@Operation(summary = "根据仓库ID查询算法列表", description = "获取指定仓库下的所有算法")
	public R<List<Algorithm>> getAlgorithmsByRepositoryId(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long repositoryId) {

		log.info("根据仓库ID查询算法列表：{}", repositoryId);

		List<Algorithm> algorithms = vlsAlgorithmService.getByRepositoryId(repositoryId);
		return R.data(algorithms);
	}

	/**
	 * 根据分类查询算法列表
	 */
	@GetMapping("/category/{category}")
	@Operation(summary = "根据分类查询算法列表", description = "获取指定分类的所有算法")
	public R<List<Algorithm>> getAlgorithmsByCategory(
		@Parameter(description = "算法类型", example = "person-detection") @PathVariable String category) {

		log.info("根据分类查询算法列表：{}", category);

		List<Algorithm> algorithms = vlsAlgorithmService.getByCategory(category);
		return R.data(algorithms);
	}

	/**
	 * 根据ID查询算法详情
	 */
	@GetMapping("/{id}")
	@Operation(summary = "查询算法详情", description = "根据ID获取算法详细信息")
	public R<Algorithm> getAlgorithmById(
		@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("查询算法详情：ID={}", id);

		Algorithm algorithm = vlsAlgorithmService.getById(id);
		if (algorithm == null) {
			return R.fail("算法不存在");
		}

		return R.data(algorithm);
	}

	/**
	 * 创建算法
	 */
	@PostMapping
	@Operation(summary = "创建算法", description = "新增算法")
	public R<String> createAlgorithm(@Valid @RequestBody Algorithm algorithm) {
		log.info("创建算法：{}", algorithm.getName());

		boolean success = vlsAlgorithmService.createAlgorithm(algorithm);
		if (success) {
			return R.success("算法创建成功");
		} else {
			return R.fail("算法创建失败，同一仓库下名称可能已存在");
		}
	}

	/**
	 * 更新算法
	 */
	@PutMapping("/{id}")
	@Operation(summary = "更新算法", description = "根据ID更新算法信息")
	public R<String> updateAlgorithm(
		@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody Algorithm algorithm) {

		log.info("更新算法：ID={}", id);

		algorithm.setId(id);
		boolean success = vlsAlgorithmService.updateAlgorithm(algorithm);

		if (success) {
			return R.success("算法更新成功");
		} else {
			return R.fail("算法更新失败");
		}
	}

	/**
	 * 删除算法
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "删除算法", description = "根据ID删除算法（软删除）")
	public R<String> deleteAlgorithm(
		@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("删除算法：ID={}", id);

		boolean success = vlsAlgorithmService.deleteAlgorithm(id);
		if (success) {
			return R.success("算法删除成功");
		} else {
			return R.fail("算法删除失败");
		}
	}

	/**
	 * 批量删除算法
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "批量删除算法", description = "根据ID列表批量删除算法")
	public R<String> batchDeleteAlgorithms(@RequestBody List<Long> ids) {
		log.info("批量删除算法：IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要删除的算法");
		}

		boolean success = vlsAlgorithmService.batchDeleteAlgorithms(ids);
		if (success) {
			return R.success("算法批量删除成功");
		} else {
			return R.fail("算法批量删除失败");
		}
	}

	/**
	 * 更新部署状态
	 */
	@PutMapping("/{id}/deploy-status")
	@Operation(summary = "更新部署状态", description = "更新算法的部署状态")
	public R<String> updateDeployStatus(
		@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "部署状态", example = "deployed") @RequestParam @NotNull String deployStatus) {

		log.info("更新算法部署状态：ID={}, Status={}", id, deployStatus);

		boolean success = vlsAlgorithmService.updateDeployStatus(id, deployStatus);
		if (success) {
			return R.success("部署状态更新成功");
		} else {
			return R.fail("部署状态更新失败");
		}
	}

	/**
	 * 批量更新部署状态
	 */
	@PutMapping("/batch/deploy-status")
	@Operation(summary = "批量更新部署状态", description = "批量更新算法的部署状态")
	public R<String> batchUpdateDeployStatus(
		@RequestBody List<Long> ids,
		@Parameter(description = "部署状态", example = "deployed") @RequestParam @NotNull String deployStatus) {

		log.info("批量更新算法部署状态：IDs={}, Status={}", ids, deployStatus);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要更新的算法");
		}

		boolean success = vlsAlgorithmService.batchUpdateDeployStatus(ids, deployStatus);
		if (success) {
			return R.success("部署状态批量更新成功");
		} else {
			return R.fail("部署状态批量更新失败");
		}
	}

	/**
	 * 部署算法到设备
	 */
	@PostMapping("/{id}/deploy")
	@Operation(summary = "部署算法到设备", description = "将算法部署到指定设备")
	public R<String> deployAlgorithmToDevices(
		@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long algorithmId,
		@RequestBody List<Long> deviceIds) {

		log.info("部署算法到设备：AlgorithmId={}, DeviceIds={}", algorithmId, deviceIds);

		if (deviceIds == null || deviceIds.isEmpty()) {
			return R.fail("请选择要部署的设备");
		}

		boolean success = vlsAlgorithmService.deployAlgorithmToDevices(algorithmId, deviceIds);
		if (success) {
			return R.success("算法部署成功");
		} else {
			return R.fail("算法部署失败");
		}
	}

	/**
	 * 算法评估
	 */
	@PostMapping("/{algorithmId}/evaluate")
	@Operation(summary = "算法评估", description = "对算法进行性能评估")
	public R<Map<String, Object>> evaluateAlgorithm(@Parameter(description = "算法ID", example = "1") @PathVariable @NotNull Long algorithmId) {

		log.info("算法评估：AlgorithmId={}", algorithmId);

		Map<String, Object> result = vlsAlgorithmService.evaluateAlgorithm(algorithmId);
		if (result != null) {
			return R.data(result);
		} else {
			return R.fail("算法评估失败，算法不存在");
		}
	}

	/**
	 * 获取算法分类统计
	 */
	@GetMapping("/statistics/category")
	@Operation(summary = "获取算法分类统计", description = "获取各分类的算法数量统计")
	public R<List<Map<String, Object>>> getCategoryStatistics() {
		log.info("获取算法分类统计");

		List<Map<String, Object>> statistics = vlsAlgorithmService.getCategoryStatistics();
		return R.data(statistics);
	}

	/**
	 * 获取算法类型统计
	 */
	@GetMapping("/statistics/type")
	@Operation(summary = "获取算法类型统计", description = "获取各类型的算法数量统计")
	public R<List<Map<String, Object>>> getTypeStatistics() {
		log.info("获取算法类型统计");

		List<Map<String, Object>> statistics = vlsAlgorithmService.getTypeStatistics();
		return R.data(statistics);
	}

	/**
	 * 获取部署状态统计
	 */
	@GetMapping("/statistics/deploy-status")
	@Operation(summary = "获取部署状态统计", description = "获取各部署状态的算法数量统计")
	public R<List<Map<String, Object>>> getDeployStatusStatistics() {
		log.info("获取部署状态统计");

		List<Map<String, Object>> statistics = vlsAlgorithmService.getDeployStatusStatistics();
		return R.data(statistics);
	}

	/**
	 * 统计某仓库下的算法数量
	 */
	@GetMapping("/count/repository/{repositoryId}")
	@Operation(summary = "统计某仓库下的算法数量", description = "获取指定仓库的算法数量")
	public R<Long> countByRepositoryId(
		@Parameter(description = "仓库ID", example = "1") @PathVariable @NotNull Long repositoryId) {

		log.info("统计某仓库下的算法数量：RepositoryId={}", repositoryId);

		Long count = vlsAlgorithmService.countByRepositoryId(repositoryId);
		return R.data(count);
	}

}
