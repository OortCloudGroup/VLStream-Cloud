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
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationImageExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationImageService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAnnotationImageWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标注图片信息表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAnnotationImage")
@Tag(name = "标注图片信息表", description = "标注图片信息表接口")
public class VlsAnnotationImageController extends BladeController {

	private final IVlsAnnotationImageService vlsAnnotationImageService;

	/**
	 * 标注图片信息表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description  = "传入vlsAnnotationImage")
	public R<AnnotationImageVO> detail(AnnotationImage vlsAnnotationImage) {
		AnnotationImage detail = vlsAnnotationImageService.getOne(Condition.getQueryWrapper(vlsAnnotationImage));
		return R.data(VlsAnnotationImageWrapper.build().entityVO(detail));
	}

	/**
	 * 标注图片信息表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description  = "传入vlsAnnotationImage")
	public R<IPage<AnnotationImageVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationImage, Query query) {
		IPage<AnnotationImage> pages = vlsAnnotationImageService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAnnotationImage, AnnotationImage.class));
		return R.data(VlsAnnotationImageWrapper.build().pageVO(pages));
	}


	/**
	 * 标注图片信息表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description  = "传入vlsAnnotationImage")
	public R<IPage<AnnotationImageVO>> page(AnnotationImageVO vlsAnnotationImage, Query query) {
		IPage<AnnotationImageVO> pages = vlsAnnotationImageService.selectVlsAnnotationImagePage(Condition.getPage(query), vlsAnnotationImage);
		return R.data(pages);
	}

	/**
	 * 标注图片信息表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description  = "传入vlsAnnotationImage")
	public R save(@Valid @RequestBody AnnotationImage vlsAnnotationImage) {
		return R.status(vlsAnnotationImageService.save(vlsAnnotationImage));
	}

	/**
	 * 标注图片信息表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description  = "传入vlsAnnotationImage")
	public R update(@Valid @RequestBody AnnotationImage vlsAnnotationImage) {
		return R.status(vlsAnnotationImageService.updateById(vlsAnnotationImage));
	}

	/**
	 * 标注图片信息表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description  = "传入vlsAnnotationImage")
	public R submit(@Valid @RequestBody AnnotationImage vlsAnnotationImage) {
		return R.status(vlsAnnotationImageService.saveOrUpdate(vlsAnnotationImage));
	}

	/**
	 * 标注图片信息表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description  = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAnnotationImageService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAnnotationImage")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description  = "传入vlsAnnotationImage")
	public void exportVlsAnnotationImage(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationImage, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AnnotationImage> queryWrapper = Condition.getQueryWrapper(vlsAnnotationImage, AnnotationImage.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAnnotationImageEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAnnotationImageEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAnnotationImageExcel> list = vlsAnnotationImageService.exportVlsAnnotationImage(queryWrapper);
		ExcelUtil.export(response, "标注图片信息表数据" + DateUtil.today(), "标注图片信息表数据表", list, VlsAnnotationImageExcel.class);
	}

	/**
	 * 上传标注图片
	 */
	@PostMapping("/upload")
	public ResponseEntity<?> uploadImages(
		@RequestPart("files") MultipartFile[] files,
		@RequestParam("annotationId") Long annotationId) {
		try {
			List<AnnotationImage> images = vlsAnnotationImageService.uploadImages(files, annotationId);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "图片上传成功");
			response.put("data", images);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "图片上传失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 获取数据集的所有图片
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
			errorResponse.put("message", "获取图片列表失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 获取图片详情
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
			errorResponse.put("message", "获取图片详情失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 更新图片标注信息
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateImage(@PathVariable Long id, @RequestBody AnnotationImage image) {
		try {
			image.setId(id);
			AnnotationImage updatedImage = vlsAnnotationImageService.updateImage(image);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "图片信息更新成功");
			response.put("data", updatedImage);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "更新图片信息失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 删除图片
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteImage(@PathVariable Long id) {
		try {
			vlsAnnotationImageService.deleteImage(id);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "图片删除成功");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "删除图片失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 批量删除图片
	 */
	@DeleteMapping("/batch")
	public ResponseEntity<?> batchDeleteImages(@RequestBody List<Long> ids) {
		try {
			vlsAnnotationImageService.batchDeleteImages(ids);
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "批量删除成功");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "批量删除失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 获取数据集统计信息
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
			errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * 批量保存图片信息到annotation_image表
	 *
	 * @param annotationImages 图片信息列表
	 * @return 保存结果
	 */
	@PostMapping("/images/batch")
	public ResponseEntity<?> batchSaveImages(@RequestBody List<AnnotationImage> annotationImages) {
		try {
			log.info("批量保存图片信息，数量: {}", annotationImages.size());

			boolean success = vlsAnnotationImageService.batchSaveImages(annotationImages);
			if (success) {
				Map<String, Object> response = new HashMap<>();
				response.put("success", true);
				response.put("message", "批量保存图片信息成功");
				response.put("data", annotationImages.size());
				return ResponseEntity.ok(response);
			} else {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("message", "批量保存图片信息失败");
				return ResponseEntity.badRequest().body(errorResponse);
			}
		} catch (Exception e) {
			log.error("批量保存图片信息失败", e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "批量保存图片信息失败: " + e.getMessage());
			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

}
