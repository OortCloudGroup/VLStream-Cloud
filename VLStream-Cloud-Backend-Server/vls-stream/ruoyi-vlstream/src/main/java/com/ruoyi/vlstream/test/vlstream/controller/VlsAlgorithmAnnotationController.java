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
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmAnnotationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmAnnotationVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmAnnotationService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmAnnotationWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 算法标注数据表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmAnnotation")
@Tag(name = "算法标注数据表", description = "算法标注数据表接口")
public class VlsAlgorithmAnnotationController extends BladeController {

	private final IVlsAlgorithmAnnotationService vlsAlgorithmAnnotationService;

	/**
	 * 算法标注数据表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAlgorithmAnnotation")
	public R<AlgorithmAnnotationVO> detail(AlgorithmAnnotation vlsAlgorithmAnnotation) {
		AlgorithmAnnotation detail = vlsAlgorithmAnnotationService.getOne(Condition.getQueryWrapper(vlsAlgorithmAnnotation));
		return R.data(VlsAlgorithmAnnotationWrapper.build().entityVO(detail));
	}

	/**
	 * 算法标注数据表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAlgorithmAnnotation")
	public R<IPage<AlgorithmAnnotationVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmAnnotation, Query query) {
		IPage<AlgorithmAnnotation> pages = vlsAlgorithmAnnotationService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmAnnotation, AlgorithmAnnotation.class));
		return R.data(VlsAlgorithmAnnotationWrapper.build().pageVO(pages));
	}


	/**
	 * 算法标注数据表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAlgorithmAnnotation")
	public R<IPage<AlgorithmAnnotationVO>> page(AlgorithmAnnotationVO vlsAlgorithmAnnotation, Query query) {
		IPage<AlgorithmAnnotationVO> pages = vlsAlgorithmAnnotationService.selectVlsAlgorithmAnnotationPage(Condition.getPage(query), vlsAlgorithmAnnotation);
		return R.data(pages);
	}

	/**
	 * 算法标注数据表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAlgorithmAnnotation")
	public R save(@Valid @RequestBody AlgorithmAnnotation vlsAlgorithmAnnotation) {
		return R.status(vlsAlgorithmAnnotationService.save(vlsAlgorithmAnnotation));
	}

	/**
	 * 算法标注数据表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAlgorithmAnnotation")
	public R update(@Valid @RequestBody AlgorithmAnnotation vlsAlgorithmAnnotation) {
		return R.status(vlsAlgorithmAnnotationService.updateById(vlsAlgorithmAnnotation));
	}

	/**
	 * 算法标注数据表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAlgorithmAnnotation")
	public R submit(@Valid @RequestBody AlgorithmAnnotation vlsAlgorithmAnnotation) {
		return R.status(vlsAlgorithmAnnotationService.saveOrUpdate(vlsAlgorithmAnnotation));
	}

	/**
	 * 算法标注数据表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmAnnotationService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAlgorithmAnnotation")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAlgorithmAnnotation")
	public void exportVlsAlgorithmAnnotation(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmAnnotation, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmAnnotation> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmAnnotation, AlgorithmAnnotation.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmAnnotationEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmAnnotationEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmAnnotationExcel> list = vlsAlgorithmAnnotationService.exportVlsAlgorithmAnnotation(queryWrapper);
		ExcelUtil.export(response, "算法标注数据表数据" + DateUtil.today(), "算法标注数据表数据表", list, VlsAlgorithmAnnotationExcel.class);
	}

	/**
	 * 根据标注类型查询标注列表
	 */
	@GetMapping("/type/{annotationType}")
	@Operation(summary = "根据标注类型查询标注列表", description = "获取指定类型的所有标注")
	public R<List<AlgorithmAnnotation>> getAnnotationsByType(
		@Parameter(description = "标注类型", example = "object_detection") @PathVariable String annotationType) {

		log.info("根据标注类型查询标注列表：{}", annotationType);

		List<AlgorithmAnnotation> annotations = vlsAlgorithmAnnotationService.getByAnnotationType(annotationType);
		return R.data(annotations);
	}

	/**
	 * 根据标注状态查询标注列表
	 */
	@GetMapping("/status/{annotationStatus}")
	@Operation(summary = "根据标注状态查询标注列表", description = "获取指定状态的所有标注")
	public R<List<AlgorithmAnnotation>> getAnnotationsByStatus(
		@Parameter(description = "标注状态", example = "partial") @PathVariable String annotationStatus) {

		log.info("根据标注状态查询标注列表：{}", annotationStatus);

		List<AlgorithmAnnotation> annotations = vlsAlgorithmAnnotationService.getByAnnotationStatus(annotationStatus);
		return R.data(annotations);
	}

