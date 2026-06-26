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
import org.springblade.vlstream.excel.VlsAlgorithmAnnotationExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.vo.AlgorithmAnnotationVO;
import org.springblade.vlstream.service.IVlsAlgorithmAnnotationService;
import org.springblade.vlstream.wrapper.VlsAlgorithmAnnotationWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Algorithm annotation data table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmAnnotation")
@Tag(name = "Algorithm annotation data table", description = "Algorithm annotation data table interface")
public class VlsAlgorithmAnnotationController extends BladeController {

	private final IVlsAlgorithmAnnotationService vlsAlgorithmAnnotationService;

	/**
	 * Algorithm annotation data table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsAlgorithmAnnotation")
	public R<AlgorithmAnnotationVO> detail(AlgorithmAnnotation vlsAlgorithmAnnotation) {
		AlgorithmAnnotation detail = vlsAlgorithmAnnotationService.getOne(Condition.getQueryWrapper(vlsAlgorithmAnnotation));
		return R.data(VlsAlgorithmAnnotationWrapper.build().entityVO(detail));
	}

	/**
	 * Algorithm annotation data table paging
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmAnnotation")
	public R<IPage<AlgorithmAnnotationVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmAnnotation, Query query) {
		IPage<AlgorithmAnnotation> pages = vlsAlgorithmAnnotationService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmAnnotation, AlgorithmAnnotation.class));
		return R.data(VlsAlgorithmAnnotationWrapper.build().pageVO(pages));
	}


	/**
	 * Algorithm annotation data table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsAlgorithmAnnotation")
	public R<IPage<AlgorithmAnnotationVO>> page(AlgorithmAnnotationVO vlsAlgorithmAnnotation, Query query) {
		IPage<AlgorithmAnnotationVO> pages = vlsAlgorithmAnnotationService.selectVlsAlgorithmAnnotationPage(Condition.getPage(query), vlsAlgorithmAnnotation);
		return R.data(pages);
	}

	/**
	 * Add algorithm annotation data table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsAlgorithmAnnotation")
	public R save(@Valid @RequestBody AlgorithmAnnotation vlsAlgorithmAnnotation) {
		return R.status(vlsAlgorithmAnnotationService.save(vlsAlgorithmAnnotation));
	}

	/**
	 * Modify algorithm annotation data table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsAlgorithmAnnotation")
	public R update(@Valid @RequestBody AlgorithmAnnotation vlsAlgorithmAnnotation) {
		return R.status(vlsAlgorithmAnnotationService.updateById(vlsAlgorithmAnnotation));
	}

	/**
	 * Add or modify algorithm annotation data table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsAlgorithmAnnotation")
	public R submit(@Valid @RequestBody AlgorithmAnnotation vlsAlgorithmAnnotation) {
		return R.status(vlsAlgorithmAnnotationService.saveOrUpdate(vlsAlgorithmAnnotation));
	}

	/**
	 * Delete algorithm annotation data table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmAnnotationService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAlgorithmAnnotation")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsAlgorithmAnnotation")
	public void exportVlsAlgorithmAnnotation(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmAnnotation, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmAnnotation> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmAnnotation, AlgorithmAnnotation.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmAnnotationEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmAnnotationEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmAnnotationExcel> list = vlsAlgorithmAnnotationService.exportVlsAlgorithmAnnotation(queryWrapper);
		ExcelUtil.export(response, "Algorithm annotation data table data" + DateUtil.today(), "Algorithm annotation data table database table", list, VlsAlgorithmAnnotationExcel.class);
	}

	/**
	 * Query annotation list by annotation type
	 */
	@GetMapping("/type/{annotationType}")
	@Operation(summary = "Query annotation list by annotation type", description = "Get all annotations of specified type")
	public R<List<AlgorithmAnnotation>> getAnnotationsByType(
		@Parameter(description = "Annotation Type", example = "object_detection") @PathVariable String annotationType) {

		log.info("Query annotation list by annotation type: {}", annotationType);

		List<AlgorithmAnnotation> annotations = vlsAlgorithmAnnotationService.getByAnnotationType(annotationType);
		return R.data(annotations);
	}

	/**
	 * Query annotation list by annotation status
	 */
	@GetMapping("/status/{annotationStatus}")
	@Operation(summary = "Query annotation list by annotation status", description = "Get all annotations of specified status")
	public R<List<AlgorithmAnnotation>> getAnnotationsByStatus(
		@Parameter(description = "Annotation Status", example = "partial") @PathVariable String annotationStatus) {

		log.info("Query annotation list by annotation status: {}", annotationStatus);

		List<AlgorithmAnnotation> annotations = vlsAlgorithmAnnotationService.getByAnnotationStatus(annotationStatus);
		return R.data(annotations);
	}

