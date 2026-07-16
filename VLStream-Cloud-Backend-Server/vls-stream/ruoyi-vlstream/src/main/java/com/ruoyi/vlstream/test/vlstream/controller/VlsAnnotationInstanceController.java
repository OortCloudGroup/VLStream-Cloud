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
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationStatusEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationInstanceVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmAnnotationService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationInstanceService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAnnotationInstanceWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标注实例实体类 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAnnotationInstance")
@Tag(name = "标注实例实体类", description = "标注实例实体类接口")
public class VlsAnnotationInstanceController extends BladeController {

	private final IVlsAnnotationInstanceService vlsAnnotationInstanceService;
	private final IVlsAlgorithmAnnotationService algorithmAnnotationService;

	/**
	 * 标注实例实体类 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsAnnotationInstance")
	public R<AnnotationInstanceVO> detail(AnnotationInstance vlsAnnotationInstance) {
		AnnotationInstance detail = vlsAnnotationInstanceService.getOne(Condition.getQueryWrapper(vlsAnnotationInstance));
		return R.data(VlsAnnotationInstanceWrapper.build().entityVO(detail));
	}

	/**
	 * 标注实例实体类 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsAnnotationInstance")
	public R<IPage<AnnotationInstanceVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationInstance, Query query) {
		IPage<AnnotationInstance> pages = vlsAnnotationInstanceService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAnnotationInstance, AnnotationInstance.class));
		return R.data(VlsAnnotationInstanceWrapper.build().pageVO(pages));
	}


	/**
	 * 标注实例实体类 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsAnnotationInstance")
	public R<IPage<AnnotationInstanceVO>> page(AnnotationInstanceVO vlsAnnotationInstance, Query query) {
		IPage<AnnotationInstanceVO> pages = vlsAnnotationInstanceService.selectVlsAnnotationInstancePage(Condition.getPage(query), vlsAnnotationInstance);
		return R.data(pages);
	}

	/**
	 * 标注实例实体类 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsAnnotationInstance")
	public R save(@Valid @RequestBody AnnotationInstance vlsAnnotationInstance) {
		return R.status(vlsAnnotationInstanceService.save(vlsAnnotationInstance));
	}

	/**
	 * 标注实例实体类 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsAnnotationInstance")
	public R update(@Valid @RequestBody AnnotationInstance vlsAnnotationInstance) {
		return R.status(vlsAnnotationInstanceService.updateById(vlsAnnotationInstance));
	}

	/**
	 * 标注实例实体类 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsAnnotationInstance")
	public R submit(@Valid @RequestBody AnnotationInstance vlsAnnotationInstance) {
		return R.status(vlsAnnotationInstanceService.saveOrUpdate(vlsAnnotationInstance));
	}

	/**
	 * 标注实例实体类 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAnnotationInstanceService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAnnotationInstance")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsAnnotationInstance")
	public void exportVlsAnnotationInstance(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnnotationInstance, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AnnotationInstance> queryWrapper = Condition.getQueryWrapper(vlsAnnotationInstance, AnnotationInstance.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAnnotationInstanceEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAnnotationInstanceEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAnnotationInstanceExcel> list = vlsAnnotationInstanceService.exportVlsAnnotationInstance(queryWrapper);
		ExcelUtil.export(response, "标注实例实体类数据" + DateUtil.today(), "标注实例实体类数据表", list, VlsAnnotationInstanceExcel.class);
	}


	/**
	 * 获取图片的标注实例列表
	 *
	 * @param annotationId 标注项目ID
	 * @param imageName    图片名称
	 * @return 标注实例列表
	 */
	@GetMapping("/{annotationId}/instances")
	public R<List<AnnotationInstance>> getAnnotationInstances(@PathVariable Long annotationId,
															  @RequestParam String imageName) {
		try {
			List<AnnotationInstance> instances = vlsAnnotationInstanceService.getByAnnotationIdAndImageName(annotationId, imageName);
			return R.data(instances);
		} catch (Exception e) {
			log.error("获取标注实例失败", e);
			return R.fail("获取标注实例失败: " + e.getMessage());
		}
	}

	/**
	 * 获取标注项目的所有标注实例列表
	 *
	 * @param annotationId 标注项目ID
	 * @return 标注实例列表
	 */
	@GetMapping("/{annotationId}/instances/all")
	public R<List<AnnotationInstanceVO>> getAllAnnotationInstances(@PathVariable Long annotationId) {
		try {
			List<AnnotationInstance> instances = vlsAnnotationInstanceService.getByAnnotationId(annotationId);
			return R.data(VlsAnnotationInstanceWrapper.build().listVO(instances));
		} catch (Exception e) {
			log.error("获取标注项目所有实例失败", e);
			return R.fail("获取标注项目所有实例失败: " + e.getMessage());
		}
	}