	/**
	 * 根据ID查询标注详情
	 */
	@GetMapping("/{id}")
	@Operation(summary = "查询标注详情", description = "根据ID获取标注详细信息")
	public R<AlgorithmAnnotation> getAnnotationById(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("查询标注详情：ID={}", id);

		AlgorithmAnnotation annotation = vlsAlgorithmAnnotationService.getById(id);
		if (annotation == null) {
			return R.fail("标注不存在");
		}

		return R.data(annotation);
	}

	/**
	 * 创建算法标注
	 */
	@PostMapping
	@Operation(summary = "创建算法标注", description = "新增算法标注")
	public R<String> createAnnotation(@Valid @RequestBody AlgorithmAnnotation annotation) {
		log.info("创建算法标注：{}", annotation.getAnnotationName());

		boolean success = vlsAlgorithmAnnotationService.createAnnotation(annotation);
		if (success) {
			return R.data("标注创建成功");
		} else {
			return R.fail("标注创建失败，名称可能已存在");
		}
	}

	/**
	 * 更新算法标注
	 */
	@PutMapping("/{id}")
	@Operation(summary = "更新算法标注", description = "根据ID更新标注信息")
	public R<String> updateAnnotation(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody AlgorithmAnnotation annotation) {

		log.info("更新算法标注：ID={}", id);

		annotation.setId(id);
		boolean success = vlsAlgorithmAnnotationService.updateAnnotation(annotation);

		if (success) {
			return R.data("标注更新成功");
		} else {
			return R.fail("标注更新失败");
		}
	}

