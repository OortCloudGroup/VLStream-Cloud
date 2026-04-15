package com.vlstream.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vlstream.common.Result;
import com.vlstream.entity.AlgorithmAnnotation;
import com.vlstream.entity.AnnotationInstance;
import com.vlstream.entity.AnnotationLabel;
import com.vlstream.service.AlgorithmAnnotationService;
import com.vlstream.service.AnnotationInstanceService;
import com.vlstream.service.AnnotationLabelService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标注标签Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/annotation")
@CrossOrigin(origins = "*")
@Api(tags = "标注标签管理")
public class AnnotationLabelController {

    @Autowired
    private AnnotationLabelService annotationLabelService;

    @Autowired
    private AnnotationInstanceService annotationInstanceService;

    @Autowired
    private AlgorithmAnnotationService algorithmAnnotationService;

    /**
     * 获取标注项目的标签列表
     *
     * @param annotationId 标注项目ID
     * @param keyword      搜索关键词（可选）
     * @return 标签列表
     */
    @GetMapping("/{annotationId}/labels")
    public Result<List<AnnotationLabel>> getLabels(@PathVariable Long annotationId,
                                                   @RequestParam(required = false) String keyword) {
        try {
            List<AnnotationLabel> labels;
            if (keyword != null && !keyword.trim().isEmpty()) {
                labels = annotationLabelService.searchLabels(annotationId, keyword.trim());
            } else {
                labels = annotationLabelService.getByAnnotationIdWithUsageCount(annotationId);
            }
            return Result.success(labels);
        } catch (Exception e) {
            log.error("获取标签列表失败", e);
            return Result.error("获取标签列表失败: " + e.getMessage());
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
    public Result<AnnotationLabel> createLabel(@PathVariable Long annotationId,
                                               @RequestBody Map<String, Object> requestBody) {
        try {
            String name = (String) requestBody.get("name");
            String color = (String) requestBody.get("color");
            String description = (String) requestBody.get("description");

            if (name == null || name.trim().isEmpty()) {
                return Result.error("标签名称不能为空");
            }
            if (color == null || color.trim().isEmpty()) {
                return Result.error("标签颜色不能为空");
            }

            AnnotationLabel label = annotationLabelService.createLabel(annotationId, name.trim(), color.trim(), description);
            return Result.success(label);
        } catch (Exception e) {
            log.error("创建标签失败", e);
            return Result.error("创建标签失败: " + e.getMessage());
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
    public Result<AnnotationLabel> updateLabel(@PathVariable Long labelId,
                                               @RequestBody Map<String, Object> requestBody) {
        try {
            String name = (String) requestBody.get("name");
            String color = (String) requestBody.get("color");
            String description = (String) requestBody.get("description");

            if (name == null || name.trim().isEmpty()) {
                return Result.error("标签名称不能为空");
            }
            if (color == null || color.trim().isEmpty()) {
                return Result.error("标签颜色不能为空");
            }

            AnnotationLabel label = annotationLabelService.updateLabel(labelId, name.trim(), color.trim(), description);
            return Result.success(label);
        } catch (Exception e) {
            log.error("更新标签失败", e);
            return Result.error("更新标签失败: " + e.getMessage());
        }
    }

    /**
     * 删除标签
     *
     * @param labelId 标签ID
     * @return 删除结果
     */
    @DeleteMapping("/labels/{labelId}")
    public Result<Boolean> deleteLabel(@PathVariable Long labelId) {
        try {
            boolean success = annotationLabelService.deleteLabel(labelId);
            return Result.success(success);
        } catch (Exception e) {
            log.error("删除标签失败", e);
            return Result.error("删除标签失败: " + e.getMessage());
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
    public Result<Boolean> updateLabelSort(@PathVariable Long annotationId,
                                           @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> labelIds = (List<Long>) requestBody.get("labelIds");

            if (labelIds == null || labelIds.isEmpty()) {
                return Result.error("标签ID列表不能为空");
            }

            boolean success = annotationLabelService.updateSortOrder(annotationId, labelIds);
            return Result.success(success);
        } catch (Exception e) {
            log.error("更新标签排序失败", e);
            return Result.error("更新标签排序失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片的标注实例列表
     *
     * @param annotationId 标注项目ID
     * @param imageName    图片名称
     * @return 标注实例列表
     */
    @GetMapping("/{annotationId}/instances")
    public Result<List<AnnotationInstance>> getAnnotationInstances(@PathVariable Long annotationId,
                                                                   @RequestParam String imageName) {
        try {
            List<AnnotationInstance> instances = annotationInstanceService.getByAnnotationIdAndImageName(annotationId, imageName);
            return Result.success(instances);
        } catch (Exception e) {
            log.error("获取标注实例失败", e);
            return Result.error("获取标注实例失败: " + e.getMessage());
        }
    }

    /**
     * 获取标注项目的所有标注实例列表
     *
     * @param annotationId 标注项目ID
     * @return 标注实例列表
     */
    @GetMapping("/{annotationId}/instances/all")
    public Result<List<AnnotationInstance>> getAllAnnotationInstances(@PathVariable Long annotationId) {
        try {
            List<AnnotationInstance> instances = annotationInstanceService.getByAnnotationId(annotationId);
            return Result.success(instances);
        } catch (Exception e) {
            log.error("获取标注项目所有实例失败", e);
            return Result.error("获取标注项目所有实例失败: " + e.getMessage());
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
    public Result<AnnotationInstance> saveAnnotationInstance(@PathVariable Long annotationId,
                                                             @RequestBody Map<String, Object> requestBody) {
        try {
            Long labelId = Long.valueOf(requestBody.get("labelId").toString());
            Long imageId = (Long) requestBody.get("imageId");
            String annotationType = (String) requestBody.get("annotationType");
            String annotationData = (String) requestBody.get("annotationData");

            AnnotationInstance instance = annotationInstanceService.saveAnnotation(annotationId, labelId, imageId, annotationType, annotationData);
            return Result.success(instance);
        } catch (Exception e) {
            log.error("保存标注实例失败", e);
            return Result.error("保存标注实例失败: " + e.getMessage());
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
    public Result<Boolean> batchSaveAnnotationInstances(@PathVariable Long annotationId,
                                                        @RequestBody Map<String, Object> requestBody) {
        try {
            Long imageId = Long.valueOf(requestBody.get("imageId").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> instancesData = (List<Map<String, Object>>) requestBody.get("instances");

            List<AnnotationInstance> instances = instancesData.stream().map(data -> {
                AnnotationInstance instance = new AnnotationInstance();
                instance.setAnnotationId(annotationId);
                instance.setLabelId(Long.valueOf(data.get("labelId").toString()));
                instance.setImageId(imageId);
                instance.setAnnotationType((String) data.get("annotationType"));
                instance.setAnnotationData((String) data.get("annotationData"));
                return instance;
            }).collect(Collectors.toList());

            boolean success = annotationInstanceService.batchSaveAnnotations(annotationId, imageId, instances);

            // 更新标注统计信息
            try {
                int annotatedCount = Math.toIntExact(annotationInstanceService.count(new QueryWrapper<AnnotationInstance>()
                        .eq("annotation_id", annotationId)
                        .eq("deleted", 0)));

                AlgorithmAnnotation annotation = algorithmAnnotationService.getById(annotationId);
                if (annotation != null) {
                    int totalCount = annotation.getTotalCount() == null ? annotatedCount : annotation.getTotalCount();
                    int progress = calculateProgress(annotatedCount, totalCount);

                    annotation.setAnnotatedCount(annotatedCount);
                    annotation.setTotalCount(totalCount);
                    annotation.setProgress(progress);
                    annotation.setAnnotationStatus(calculateAnnotationStatus(progress));
                    algorithmAnnotationService.updateById(annotation);
                }
            } catch (Exception statEx) {
                log.warn("批量保存后更新统计信息失败: annotationId={}, error={}", annotationId, statEx.getMessage());
            }

            return Result.success(success);
        } catch (Exception e) {
            log.error("批量保存标注实例失败", e);
            return Result.error("批量保存标注实例失败: " + e.getMessage());
        }
    }

    /**
     * 删除标注实例
     *
     * @param instanceId 实例ID
     * @return 删除结果
     */
    @DeleteMapping("/instances/{instanceId}")
    public Result<Boolean> deleteAnnotationInstance(@PathVariable Long instanceId) {
        log.info("收到删除标注实例请求: instanceId={}", instanceId);
        try {
            boolean success = annotationInstanceService.deleteAnnotation(instanceId);
            log.info("删除标注实例结果: instanceId={}, success={}", instanceId, success);
            return Result.success(success);
        } catch (Exception e) {
            log.error("删除标注实例失败: instanceId={}", instanceId, e);
            return Result.error("删除标注实例失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除标注实例
     *
     * @param instanceIds 实例ID列表
     * @return 删除结果
     */
    @DeleteMapping("/instances/batch")
    public Result<String> batchDeleteAnnotationInstances(@RequestBody List<Long> instanceIds) {
        log.info("批量删除标注实例：IDs={}", instanceIds);

        if (instanceIds == null || instanceIds.isEmpty()) {
            return Result.error("实例ID列表不能为空");
        }

        try {
            int totalDeleted = 0;
            for (Long instanceId : instanceIds) {
                boolean success = annotationInstanceService.deleteAnnotation(instanceId);
                if (success) {
                    totalDeleted++;
                }
            }

            log.info("批量删除完成，成功删除 {} 个实例", totalDeleted);
            return Result.success("批量删除成功，共删除 " + totalDeleted + " 个实例");
        } catch (Exception e) {
            log.error("批量删除标注实例失败", e);
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * Batch delete annotation instances and related data by image names.
     *
     * @param params request body containing annotationId and imageNames/imageName
     * @return deletion result message
     */
    @DeleteMapping("/instances/by-image")
    public Result<String> deleteAnnotationInstancesByImage(@RequestBody Map<String, Object> params) {
        Object annotationIdObj = params.get("annotationId");
        if (annotationIdObj == null) {
            return Result.error("annotationId cannot be null");
        }
        Long annotationId = Long.valueOf(annotationIdObj.toString());

        List<Long> imageIds = new ArrayList<>();
        Object imageIdsObj = params.get("imageIds");
        if (imageIdsObj instanceof List<?>) {
            List<Long> finalImageIds = imageIds;
            ((List<?>) imageIdsObj).forEach(item -> {
                if (item != null) {
                    finalImageIds.add(Long.valueOf(item.toString()));
                }
            });
        }

        imageIds = imageIds.stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (imageIds.isEmpty()) {
            return Result.error("imageNames cannot be empty");
        }

        log.info("Batch deleting annotation data: annotationId={}, imageNames={}", annotationId, imageIds);

        List<Long> failedImageNames = new ArrayList<>();
        for (Long imageId : imageIds) {
            try {
                boolean success = annotationInstanceService.deleteImageAndRelatedData(annotationId, imageId);
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
            return Result.success("Deleted annotation data for " + imageIds.size() + " images");
        }

        String errorMsg = "Failed to delete images: " + StringUtils.join(", ", failedImageNames);
        if (failedImageNames.size() == imageIds.size()) {
            return Result.error(errorMsg);
        }
        return Result.error(errorMsg + "; others deleted");
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
}
