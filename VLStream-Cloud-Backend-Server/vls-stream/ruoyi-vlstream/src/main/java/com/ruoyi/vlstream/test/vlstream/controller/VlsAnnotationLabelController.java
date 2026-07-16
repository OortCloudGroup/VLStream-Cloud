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
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationLabelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationLabelVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationLabelService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAnnotationLabelWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 标注标签实体类 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAnnotationLabel")
@Tag(name = "标注标签实体类", description = "标注标签实体类接口")
public class VlsAnnotationLabelController extends BladeController {

	private final IVlsAnnotationLabelService vlsAnnotationLabelService;

	/**
	 * 标注标签实体类 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAnnotationLabel")
	public R<AnnotationLabelVO> detail(AnnotationLabel vlsAnnotationLabel) {
		AnnotationLabel detail = vlsAnnotationLabelService.getOne(Condition.getQueryWrapper(vlsAnnotationLabel));
		return R.data(VlsAnnotationLabelWrapper.build().entityVO(detail));
	}

	/**
	 * 标注标签实体类 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAnnotationLabel")
	public R<IPage<AnnotationLabelVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationLabel, Query query) {
		IPage<AnnotationLabel> pages = vlsAnnotationLabelService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAnnotationLabel, AnnotationLabel.class));
		return R.data(VlsAnnotationLabelWrapper.build().pageVO(pages));
	}


	/**
	 * 标注标签实体类 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAnnotationLabel")
	public R<IPage<AnnotationLabelVO>> page(AnnotationLabelVO vlsAnnotationLabel, Query query) {
		IPage<AnnotationLabelVO> pages = vlsAnnotationLabelService.selectVlsAnnotationLabelPage(Condition.getPage(query), vlsAnnotationLabel);
		return R.data(pages);
	}

	/**
	 * 标注标签实体类 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAnnotationLabel")
	public R save(@Valid @RequestBody AnnotationLabel vlsAnnotationLabel) {
		return R.status(vlsAnnotationLabelService.save(vlsAnnotationLabel));
	}

	/**
	 * 标注标签实体类 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAnnotationLabel")
	public R update(@Valid @RequestBody AnnotationLabel vlsAnnotationLabel) {
		return R.status(vlsAnnotationLabelService.updateById(vlsAnnotationLabel));
	}

	/**
	 * 标注标签实体类 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAnnotationLabel")
	public R submit(@Valid @RequestBody AnnotationLabel vlsAnnotationLabel) {
		return R.status(vlsAnnotationLabelService.saveOrUpdate(vlsAnnotationLabel));
	}

	/**
	 * 标注标签实体类 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAnnotationLabelService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAnnotationLabel")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAnnotationLabel")
	public void exportVlsAnnotationLabel(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationLabel, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AnnotationLabel> queryWrapper = Condition.getQueryWrapper(vlsAnnotationLabel, AnnotationLabel.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAnnotationLabelEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAnnotationLabelEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAnnotationLabelExcel> list = vlsAnnotationLabelService.exportVlsAnnotationLabel(queryWrapper);
		ExcelUtil.export(response, "标注标签实体类数据" + DateUtil.today(), "标注标签实体类数据表", list, VlsAnnotationLabelExcel.class);
	}

	/**
	 * 获取标注项目的标签列表
	 *
	 * @param annotationId 标注项目ID
	 * @param keyword      搜索关键词（可选）
	 * @return 标签列表
	 */
	@GetMapping("/{annotationId}/labels")
	public R<List<AnnotationLabel>> getLabels(@PathVariable Long annotationId,
											  @RequestParam(required = false) String keyword) {
		try {
			List<AnnotationLabel> labels;
			if (keyword != null && !keyword.trim().isEmpty()) {
				labels = vlsAnnotationLabelService.searchLabels(annotationId, keyword.trim());
			} else {
				labels = vlsAnnotationLabelService.getByAnnotationIdWithUsageCount(annotationId);
			}
			return R.data(labels);
		} catch (Exception e) {
			log.error("获取标签列表失败", e);
			return R.fail("获取标签列表失败: " + e.getMessage());
		}
	}

	/**
	 * 创建标签
	 *
	 * @param annotationId 标注项目ID
	 * @param requestBody  请求体
	 * @return 创建的标签
	 */
	@PostMapping("/{annotationId}/labels")
	public R<AnnotationLabel> createLabel(@PathVariable Long annotationId,
										  @RequestBody Map<String, Object> requestBody) {
		try {
			String name = (String) requestBody.get("name");
			String color = (String) requestBody.get("color");
			String description = (String) requestBody.get("description");

			if (name == null || name.trim().isEmpty()) {
				return R.fail("标签名称不能为空");
			}
			if (color == null || color.trim().isEmpty()) {
				return R.fail("标签颜色不能为空");
			}

			AnnotationLabel label = vlsAnnotationLabelService.createLabel(annotationId, name.trim(), color.trim(), description);
			return R.data(label);
		} catch (Exception e) {
			log.error("创建标签失败", e);
			return R.fail("创建标签失败: " + e.getMessage());
		}
	}

	/**
	 * 更新标签
	 *
	 * @param labelId     标签ID
	 * @param requestBody 请求体
	 * @return 更新后的标签
	 */
	@PutMapping("/labels/{labelId}")
	public R<AnnotationLabel> updateLabel(@PathVariable Long labelId,
										  @RequestBody Map<String, Object> requestBody) {
		try {
			String name = (String) requestBody.get("name");
			String color = (String) requestBody.get("color");
			String description = (String) requestBody.get("description");

			if (name == null || name.trim().isEmpty()) {
				return R.fail("标签名称不能为空");
			}
			if (color == null || color.trim().isEmpty()) {
				return R.fail("标签颜色不能为空");
			}

			AnnotationLabel label = vlsAnnotationLabelService.updateLabel(labelId, name.trim(), color.trim(), description);
			return R.data(label);
		} catch (Exception e) {
			log.error("更新标签失败", e);
			return R.fail("更新标签失败: " + e.getMessage());
		}
	}

	/**
	 * 删除标签
	 *
	 * @param labelId 标签ID
	 * @return 删除结果
	 */
	@DeleteMapping("/labels/{labelId}")
	public R<Boolean> deleteLabel(@PathVariable Long labelId) {
		try {
			boolean success = vlsAnnotationLabelService.deleteLabel(labelId);
			return R.data(success);
		} catch (Exception e) {
			log.error("删除标签失败", e);
			return R.fail("删除标签失败: " + e.getMessage());
		}
	}

	/**
	 * 批量更新标签排序
	 *
	 * @param annotationId 标注项目ID
	 * @param requestBody  请求体（包含labelIds数组）
	 * @return 更新结果
	 */
	@PutMapping("/{annotationId}/labels/sort")
	public R<Boolean> updateLabelSort(@PathVariable Long annotationId,
									  @RequestBody Map<String, Object> requestBody) {
		try {
			@SuppressWarnings("unchecked")
			List<Long> labelIds = (List<Long>) requestBody.get("labelIds");

			if (labelIds == null || labelIds.isEmpty()) {
				return R.fail("标签ID列表不能为空");
			}

			boolean success = vlsAnnotationLabelService.updateSortOrder(annotationId, labelIds);
			return R.data(success);
		} catch (Exception e) {
			log.error("更新标签排序失败", e);
			return R.fail("更新标签排序失败: " + e.getMessage());
		}
	}

}