	/**
	 * 删除算法标注
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "删除算法标注", description = "根据ID删除标注（软删除）")
	public R<String> deleteAnnotation(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("删除算法标注：ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.deleteAnnotation(id);
		if (success) {
			return R.success("标注删除成功");
		} else {
			return R.fail("标注删除失败");
		}
	}

	/**
	 * 批量删除算法标注
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "批量删除算法标注", description = "根据ID列表批量删除标注")
	public R<String> batchDeleteAnnotations(@RequestBody List<Long> ids) {
		log.info("批量删除算法标注：IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要删除的标注");
		}

		boolean success = vlsAlgorithmAnnotationService.batchDeleteAnnotations(ids);
		if (success) {
			return R.success("标注批量删除成功");
		} else {
			return R.fail("标注批量删除失败");
		}
	}

	/**
	 * 更新标注进度
	 */
	@PutMapping("/{id}/progress")
	@Operation(summary = "更新标注进度", description = "更新标注的进度信息")
	public R<String> updateAnnotationProgress(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "已标注数量", example = "50") @RequestParam @NotNull Integer annotatedCount) {

		log.info("更新标注进度：ID={}, AnnotatedCount={}", id, annotatedCount);

		boolean success = vlsAlgorithmAnnotationService.updateAnnotationProgress(id, annotatedCount);
		if (success) {
			return R.success("标注进度更新成功");
		} else {
			return R.fail("标注进度更新失败");
		}
	}

	/**
	 * 批量更新标注状态
	 */
	@PutMapping("/batch/status")
	@Operation(summary = "批量更新标注状态", description = "批量更新标注的状态")
	public R<String> batchUpdateAnnotationStatus(
		@RequestBody List<Long> ids,
		@Parameter(description = "标注状态", example = "completed") @RequestParam @NotNull String annotationStatus) {

		log.info("批量更新标注状态：IDs={}, Status={}", ids, annotationStatus);

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要更新的标注");
		}

		boolean success = vlsAlgorithmAnnotationService.batchUpdateAnnotationStatus(ids, annotationStatus);
		if (success) {
			return R.success("标注状态批量更新成功");
		} else {
			return R.fail("标注状态批量更新失败");
		}
	}

	/**
	 * 开始标注任务
	 */
	@PostMapping("/{id}/start")
	@Operation(summary = "开始标注任务", description = "开始指定的标注任务")
	public R<String> startAnnotationTask(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("开始标注任务：ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.startAnnotationTask(id);
		if (success) {
			return R.success("标注任务开始成功");
		} else {
			return R.fail("标注任务开始失败");
		}
	}

	/**
	 * 完成标注任务
	 */
	@PostMapping("/{id}/complete")
	@Operation(summary = "完成标注任务", description = "完成指定的标注任务")
	public R<String> completeAnnotationTask(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("完成标注任务：ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.completeAnnotationTask(id);
		if (success) {
			return R.success("标注任务完成成功");
		} else {
			return R.fail("标注任务完成失败");
		}
	}

	/**
	 * 重置标注任务
	 */
	@PostMapping("/{id}/reset")
	@Operation(summary = "重置标注任务", description = "重置指定的标注任务")
	public R<String> resetAnnotationTask(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("重置标注任务：ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.resetAnnotationTask(id);
		if (success) {
			return R.success("标注任务重置成功");
		} else {
			return R.fail("标注任务重置失败");
		}
	}

	/**
	 * 导出标注数据
	 */
	@PostMapping("/{id}/export")
	@Operation(summary = "导出标注数据", description = "导出指定标注的数据")
	public void exportAnnotationData(@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id, HttpServletResponse response) {
		log.info("Download dataset zip request, id={}", id);
		vlsAlgorithmAnnotationService.downloadAnnotationDataset(id, response);
	}

	/**
	 * 导入标注数据
	 */
	@PostMapping("/{id}/import")
	@Operation(summary = "导入标注数据", description = "导入标注数据")
	public R<Map<String, Object>> importAnnotationData(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "数据路径") @RequestParam @NotNull String dataPath) {

		log.info("导入标注数据：ID={}, DataPath={}", id, dataPath);

		Map<String, Object> result = vlsAlgorithmAnnotationService.importAnnotationData(id, dataPath);
		if (result != null) {
			return R.data(result);
		} else {
			return R.fail("标注数据导入失败");
		}
	}

	/**
	 * 验证标注数据
	 */
	/**
	 * Import annotation dataset zip.
	 */
	@PostMapping(value = "/{id}/import-zip", consumes = "multipart/form-data")
	@Operation(summary = "Import annotation dataset zip", description = "Import annotation dataset zip")
	public R<Map<String, Object>> importAnnotationZip(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "Zip dataset file") @RequestPart("file") @NotNull MultipartFile zipFile) {

		String originalFileName = zipFile == null ? null : zipFile.getOriginalFilename();
		log.info("Import annotation dataset zip request: id={}, fileName={}", id, originalFileName);

		try {
			Map<String, Object> result = vlsAlgorithmAnnotationService.importAnnotationDatasetZip(id, zipFile);
			if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
				String message = result == null ? "Import failed" : String.valueOf(result.get("message"));
				return R.fail(message);
			}
			return R.data(result);
		} catch (Exception importException) {
			log.error("Import annotation dataset zip failed: id={}, fileName={}, error={}",
				id, originalFileName, importException.getMessage(), importException);
			return R.fail("Import failed: " + importException.getMessage());
		}
	}

	@PostMapping("/{id}/validate")
	@Operation(summary = "验证标注数据", description = "验证指定标注的数据质量")
	public R<Map<String, Object>> validateAnnotationData(
		@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("验证标注数据：ID={}", id);

		Map<String, Object> result = vlsAlgorithmAnnotationService.validateAnnotationData(id);
		if (result != null) {
			return R.data(result);
		} else {
			return R.fail("标注数据验证失败");
		}
	}

	/**
	 * 获取标注类型统计
	 */
	@GetMapping("/statistics/type")
	@Operation(summary = "获取标注类型统计", description = "获取各类型的标注数量统计")
	public R<List<Map<String, Object>>> getAnnotationTypeStatistics() {
		log.info("获取标注类型统计");

		List<Map<String, Object>> statistics = vlsAlgorithmAnnotationService.getAnnotationTypeStatistics();
		return R.data(statistics);
	}

	/**
	 * 获取标注状态统计
	 */
	@GetMapping("/statistics/status")
	@Operation(summary = "获取标注状态统计", description = "获取各状态的标注数量统计")
	public R<List<Map<String, Object>>> getAnnotationStatusStatistics() {
		log.info("获取标注状态统计");

		List<Map<String, Object>> statistics = vlsAlgorithmAnnotationService.getAnnotationStatusStatistics();
		return R.data(statistics);
	}

	/**
	 * 获取标注进度统计
	 */
	@GetMapping("/statistics/progress")
	@Operation(summary = "获取标注进度统计", description = "获取各进度区间的标注数量统计")
	public R<List<Map<String, Object>>> getProgressStatistics() {
		log.info("获取标注进度统计");

		List<Map<String, Object>> statistics = vlsAlgorithmAnnotationService.getProgressStatistics();
		return R.data(statistics);
	}

	/**
	 * 获取标注工作量统计
	 */
	@GetMapping("/statistics/workload")
	@Operation(summary = "获取标注工作量统计", description = "获取标注工作量的总体统计")
	public R<Map<String, Object>> getWorkloadStatistics() {
		log.info("获取标注工作量统计");

		Map<String, Object> statistics = vlsAlgorithmAnnotationService.getWorkloadStatistics();
		return R.data(statistics);
	}

	/**
	 * 保存标注数据到数据集文件
	 */
	@PostMapping("/{id}/save-dataset")
	@Operation(summary = "保存标注数据到数据集", description = "将标注数据保存到数据集文件并更新数据库路径")
	public R<String> saveAnnotationToDataset(@Parameter(description = "标注ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("保存标注数据到数据集：ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.saveAnnotationToDataset(id);
		if (success) {
			return R.success("标注数据保存到数据集成功");
		} else {
			return R.fail("标注数据保存到数据集失败");
		}
	}

}
