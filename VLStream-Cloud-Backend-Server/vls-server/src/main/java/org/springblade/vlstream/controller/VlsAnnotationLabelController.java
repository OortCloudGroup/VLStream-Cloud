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
import org.springblade.vlstream.excel.VlsAnnotationLabelExcel;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;
import org.springblade.vlstream.pojo.vo.AnnotationLabelVO;
import org.springblade.vlstream.service.IVlsAnnotationLabelService;
import org.springblade.vlstream.wrapper.VlsAnnotationLabelWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Annotation Label Entity Class Controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAnnotationLabel")
@Tag(name = "Annotation Label Entity Class", description = "Annotation Label Entity Class Interface")
public class VlsAnnotationLabelController extends BladeController {

	private final IVlsAnnotationLabelService vlsAnnotationLabelService;

	/**
	 * Annotation Label Entity Class Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsAnnotationLabel")
	public R<AnnotationLabelVO> detail(AnnotationLabel vlsAnnotationLabel) {
		AnnotationLabel detail = vlsAnnotationLabelService.getOne(Condition.getQueryWrapper(vlsAnnotationLabel));
		return R.data(VlsAnnotationLabelWrapper.build().entityVO(detail));
	}

	/**
	 * Annotation Label Entity Class Pagination
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsAnnotationLabel")
	public R<IPage<AnnotationLabelVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationLabel, Query query) {
		IPage<AnnotationLabel> pages = vlsAnnotationLabelService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAnnotationLabel, AnnotationLabel.class));
		return R.data(VlsAnnotationLabelWrapper.build().pageVO(pages));
	}


	/**
	 * Annotation Label Entity Class Custom Pagination
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsAnnotationLabel")
	public R<IPage<AnnotationLabelVO>> page(AnnotationLabelVO vlsAnnotationLabel, Query query) {
		IPage<AnnotationLabelVO> pages = vlsAnnotationLabelService.selectVlsAnnotationLabelPage(Condition.getPage(query), vlsAnnotationLabel);
		return R.data(pages);
	}

	/**
	 * Annotation Label Entity Class Creation
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsAnnotationLabel")
	public R save(@Valid @RequestBody AnnotationLabel vlsAnnotationLabel) {
		return R.status(vlsAnnotationLabelService.save(vlsAnnotationLabel));
	}

	/**
	 * Annotation Label Entity Class Modification
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsAnnotationLabel")
	public R update(@Valid @RequestBody AnnotationLabel vlsAnnotationLabel) {
		return R.status(vlsAnnotationLabelService.updateById(vlsAnnotationLabel));
	}

	/**
	 * Annotation Label Entity Class Creation or Modification
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsAnnotationLabel")
	public R submit(@Valid @RequestBody AnnotationLabel vlsAnnotationLabel) {
		return R.status(vlsAnnotationLabelService.saveOrUpdate(vlsAnnotationLabel));
	}

	/**
	 * Annotation Label Entity Class Deletion
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAnnotationLabelService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAnnotationLabel")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsAnnotationLabel")
	public void exportVlsAnnotationLabel(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationLabel, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AnnotationLabel> queryWrapper = Condition.getQueryWrapper(vlsAnnotationLabel, AnnotationLabel.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAnnotationLabelEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAnnotationLabelEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAnnotationLabelExcel> list = vlsAnnotationLabelService.exportVlsAnnotationLabel(queryWrapper);
		ExcelUtil.export(response, "Annotation Label Entity Class Data" + DateUtil.today(), "Annotation Label Entity Class Data Table", list, VlsAnnotationLabelExcel.class);
	}

	/**
	 * Get label list of annotation project
	 *
	 * @param annotationId annotation project ID
	 * @param keyword      search keyword (optional)
	 * @return label list
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
			log.error("Failed to get label list", e);
			return R.fail("Failed to get label list:" + e.getMessage());
		}
	}

	/**
	 * Create tag
	 *
	 * @param annotationId annotation project ID
	 * @param requestBody  request body
	 * @return created label
	 */
	@PostMapping("/{annotationId}/labels")
	public R<AnnotationLabel> createLabel(@PathVariable Long annotationId,
										  @RequestBody Map<String, Object> requestBody) {
		try {
			String name = (String) requestBody.get("name");
			String color = (String) requestBody.get("color");
			String description = (String) requestBody.get("description");

			if (name == null || name.trim().isEmpty()) {
				return R.fail("Label name cannot be empty");
			}
			if (color == null || color.trim().isEmpty()) {
				return R.fail("Label color cannot be empty");
			}

			AnnotationLabel label = vlsAnnotationLabelService.createLabel(annotationId, name.trim(), color.trim(), description);
			return R.data(label);
		} catch (Exception e) {
			log.error("Failed to create tag", e);
			return R.fail("Failed to create tag:" + e.getMessage());
		}
	}

	/**
	 * Update label
	 *
	 * @param labelId     label ID
	 * @param requestBody request body
	 * @return updated label
	 */
	@PutMapping("/labels/{labelId}")
	public R<AnnotationLabel> updateLabel(@PathVariable Long labelId,
										  @RequestBody Map<String, Object> requestBody) {
		try {
			String name = (String) requestBody.get("name");
			String color = (String) requestBody.get("color");
			String description = (String) requestBody.get("description");

			if (name == null || name.trim().isEmpty()) {
				return R.fail("Label name cannot be empty");
			}
			if (color == null || color.trim().isEmpty()) {
				return R.fail("Label color cannot be empty");
			}

			AnnotationLabel label = vlsAnnotationLabelService.updateLabel(labelId, name.trim(), color.trim(), description);
			return R.data(label);
		} catch (Exception e) {
			log.error("Failed to update label", e);
			return R.fail("Failed to update label:" + e.getMessage());
		}
	}

	/**
	 * Delete tag
	 *
	 * @param labelId label ID
	 * @return deletion result
	 */
	@DeleteMapping("/labels/{labelId}")
	public R<Boolean> deleteLabel(@PathVariable Long labelId) {
		try {
			boolean success = vlsAnnotationLabelService.deleteLabel(labelId);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to delete tag", e);
			return R.fail("Failed to delete tag:" + e.getMessage());
		}
	}

	/**
	 * Batch update label sorting
	 *
	 * @param annotationId annotation project ID
	 * @param requestBody  request body (including labelIds array)
	 * @return update result
	 */
	@PutMapping("/{annotationId}/labels/sort")
	public R<Boolean> updateLabelSort(@PathVariable Long annotationId,
									  @RequestBody Map<String, Object> requestBody) {
		try {
			@SuppressWarnings("unchecked")
			List<Long> labelIds = (List<Long>) requestBody.get("labelIds");

			if (labelIds == null || labelIds.isEmpty()) {
				return R.fail("Label ID list cannot be empty");
			}

			boolean success = vlsAnnotationLabelService.updateSortOrder(annotationId, labelIds);
			return R.data(success);
		} catch (Exception e) {
			log.error("Failed to update label sorting", e);
			return R.fail("Failed to update label sorting:" + e.getMessage());
		}
	}

}