	/**
	 * Query annotation details by ID
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Query Annotation Details", description = "Get annotation detailed information by ID")
	public R<AlgorithmAnnotation> getAnnotationById(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Query annotation details: ID={}", id);

		AlgorithmAnnotation annotation = vlsAlgorithmAnnotationService.getById(id);
		if (annotation == null) {
			return R.fail("Annotation does not exist");
		}

		return R.data(annotation);
	}

	/**
	 * Create algorithm annotation
	 */
	@PostMapping
	@Operation(summary = "Create algorithm annotation", description = "Add algorithm annotation")
	public R<String> createAnnotation(@Valid @RequestBody AlgorithmAnnotation annotation) {
		log.info("Create algorithm annotation: {}", annotation.getAnnotationName());

		boolean success = vlsAlgorithmAnnotationService.createAnnotation(annotation);
		if (success) {
			return R.data("Annotation created successfully");
		} else {
			return R.fail("Failed to create annotation, name may already exist");
		}
	}

	/**
	 * Update algorithm annotation
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Update algorithm annotation", description = "Update annotation information by ID")
	public R<String> updateAnnotation(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
		@Valid @RequestBody AlgorithmAnnotation annotation) {

		log.info("Update algorithm annotation: ID={}", id);

		annotation.setId(id);
		boolean success = vlsAlgorithmAnnotationService.updateAnnotation(annotation);

		if (success) {
			return R.data("Annotation updated successfully");
		} else {
			return R.fail("Failed to update annotation");
		}
	}

	/**
	 * Delete algorithm annotation
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete algorithm annotation", description = "Delete annotation by ID (soft delete)")
	public R<String> deleteAnnotation(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Delete algorithm annotation: ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.deleteAnnotation(id);
		if (success) {
			return R.success("Annotation deleted successfully");
		} else {
			return R.fail("Failed to delete annotation");
		}
	}

	/**
	 * Batch delete algorithm annotations
	 */
	@DeleteMapping("/batch")
	@Operation(summary = "Batch delete algorithm annotations", description = "Batch delete annotations by ID list")
	public R<String> batchDeleteAnnotations(@RequestBody List<Long> ids) {
		log.info("Batch delete algorithm annotations: IDs={}", ids);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select annotations to delete");
		}

