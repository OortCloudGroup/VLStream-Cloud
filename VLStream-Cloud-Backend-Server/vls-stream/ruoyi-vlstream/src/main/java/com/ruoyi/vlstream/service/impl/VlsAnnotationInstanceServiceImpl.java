package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import com.ruoyi.vlstream.domain.AnnotationImage;
import com.ruoyi.vlstream.domain.AnnotationInstance;
import com.ruoyi.vlstream.domain.AnnotationLabel;
import com.ruoyi.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.mapper.VlsAnnotationInstanceMapper;
import com.ruoyi.vlstream.mapper.VlsAnnotationLabelMapper;
import com.ruoyi.vlstream.service.IVlsAnnotationInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Local annotation instance service for VLStream-Web compatibility.
 */
@Service
@RequiredArgsConstructor
public class VlsAnnotationInstanceServiceImpl implements IVlsAnnotationInstanceService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final VlsAnnotationInstanceMapper annotationInstanceMapper;
    private final VlsAnnotationImageMapper annotationImageMapper;
    private final VlsAnnotationLabelMapper annotationLabelMapper;
    private final VlsAlgorithmAnnotationMapper algorithmAnnotationMapper;

    /**
     * Return instances by image id or image name.
     */
    @Override
    public List<AnnotationInstance> getAnnotationInstances(Long annotationId, String imageToken) {
        if (annotationId == null || !StringUtils.hasText(imageToken)) {
            return new ArrayList<AnnotationInstance>();
        }
        return annotationInstanceMapper.selectByAnnotationIdAndImageToken(annotationId, imageToken.trim());
    }

    /**
     * Return all active instances for the annotation task.
     */
    @Override
    public List<AnnotationInstance> getAllAnnotationInstances(Long annotationId) {
        if (annotationId == null) {
            return new ArrayList<AnnotationInstance>();
        }
        return annotationInstanceMapper.selectByAnnotationId(annotationId);
    }

    /**
     * Replace all active instances for the image with the supplied frontend list.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSaveAnnotationInstances(Long annotationId, String imageToken, List<AnnotationInstance> instances) {
        Long imageId = resolveImageId(annotationId, imageToken);
        Set<Long> labelsToRefresh = new LinkedHashSet<Long>();
        List<AnnotationInstance> existing = annotationInstanceMapper.selectList(new LambdaQueryWrapper<AnnotationInstance>()
            .eq(AnnotationInstance::getAnnotationId, annotationId)
            .eq(AnnotationInstance::getImageId, imageId));
        for (AnnotationInstance instance : existing) {
            if (instance.getLabelId() != null) {
                labelsToRefresh.add(instance.getLabelId());
            }
        }
        if (!existing.isEmpty()) {
            annotationInstanceMapper.delete(new LambdaQueryWrapper<AnnotationInstance>()
                .eq(AnnotationInstance::getAnnotationId, annotationId)
                .eq(AnnotationInstance::getImageId, imageId));
        }

        if (instances != null) {
            for (AnnotationInstance request : instances) {
                AnnotationInstance instance = copyForInsert(annotationId, imageId, request);
                annotationInstanceMapper.insert(instance);
                if (instance.getLabelId() != null) {
                    labelsToRefresh.add(instance.getLabelId());
                }
            }
        }

        refreshLabelUsageCounts(labelsToRefresh);
        refreshAnnotationProgress(annotationId);
        return true;
    }

    /**
     * Soft delete one annotation instance and refresh derived counts.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAnnotationInstance(Long instanceId) {
        if (instanceId == null) {
            return false;
        }
        AnnotationInstance instance = annotationInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return false;
        }
        boolean deleted = annotationInstanceMapper.deleteById(instanceId) > 0;
        refreshLabelUsageCounts(singletonLabel(instance.getLabelId()));
        refreshAnnotationProgress(instance.getAnnotationId());
        return deleted;
    }

    /**
     * Soft delete multiple annotation instances.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteAnnotationInstances(List<Long> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return false;
        }
        Set<Long> labelsToRefresh = new LinkedHashSet<Long>();
        Set<Long> annotationsToRefresh = new LinkedHashSet<Long>();
        List<AnnotationInstance> existing = annotationInstanceMapper.selectBatchIds(instanceIds);
        for (AnnotationInstance instance : existing) {
            if (instance.getLabelId() != null) {
                labelsToRefresh.add(instance.getLabelId());
            }
            if (instance.getAnnotationId() != null) {
                annotationsToRefresh.add(instance.getAnnotationId());
            }
        }
        annotationInstanceMapper.deleteBatchIds(instanceIds);
        refreshLabelUsageCounts(labelsToRefresh);
        for (Long annotationId : annotationsToRefresh) {
            refreshAnnotationProgress(annotationId);
        }
        return true;
    }

    /**
     * Delete annotation instances and image metadata by image id or image name.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAnnotationInstancesByImage(Long annotationId, List<String> imageTokens) {
        if (annotationId == null || imageTokens == null || imageTokens.isEmpty()) {
            return false;
        }
        Set<Long> labelsToRefresh = new LinkedHashSet<Long>();
        for (String imageToken : imageTokens) {
            Long imageId = resolveImageId(annotationId, imageToken);
            List<AnnotationInstance> instances = annotationInstanceMapper.selectList(new LambdaQueryWrapper<AnnotationInstance>()
                .eq(AnnotationInstance::getAnnotationId, annotationId)
                .eq(AnnotationInstance::getImageId, imageId));
            for (AnnotationInstance instance : instances) {
                if (instance.getLabelId() != null) {
                    labelsToRefresh.add(instance.getLabelId());
                }
            }
            if (!instances.isEmpty()) {
                annotationInstanceMapper.delete(new LambdaQueryWrapper<AnnotationInstance>()
                    .eq(AnnotationInstance::getAnnotationId, annotationId)
                    .eq(AnnotationInstance::getImageId, imageId));
            }
            annotationImageMapper.deleteById(imageId);
        }
        refreshLabelUsageCounts(labelsToRefresh);
        refreshAnnotationProgress(annotationId);
        return true;
    }

    private AnnotationInstance copyForInsert(Long annotationId, Long imageId, AnnotationInstance request) {
        if (request == null) {
            throw new IllegalArgumentException("Annotation instance cannot be empty");
        }
        if (request.getLabelId() == null) {
            throw new IllegalArgumentException("labelId cannot be empty");
        }
        if (!StringUtils.hasText(request.getAnnotationType())) {
            throw new IllegalArgumentException("annotationType cannot be empty");
        }
        AnnotationInstance instance = new AnnotationInstance();
        instance.setTenantId(DEFAULT_TENANT_ID);
        instance.setAnnotationId(annotationId);
        instance.setLabelId(request.getLabelId());
        instance.setImageId(imageId);
        instance.setAnnotationType(request.getAnnotationType().trim());
        instance.setAnnotationData(request.getAnnotationData());
        instance.setConfidence(request.getConfidence() == null ? new BigDecimal("1.0000") : request.getConfidence());
        instance.setVerified(request.getVerified() == null ? 0 : request.getVerified());
        instance.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        instance.setIsDeleted(0);
        instance.setCreateTime(new Date());
        instance.setUpdateTime(new Date());
        return instance;
    }

    private Long resolveImageId(Long annotationId, String imageToken) {
        if (annotationId == null || !StringUtils.hasText(imageToken)) {
            throw new IllegalArgumentException("imageId cannot be empty");
        }
        String value = imageToken.trim();
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            AnnotationImage image = annotationImageMapper.selectOne(new LambdaQueryWrapper<AnnotationImage>()
                .eq(AnnotationImage::getAnnotationId, annotationId)
                .and(wrapper -> wrapper.eq(AnnotationImage::getImageName, value).or().eq(AnnotationImage::getOriginalName, value))
                .last("LIMIT 1"));
            if (image != null && image.getId() != null) {
                return image.getId();
            }
            throw new IllegalArgumentException("Image does not exist: " + value);
        }
    }

    private Set<Long> singletonLabel(Long labelId) {
        Set<Long> labels = new LinkedHashSet<Long>();
        if (labelId != null) {
            labels.add(labelId);
        }
        return labels;
    }

    private void refreshLabelUsageCounts(Set<Long> labelIds) {
        if (labelIds == null) {
            return;
        }
        for (Long labelId : labelIds) {
            if (labelId == null) {
                continue;
            }
            AnnotationLabel update = new AnnotationLabel();
            update.setId(labelId);
            Integer count = annotationInstanceMapper.countByLabelId(labelId);
            update.setUsageCount(count == null ? 0 : count);
            update.setUpdateTime(new Date());
            annotationLabelMapper.updateById(update);
        }
    }

    private void refreshAnnotationProgress(Long annotationId) {
        if (annotationId == null) {
            return;
        }
        AlgorithmAnnotation annotation = algorithmAnnotationMapper.selectById(annotationId);
        if (annotation == null) {
            return;
        }
        int annotated = safe(annotationInstanceMapper.countAnnotatedImages(annotationId));
        int total = annotation.getTotalCount() == null ? annotated : Math.max(annotation.getTotalCount(), annotated);
        AlgorithmAnnotation update = new AlgorithmAnnotation();
        update.setId(annotationId);
        update.setAnnotatedCount(annotated);
        update.setTotalCount(total);
        int progress = calculateProgress(annotated, total);
        update.setProgress(progress);
        update.setAnnotationStatus(calculateStatus(progress));
        update.setUpdateTime(new Date());
        algorithmAnnotationMapper.updateById(update);
    }

    private int safe(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int calculateProgress(int annotated, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, (annotated * 100) / total);
    }

    private String calculateStatus(int progress) {
        if (progress <= 0) {
            return "none";
        }
        return progress >= 100 ? "completed" : "partial";
    }
}
