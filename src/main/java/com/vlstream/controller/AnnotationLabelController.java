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
 * Annotation Label Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/annotation")
@CrossOrigin(origins = "*")
@Api(tags = "Annotation Label Management")
public class AnnotationLabelController {

    @Autowired
    private AnnotationLabelService annotationLabelService;

    @Autowired
    private AnnotationInstanceService annotationInstanceService;

    @Autowired
    private AlgorithmAnnotationService algorithmAnnotationService;

    /**
     * Get label list of annotation project
     *
     * @param annotationId Annotation project ID
     * @param keyword      Search keyword (optional)
     * @return Label list
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
            log.error("Failed to get label list", e);
            return Result.error("Failed to get label list: " + e.getMessage());
        }
    }

    /**
     * Create label
     *
     * @param annotationId Annotation project ID
     * @param requestBody  Request body
     * @return Created label
     */
    @PostMapping("/{annotationId}/labels")
    public Result<AnnotationLabel> createLabel(@PathVariable Long annotationId,
                                               @RequestBody Map<String, Object> requestBody) {
        try {
            String name = (String) requestBody.get("name");
            String color = (String) requestBody.get("color");
            String description = (String) requestBody.get("description");

            if (name == null || name.trim().isEmpty()) {
                return Result.error("Label name cannot be empty");
            }
            if (color == null || color.trim().isEmpty()) {
                return Result.error("Label color cannot be empty");
            }

            AnnotationLabel label = annotationLabelService.createLabel(annotationId, name.trim(), color.trim(), description);
            return Result.success(label);
        } catch (Exception e) {
            log.error("Failed to create label", e);
            return Result.error("Failed to create label: " + e.getMessage());
        }
    }

    /**
     * Update label
     *
     * @param labelId     Label ID
     * @param requestBody Request body
     * @return Updated label
     */
    @PutMapping("/labels/{labelId}")
    public Result<AnnotationLabel> updateLabel(@PathVariable Long labelId,
                                               @RequestBody Map<String, Object> requestBody) {
        try {
            String name = (String) requestBody.get("name");
            String color = (String) requestBody.get("color");
            String description = (String) requestBody.get("description");

            if (name == null || name.trim().isEmpty()) {
                return Result.error("Label name cannot be empty");
            }
            if (color == null || color.trim().isEmpty()) {
                return Result.error("Label color cannot be empty");
            }

            AnnotationLabel label = annotationLabelService.updateLabel(labelId, name.trim(), color.trim(), description);
            return Result.success(label);
        } catch (Exception e) {
            log.error("Failed to update label", e);
            return Result.error("Failed to update label: " + e.getMessage());
        }
    }

    /**
     * Delete label
     *
     * @param labelId Label ID
     * @return Delete result
     */
    @DeleteMapping("/labels/{labelId}")
    public Result<Boolean> deleteLabel(@PathVariable Long labelId) {
        try {
            boolean success = annotationLabelService.deleteLabel(labelId);
            return Result.success(success);
        } catch (Exception e) {
            log.error("Failed to delete label", e);
            return Result.error("Failed to delete label: " + e.getMessage());
        }
    }