	/**
	 * 保存标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param requestBody  请求体
	 * @return 保存的标注实例
	 */
	@PostMapping("/{annotationId}/instances")
	public R<AnnotationInstance> saveAnnotationInstance(@PathVariable Long annotationId,
													@RequestBody Map<String, Object> requestBody) {
		try {
			Long labelId = parseLong(requestBody.get("labelId"));
			Long imageId = parseLong(requestBody.get("imageId"));
			String annotationType = requestBody.get("annotationType") == null ? null : requestBody.get("annotationType").toString();
			String annotationData = (String) requestBody.get("annotationData");

			if (labelId == null || imageId == null) {
				return R.fail("labelId或imageId不能为空");
			}
			if (StringUtils.isBlank(annotationType)) {
				return R.fail("annotationType不能为空");
			}
			AlgorithmAnnotationTypeEnum typeEnum = AlgorithmAnnotationTypeEnum.of(annotationType);
			if (typeEnum == null) {
				return R.fail("annotationType不合法");
			}

			AnnotationInstance instance = vlsAnnotationInstanceService.saveAnnotation(annotationId, labelId, imageId, typeEnum, annotationData);
			return R.data(instance);
		} catch (Exception e) {
			log.error("保存标注实例失败", e);
			return R.fail("保存标注实例失败: " + e.getMessage());
		}
	}

	/**
	 * 批量保存图片的标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param requestBody  请求体
	 * @return 保存结果
	 */
	@PostMapping("/{annotationId}/instances/batch")
	public R<Boolean> batchSaveAnnotationInstances(@PathVariable Long annotationId,
													   @RequestBody Map<String, Object> requestBody) {
		try {
			Long imageId = parseLong(requestBody.get("imageId"));
			Object instancesObj = requestBody.get("instances");
			if (imageId == null) {
				return R.fail("imageId不能为空");
			}
			if (!(instancesObj instanceof List<?>) || ((List<?>) instancesObj).isEmpty()) {
				return R.fail("instances不能为空");
			}
			List<?> instancesData = (List<?>) instancesObj;

			List<AnnotationInstance> instances = new ArrayList<>();
			for (Object rawItem : instancesData) {
				if (!(rawItem instanceof Map<?, ?>)) {
					return R.fail("instances格式不合法");
				}
				Map<?, ?> data = (Map<?, ?>) rawItem;
				Long labelId = parseLong(data.get("labelId"));
				Object annotationTypeObj = data.get("annotationType");
				String annotationType = annotationTypeObj == null ? null : annotationTypeObj.toString();
				if (labelId == null) {
					return R.fail("labelId不能为空");
				}
				if (StringUtils.isBlank(annotationType)) {
					return R.fail("annotationType不能为空");
				}
				AlgorithmAnnotationTypeEnum typeEnum = AlgorithmAnnotationTypeEnum.of(annotationType);
				if (typeEnum == null) {
					return R.fail("annotationType不合法");
				}
				AnnotationInstance instance = new AnnotationInstance();
				instance.setAnnotationId(annotationId);
				instance.setLabelId(labelId);
				instance.setImageId(imageId);
				instance.setAnnotationType(typeEnum);
				Object annotationDataObj = data.get("annotationData");
				instance.setAnnotationData(annotationDataObj == null ? null : annotationDataObj.toString());
				instances.add(instance);
			}

			boolean success = vlsAnnotationInstanceService.batchSaveAnnotations(annotationId, imageId, instances);

			// 更新标注统计信息
			try {
				int annotatedCount = Math.toIntExact(vlsAnnotationInstanceService.count(new QueryWrapper<AnnotationInstance>()
					.eq("annotation_id", annotationId)
					.eq("is_deleted", 1)));

				AlgorithmAnnotation annotation = algorithmAnnotationService.getById(annotationId);
				if (annotation != null) {
					int totalCount = annotation.getTotalCount() == null ? annotatedCount : annotation.getTotalCount();
					int progress = calculateProgress(annotatedCount, totalCount);

					annotation.setAnnotatedCount(annotatedCount);
					annotation.setTotalCount(totalCount);
					annotation.setProgress(progress);
					annotation.setAnnotationStatus(AlgorithmAnnotationStatusEnum.of(calculateAnnotationStatus(progress)));
					algorithmAnnotationService.updateById(annotation);
				}
			} catch (Exception statEx) {
				log.warn("批量保存后更新统计信息失败: annotationId={}, error={}", annotationId, statEx.getMessage());
			}

			return R.data(success);
		} catch (Exception e) {
			log.error("批量保存标注实例失败", e);
			return R.fail("批量保存标注实例失败: " + e.getMessage());
		}
	}

