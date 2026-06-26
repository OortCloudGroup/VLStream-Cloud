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
import org.springblade.vlstream.excel.VlsAnnotationImageExcel;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.vo.AnnotationImageVO;
import org.springblade.vlstream.service.IVlsAnnotationImageService;
import org.springblade.vlstream.wrapper.VlsAnnotationImageWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotation Image Info Table Controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAnnotationImage")
@Tag(name = "Annotation Image Info Table", description = "Annotation Image Info Table Interface")
public class VlsAnnotationImageController extends BladeController {

	private final IVlsAnnotationImageService vlsAnnotationImageService;

	/**
	 * Annotation Image Info Table Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description  = "Pass in vlsAnnotationImage")
	public R<AnnotationImageVO> detail(AnnotationImage vlsAnnotationImage) {
		AnnotationImage detail = vlsAnnotationImageService.getOne(Condition.getQueryWrapper(vlsAnnotationImage));
		return R.data(VlsAnnotationImageWrapper.build().entityVO(detail));
	}

	/**
	 * Annotation Image Info Table Pagination
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description  = "Pass in vlsAnnotationImage")
	public R<IPage<AnnotationImageVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationImage, Query query) {
		IPage<AnnotationImage> pages = vlsAnnotationImageService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAnnotationImage, AnnotationImage.class));
		return R.data(VlsAnnotationImageWrapper.build().pageVO(pages));
	}


	/**
	 * Annotation Image Info Table Custom Pagination
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description  = "Pass in vlsAnnotationImage")
	public R<IPage<AnnotationImageVO>> page(AnnotationImageVO vlsAnnotationImage, Query query) {
		IPage<AnnotationImageVO> pages = vlsAnnotationImageService.selectVlsAnnotationImagePage(Condition.getPage(query), vlsAnnotationImage);
		return R.data(pages);
	}

	/**
	 * Annotation Image Info Table Creation
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description  = "Pass in vlsAnnotationImage")
	public R save(@Valid @RequestBody AnnotationImage vlsAnnotationImage) {
		return R.status(vlsAnnotationImageService.save(vlsAnnotationImage));
	}

	/**
	 * Annotation Image Info Table Modification
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description  = "Pass in vlsAnnotationImage")
	public R update(@Valid @RequestBody AnnotationImage vlsAnnotationImage) {
		return R.status(vlsAnnotationImageService.updateById(vlsAnnotationImage));
	}

	/**
	 * Annotation Image Info Table Creation or Modification
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description  = "Pass in vlsAnnotationImage")
	public R submit(@Valid @RequestBody AnnotationImage vlsAnnotationImage) {
		return R.status(vlsAnnotationImageService.saveOrUpdate(vlsAnnotationImage));
	}

	/**
	 * Annotation Image Info Table Deletion
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description  = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAnnotationImageService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAnnotationImage")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description  = "Pass in vlsAnnotationImage")
	public void exportVlsAnnotationImage(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationImage, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AnnotationImage> queryWrapper = Condition.getQueryWrapper(vlsAnnotationImage, AnnotationImage.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAnnotationImageEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAnnotationImageEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAnnotationImageExcel> list = vlsAnnotationImageService.exportVlsAnnotationImage(queryWrapper);
		ExcelUtil.export(response, "Annotation Image Info Table Data" + DateUtil.today(), "Annotation Image Info Table Data Table", list, VlsAnnotationImageExcel.class);
	}

	/**
	 * Upload annotated image
	 */
	@PostMapping("/upload")
	public ResponseEntity<?> uploadImages(
		@RequestPart("files") MultipartFile[] files,
		@RequestParam("annotationId") Long annotationId) {
		try {
			List<AnnotationImage> images = vlsAnnotationImageService.uploadImages(files, annotationId);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Image uploaded successfully");
			response.put("data", images);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Image upload failed:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Get all images of dataset
	 */
	@GetMapping("/dataset/{annotationId}")
	public ResponseEntity<?> getImagesByDataset(@PathVariable Long annotationId) {
		try {
			List<AnnotationImage> images = vlsAnnotationImageService.getImagesByDataset(annotationId);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", images);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Failed to get image list:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Get image details
	 */
	@GetMapping("/{id}")
	public ResponseEntity<?> getImageById(@PathVariable Long id) {
		try {
			AnnotationImage image = vlsAnnotationImageService.getImageById(id);
			if (image != null) {
				Map<String, Object> response = new HashMap<>();
				response.put("success", true);
				response.put("data", image);
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Failed to get image details:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Update image annotation information
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateImage(@PathVariable Long id, @RequestBody AnnotationImage image) {
		try {
			image.setId(id);
			AnnotationImage updatedImage = vlsAnnotationImageService.updateImage(image);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Image info updated successfully");
			response.put("data", updatedImage);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Failed to update image information:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Delete image
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteImage(@PathVariable Long id) {
		try {
			vlsAnnotationImageService.deleteImage(id);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Image deleted successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Failed to delete image:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Batch delete images
	 */
	@DeleteMapping("/batch")
	public ResponseEntity<?> batchDeleteImages(@RequestBody List<Long> ids) {
		try {
			vlsAnnotationImageService.batchDeleteImages(ids);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Batch delete succeeded");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Batch delete failed:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Get dataset statistics
	 */
	@GetMapping("/dataset/{datasetId}/stats")
	public ResponseEntity<?> getDatasetStats(@PathVariable Long datasetId) {
		try {
			Map<String, Object> stats = vlsAnnotationImageService.getDatasetStats(datasetId);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", stats);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Failed to get statistical info:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Bulk save image info to annotation_image table
	 *
	 * @param annotationImages image information list
	 * @return save result
	 */
	@PostMapping("/images/batch")
	public ResponseEntity<?> batchSaveImages(@RequestBody List<AnnotationImage> annotationImages) {
		try {
			log.info("Bulk save image info, count: {}", annotationImages.size());

			boolean success = vlsAnnotationImageService.batchSaveImages(annotationImages);
			if (success) {
				Map<String, Object> response = new HashMap<>();
				response.put("success", true);
				response.put("message", "Bulk saved image info successfully");
				response.put("data", annotationImages.size());
				return ResponseEntity.ok(response);
			} else {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("message", "Failed to bulk save image info");
				return ResponseEntity.badRequest().body(errorResponse);
			}
		} catch (Exception e) {
			log.error("Failed to bulk save image info", e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Failed to bulk save image info:" + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

}