		boolean success = vlsAlgorithmAnnotationService.batchDeleteAnnotations(ids);
		if (success) {
			return R.success("Annotations batch deleted successfully");
		} else {
			return R.fail("Failed to batch delete annotations");
		}
	}

	/**
	 * Update annotation progress
	 */
	@PutMapping("/{id}/progress")
	@Operation(summary = "Update annotation progress", description = "Update annotation progress information")
	public R<String> updateAnnotationProgress(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "Annotated quantity", example = "50") @RequestParam @NotNull Integer annotatedCount) {

		log.info("Update annotation progress: ID={}, AnnotatedCount={}", id, annotatedCount);

		boolean success = vlsAlgorithmAnnotationService.updateAnnotationProgress(id, annotatedCount);
		if (success) {
			return R.success("Annotation progress updated successfully");
		} else {
			return R.fail("Failed to update annotation progress");
		}
	}

	/**
	 * Batch update annotation status
	 */
	@PutMapping("/batch/status")
	@Operation(summary = "Batch update annotation status", description = "Batch update the status of annotations")
	public R<String> batchUpdateAnnotationStatus(
		@RequestBody List<Long> ids,
		@Parameter(description = "Annotation Status", example = "completed") @RequestParam @NotNull String annotationStatus) {

		log.info("Batch update annotation status: IDs={}, Status={}", ids, annotationStatus);

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select annotations to update");
		}

		boolean success = vlsAlgorithmAnnotationService.batchUpdateAnnotationStatus(ids, annotationStatus);
		if (success) {
			return R.success("Annotation status batch updated successfully");
		} else {
			return R.fail("Failed to batch update annotation status");
		}
	}

	/**
	 * Start annotation task
	 */
	@PostMapping("/{id}/start")
	@Operation(summary = "Start annotation task", description = "Start designated annotation task")
	public R<String> startAnnotationTask(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Start annotation task: ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.startAnnotationTask(id);
		if (success) {
			return R.success("Annotation task started successfully");
		} else {
			return R.fail("Failed to start annotation task");
		}
	}

	/**
	 * Complete annotation task
	 */
	@PostMapping("/{id}/complete")
	@Operation(summary = "Complete annotation task", description = "Complete designated annotation task")
	public R<String> completeAnnotationTask(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Complete annotation task: ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.completeAnnotationTask(id);
		if (success) {
			return R.success("Annotation task completed successfully");
		} else {
			return R.fail("Failed to complete annotation task");
		}
	}

	/**
	 * Reset labeling task
	 */
	@PostMapping("/{id}/reset")
	@Operation(summary = "Reset labeling task", description = "Reset specified labeling task")
	public R<String> resetAnnotationTask(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Reset labeling task: ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.resetAnnotationTask(id);
		if (success) {
			return R.success("Annotation task reset successfully");
		} else {
			return R.fail("Failed to reset annotation task");
		}
	}

	/**
	 * Export annotation data
	 */
	@PostMapping("/{id}/export")
	@Operation(summary = "Export annotation data", description = "Export data of designated annotation")
	public void exportAnnotationData(@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id, HttpServletResponse response) {
		log.info("Download dataset zip request, id={}", id);
		vlsAlgorithmAnnotationService.downloadAnnotationDataset(id, response);
	}

	/**
	 * Import annotation data
	 */
	@PostMapping("/{id}/import")
	@Operation(summary = "Import annotation data", description = "Import annotation data")
	public R<Map<String, Object>> importAnnotationData(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id,
		@Parameter(description = "Data path") @RequestParam @NotNull String dataPath) {

		log.info("Import annotation data: ID={}, DataPath={}", id, dataPath);

		Map<String, Object> result = vlsAlgorithmAnnotationService.importAnnotationData(id, dataPath);
		if (result != null) {
			return R.data(result);
		} else {
			return R.fail("Failed to import annotation data");
		}
	}

	/**
	 * Verify labeling data
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
	@Operation(summary = "Verify labeling data", description = "Verify data quality of specified labeling")
	public R<Map<String, Object>> validateAnnotationData(
		@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Verify labeling data: ID={}", id);

		Map<String, Object> result = vlsAlgorithmAnnotationService.validateAnnotationData(id);
		if (result != null) {
			return R.data(result);
		} else {
			return R.fail("Failed to validate annotation data");
		}
	}

	/**
	 * Get annotation type statistics
	 */
	@GetMapping("/statistics/type")
	@Operation(summary = "Get annotation type statistics", description = "Get annotation count statistics for each type")
	public R<List<Map<String, Object>>> getAnnotationTypeStatistics() {
		log.info("Get annotation type statistics");

		List<Map<String, Object>> statistics = vlsAlgorithmAnnotationService.getAnnotationTypeStatistics();
		return R.data(statistics);
	}

	/**
	 * Get annotation status statistics
	 */
	@GetMapping("/statistics/status")
	@Operation(summary = "Get annotation status statistics", description = "Get annotation count statistics for each status")
	public R<List<Map<String, Object>>> getAnnotationStatusStatistics() {
		log.info("Get annotation status statistics");

		List<Map<String, Object>> statistics = vlsAlgorithmAnnotationService.getAnnotationStatusStatistics();
		return R.data(statistics);
	}

	/**
	 * Get annotation progress statistics
	 */
	@GetMapping("/statistics/progress")
	@Operation(summary = "Get annotation progress statistics", description = "Get annotation count statistics for each progress interval")
	public R<List<Map<String, Object>>> getProgressStatistics() {
		log.info("Get annotation progress statistics");

		List<Map<String, Object>> statistics = vlsAlgorithmAnnotationService.getProgressStatistics();
		return R.data(statistics);
	}

	/**
	 * Get annotation workload statistics
	 */
	@GetMapping("/statistics/workload")
	@Operation(summary = "Get annotation workload statistics", description = "Get overall statistics of annotation workload")
	public R<Map<String, Object>> getWorkloadStatistics() {
		log.info("Get annotation workload statistics");

		Map<String, Object> statistics = vlsAlgorithmAnnotationService.getWorkloadStatistics();
		return R.data(statistics);
	}

	/**
	 * Save annotation data to dataset file
	 */
	@PostMapping("/{id}/save-dataset")
	@Operation(summary = "Save annotation data to dataset", description = "Save annotation data to dataset file and update database path")
	public R<String> saveAnnotationToDataset(@Parameter(description = "Annotation ID", example = "1") @PathVariable @NotNull Long id) {

		log.info("Save annotation data to dataset: ID={}", id);

		boolean success = vlsAlgorithmAnnotationService.saveAnnotationToDataset(id);
		if (success) {
			return R.success("Annotation data saved to dataset successfully");
		} else {
			return R.fail("Failed to save annotation data to dataset");
		}
	}

}