    /**
     * Batch update label sort order
     *
     * @param annotationId Annotation project ID
     * @param requestBody  Request body (contains labelIds array)
     * @return Update result
     */
    @PutMapping("/{annotationId}/labels/sort")
    public Result<Boolean> updateLabelSort(@PathVariable Long annotationId,
                                           @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> labelIds = (List<Long>) requestBody.get("labelIds");

            if (labelIds == null || labelIds.isEmpty()) {
                return Result.error("Label ID list cannot be empty");
            }

            boolean success = annotationLabelService.updateSortOrder(annotationId, labelIds);
            return Result.success(success);
        } catch (Exception e) {
            log.error("Failed to update label sort order", e);
            return Result.error("Failed to update label sort order: " + e.getMessage());
        }
    }

    /**
     * Get annotation instance list of image
     *
     * @param annotationId Annotation project ID
     * @param imageName    Image name
     * @return Annotation instance list
     */
    @GetMapping("/{annotationId}/instances")
    public Result<List<AnnotationInstance>> getAnnotationInstances(@PathVariable Long annotationId,
                                                                   @RequestParam String imageName) {
        try {
            List<AnnotationInstance> instances = annotationInstanceService.getByAnnotationIdAndImageName(annotationId, imageName);
            return Result.success(instances);
        } catch (Exception e) {
            log.error("Failed to get annotation instances", e);
            return Result.error("Failed to get annotation instances: " + e.getMessage());
        }
    }

    /**
     * Get all annotation instance list of annotation project
     *
     * @param annotationId Annotation project ID
     * @return Annotation instance list
     */
    @GetMapping("/{annotationId}/instances/all")
    public Result<List<AnnotationInstance>> getAllAnnotationInstances(@PathVariable Long annotationId) {
        try {
            List<AnnotationInstance> instances = annotationInstanceService.getByAnnotationId(annotationId);
            return Result.success(instances);
        } catch (Exception e) {
            log.error("Failed to get all annotation project instances", e);
            return Result.error("Failed to get all annotation project instances: " + e.getMessage());
        }
    }

    /**
     * Save annotation instance
     *
     * @param annotationId Annotation project ID
     * @param requestBody  Request body
     * @return Saved annotation instance
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
            log.error("Failed to save annotation instance", e);
            return Result.error("Failed to save annotation instance: " + e.getMessage());
        }
    }

    /**
     * Batch save annotation instances of image
     *
     * @param annotationId Annotation project ID
     * @param requestBody  Request body
     * @return Save result
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
                log.warn("Failed to update statistics after batch save: annotationId={}, error={}", annotationId, statEx.getMessage());
            }

            return Result.success(success);
        } catch (Exception e) {
            log.error("Failed to batch save annotation instances", e);
            return Result.error("Failed to batch save annotation instances: " + e.getMessage());
        }
    }

    /**
     * Delete annotation instance
     *
     * @param instanceId Instance ID
     * @return Delete result
     */
    @DeleteMapping("/instances/{instanceId}")
    public Result<Boolean> deleteAnnotationInstance(@PathVariable Long instanceId) {
        log.info("Received delete annotation instance request: instanceId={}", instanceId);
        try {
            boolean success = annotationInstanceService.deleteAnnotation(instanceId);
            log.info("Delete annotation instance result: instanceId={}, success={}", instanceId, success);
            return Result.success(success);
        } catch (Exception e) {
            log.error("Failed to delete annotation instance: instanceId={}", instanceId, e);
            return Result.error("Failed to delete annotation instance: " + e.getMessage());
        }
    }

    /**
     * Batch delete annotation instances
     *
     * @param instanceIds Instance ID list
     * @return Delete result
     */
    @DeleteMapping("/instances/batch")
    public Result<String> batchDeleteAnnotationInstances(@RequestBody List<Long> instanceIds) {
        log.info("Batch delete annotation instances: IDs={}", instanceIds);

        if (instanceIds == null || instanceIds.isEmpty()) {
            return Result.error("Instance ID list cannot be empty");
        }

        try {
            int totalDeleted = 0;
            for (Long instanceId : instanceIds) {
                boolean success = annotationInstanceService.deleteAnnotation(instanceId);
                if (success) {
                    totalDeleted++;
                }
            }

            log.info("Batch delete completed, successfully deleted {} instances", totalDeleted);
            return Result.success("Batch delete successful, deleted " + totalDeleted + " instances");
        } catch (Exception e) {
            log.error("Failed to batch delete annotation instances", e);
            return Result.error("Batch delete failed: " + e.getMessage());
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
     * Calculate annotation progress (0-100)
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
     * Calculate annotation status based on progress
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
