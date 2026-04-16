package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.AnnotationInstance;
import com.vlstream.entity.AnnotationImage;
import com.vlstream.mapper.AnnotationInstanceMapper;
import com.vlstream.service.AnnotationInstanceService;
import com.vlstream.service.AnnotationLabelService;
import com.vlstream.service.AnnotationImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Annotation Instance Service Implementation Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class AnnotationInstanceServiceImpl extends ServiceImpl<AnnotationInstanceMapper, AnnotationInstance> implements AnnotationInstanceService {

    @Autowired
    private AnnotationLabelService annotationLabelService;

    @Autowired
    private AnnotationImageService annotationImageService;

    @Override
    public List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName) {
        log.debug("Query annotation instances: annotationId={}, imageName={}", annotationId, imageName);
        return baseMapper.selectByAnnotationIdAndImageName(annotationId, imageName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, String annotationType, String annotationData) {
        log.info("Saving annotation instance: annotationId={}, labelId={}, imageId={}", annotationId, labelId, imageId);
        
        AnnotationInstance instance = new AnnotationInstance();
        instance.setAnnotationId(annotationId);
        instance.setLabelId(labelId);
        instance.setImageId(imageId);
        instance.setAnnotationType(annotationType);
        instance.setAnnotationData(annotationData);
        instance.setConfidence(new BigDecimal("1.0000"));
        instance.setVerified(false);
        instance.setCreatedTime(LocalDateTime.now());
        instance.setUpdatedTime(LocalDateTime.now());

        baseMapper.insert(instance);
        
        // Update label usage count
        annotationLabelService.updateUsageCount(labelId);
        
        log.info("Annotation instance saved successfully, ID: {}", instance.getId());
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationInstance updateAnnotation(Long instanceId, Long labelId, 
                                             String annotationType, String annotationData) {
        log.info("Updating annotation instance: instanceId={}, labelId={}", instanceId, labelId);
        
        AnnotationInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("Annotation instance does not exist");
        }

        Long oldLabelId = instance.getLabelId();
        
        instance.setLabelId(labelId);
        instance.setAnnotationType(annotationType);
        instance.setAnnotationData(annotationData);
        instance.setUpdatedTime(LocalDateTime.now());

        baseMapper.updateById(instance);
        
        // Update label usage count
        if (!oldLabelId.equals(labelId)) {
            annotationLabelService.updateUsageCount(oldLabelId);
            annotationLabelService.updateUsageCount(labelId);
        }
        
        log.info("Annotation instance updated successfully");
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotation(Long instanceId) {
        log.info("Deleting annotation instance: instanceId={}", instanceId);
        
        AnnotationInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("Annotation instance does not exist");
        }

        Long labelId = instance.getLabelId();

        int result = baseMapper.deleteById(instanceId);
        
        // Update label usage count
        annotationLabelService.updateUsageCount(labelId);
        
        log.info("Annotation instance deleted successfully");
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations) {
        log.info("Batch saving annotation instances: annotationId={}, imageId={}, count={}", annotationId, imageId, annotations.size());
        
        // First delete all existing annotations for this image
        LambdaQueryWrapper<AnnotationInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationInstance::getAnnotationId, annotationId)
               .eq(AnnotationInstance::getImageId, imageId);
        
        List<AnnotationInstance> existingInstances = baseMapper.selectList(wrapper);
        if (!existingInstances.isEmpty()) {
            baseMapper.delete(wrapper);
            
            // Update usage count for deleted annotations' labels
            existingInstances.forEach(instance -> {
                annotationLabelService.updateUsageCount(instance.getLabelId());
            });
        }
        
        // Batch insert new annotations
        for (AnnotationInstance annotation : annotations) {
            annotation.setCreatedTime(LocalDateTime.now());
            annotation.setUpdatedTime(LocalDateTime.now());
            baseMapper.insert(annotation);
            
            // Update label usage count
            annotationLabelService.updateUsageCount(annotation.getLabelId());
        }
        
        log.info("Batch save annotation instances successfully");
        return true;
    }

    @Override
    public List<AnnotationInstance> getByAnnotationId(Long annotationId) {
        log.debug("Query all annotation instances for annotation project: annotationId={}", annotationId);
        return baseMapper.selectByAnnotationId(annotationId);
    }

    @Override
    public Integer countByLabelId(Long labelId) {
        log.debug("Count label usage: labelId={}", labelId);
        return baseMapper.countByLabelId(labelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteImageAndRelatedData(Long annotationId, Long imageId) {
        log.info("Starting to delete image and related data: annotationId={}, imageId={}", annotationId, imageId);

        try {
            // 1. Query all annotation instances for this image
            LambdaQueryWrapper<AnnotationInstance> instanceQuery = new LambdaQueryWrapper<>();
            instanceQuery.eq(AnnotationInstance::getAnnotationId, annotationId)
                        .eq(AnnotationInstance::getImageId, imageId);
            List<AnnotationInstance> instances = baseMapper.selectList(instanceQuery);

            log.info("Found {} annotation instances to delete", instances.size());

            // 2. Count label usage for updating label counts later
            Map<Long, Integer> labelCountMap = new HashMap<>();
            for (AnnotationInstance instance : instances) {
                Long labelId = instance.getLabelId();
                labelCountMap.put(labelId, labelCountMap.getOrDefault(labelId, 0) + 1);
            }

            // 3. Delete all annotation instances
            if (!instances.isEmpty()) {
                List<Long> instanceIds = instances.stream()
                    .map(AnnotationInstance::getId)
                    .collect(Collectors.toList());

                int deletedInstances = baseMapper.deleteBatchIds(instanceIds);
                log.info("Deleted {} annotation instances", deletedInstances);
            }

            // 4. Delete image record (annotation_image table)
            try {
                // Get all images for this annotation project
                List<AnnotationImage> allImages = annotationImageService.getImagesByAnnotationId(annotationId);

                // Find the image record to delete
                AnnotationImage imageToDelete = null;
                for (AnnotationImage image : allImages) {
                    if (imageId.equals(image.getId())) {
                        imageToDelete = image;
                        break;
                    }
                }

                if (imageToDelete != null) {
                    annotationImageService.deleteImage(imageToDelete.getId());
                    log.info("Deleted image record: ID={}, imageId={}", imageToDelete.getId(), imageId);
                } else {
                    log.warn("Image record not found: imageId={}", imageId);
                }
            } catch (Exception e) {
                log.error("Failed to delete image record: imageId={}", imageId, e);
                // Do not throw exception, continue with subsequent logic
            }

            // 5. Update label usage counts (annotation_label table)
            for (Map.Entry<Long, Integer> entry : labelCountMap.entrySet()) {
                Long labelId = entry.getKey();
                Integer count = entry.getValue();

                // Decrease label usage count
                annotationLabelService.updateUsageCount(labelId);
                log.info("Updated usage count for label {}", labelId);
            }

            log.info("Successfully deleted image and related data: imageId={}", imageId);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete image and related data: imageId={}", imageId, e);
            throw new RuntimeException("Failed to delete image and related data: " + e.getMessage());
        }
    }
}