	/**
	 * 删除标注实例
	 *
	 * @param instanceId 实例ID
	 * @return 删除结果
	 */
	@DeleteMapping("/instances/{instanceId}")
	public R<Boolean> deleteAnnotationInstance(@PathVariable Long instanceId) {
		log.info("收到删除标注实例请求: instanceId={}", instanceId);
		try {
			boolean success = vlsAnnotationInstanceService.deleteAnnotation(instanceId);
			log.info("删除标注实例结果: instanceId={}, success={}", instanceId, success);
			return R.data(success);
		} catch (Exception e) {
			log.error("删除标注实例失败: instanceId={}", instanceId, e);
			return R.fail("删除标注实例失败: " + e.getMessage());
		}
	}

	/**
	 * 批量删除标注实例
	 *
	 * @param instanceIds 实例ID列表
	 * @return 删除结果
	 */
	@DeleteMapping("/instances/batch")
	public R<String> batchDeleteAnnotationInstances(@RequestBody List<Long> instanceIds) {
		log.info("批量删除标注实例：IDs={}", instanceIds);

		if (instanceIds == null || instanceIds.isEmpty()) {
			return R.fail("实例ID列表不能为空");
		}

		try {
			int totalDeleted = 0;
			for (Long instanceId : instanceIds) {
				boolean success = vlsAnnotationInstanceService.deleteAnnotation(instanceId);
				if (success) {
					totalDeleted++;
				}
			}

			log.info("批量删除完成，成功删除 {} 个实例", totalDeleted);
			return R.success("批量删除成功，共删除 " + totalDeleted + " 个实例");
		} catch (Exception e) {
			log.error("批量删除标注实例失败", e);
			return R.fail("批量删除失败：" + e.getMessage());
		}
	}

	/**
	 * Batch delete annotation instances and related data by image names.
	 *
	 * @param params request body containing annotationId and imageNames/imageName
	 * @return deletion result message
	 */
	@DeleteMapping("/instances/by-image")
	public R<String> deleteAnnotationInstancesByImage(@RequestBody Map<String, Object> params) {
		Object annotationIdObj = params.get("annotationId");
		if (annotationIdObj == null) {
			return R.fail("annotationId cannot be null");
		}
		Long annotationId = parseLong(annotationIdObj);
		if (annotationId == null) {
			return R.fail("annotationId不合法");
		}

		List<Long> imageIds = new ArrayList<>();
		Object imageIdsObj = params.get("imageIds");
		if (imageIdsObj instanceof List<?>) {
			List<Long> finalImageIds = imageIds;
			((List<?>) imageIdsObj).forEach(item -> {
				if (item != null) {
					Long imageId = parseLong(item);
					if (imageId != null) {
						finalImageIds.add(imageId);
					}
				}
			});
		}

		imageIds = imageIds.stream()
			.filter(id -> id != null)
			.distinct()
			.collect(Collectors.toList());

		if (imageIds.isEmpty()) {
			return R.fail("imageNames cannot be empty");
		}

		log.info("Batch deleting annotation data: annotationId={}, imageNames={}", annotationId, imageIds);

		List<Long> failedImageNames = new ArrayList<>();
		for (Long imageId : imageIds) {
			try {
				boolean success = vlsAnnotationInstanceService.deleteImageAndRelatedData(annotationId, imageId);
				if (success) {
					log.info("Deleted annotation data for image: annotationId={}, imageName={}", annotationId, imageId);
				} else {
					log.warn("Delete annotation data failed for image: annotationId={}, imageName={}", annotationId, imageId);
					failedImageNames.add(imageId);
				}
			} catch (Exception e) {
				log.error("Exception deleting annotation data: annotationId={}, imageName={}", annotationId, imageId, e);
				failedImageNames.add(imageId);
			}
		}

		if (failedImageNames.isEmpty()) {
			return R.success("Deleted annotation data for " + imageIds.size() + " images");
		}

		String errorMsg = "Failed to delete images: " + StringUtils.join(", ", failedImageNames);
		if (failedImageNames.size() == imageIds.size()) {
			return R.fail(errorMsg);
		}
		return R.fail(errorMsg + "; others deleted");
	}

	/**
	 * 计算标注进度（0-100）
	 */
	private int calculateProgress(Integer annotatedCount, Integer totalCount) {
		if (totalCount == null || totalCount == 0) {
			return 0;
		}
		if (annotatedCount == null) {
			return 0;
		}
		return Math.min(100, (annotatedCount * 100) / totalCount);
	}

	/**
	 * 根据进度计算标注状态
	 */
	private String calculateAnnotationStatus(int progress) {
		if (progress == 0) {
			return "none";
		} else if (progress < 100) {
			return "partial";
		} else {
			return "completed";
		}
	}

	private Long parseLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.valueOf(value.toString());
		} catch (NumberFormatException numberFormatException) {
			return null;
		}
	}

}
