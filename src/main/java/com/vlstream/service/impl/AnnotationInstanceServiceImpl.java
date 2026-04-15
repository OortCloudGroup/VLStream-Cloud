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
 * 标注实例Service实现类
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
        log.debug("查询标注实例: annotationId={}, imageName={}", annotationId, imageName);
        return baseMapper.selectByAnnotationIdAndImageName(annotationId, imageName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, String annotationType, String annotationData) {
        log.info("保存标注实例: annotationId={}, labelId={}, imageId={}", annotationId, labelId, imageId);
        
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
        
        // 更新标签使用次数
        annotationLabelService.updateUsageCount(labelId);
        
        log.info("标注实例保存成功，ID: {}", instance.getId());
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationInstance updateAnnotation(Long instanceId, Long labelId, 
                                             String annotationType, String annotationData) {
        log.info("更新标注实例: instanceId={}, labelId={}", instanceId, labelId);
        
        AnnotationInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("标注实例不存在");
        }

        Long oldLabelId = instance.getLabelId();
        
        instance.setLabelId(labelId);
        instance.setAnnotationType(annotationType);
        instance.setAnnotationData(annotationData);
        instance.setUpdatedTime(LocalDateTime.now());

        baseMapper.updateById(instance);
        
        // 更新标签使用次数
        if (!oldLabelId.equals(labelId)) {
            annotationLabelService.updateUsageCount(oldLabelId);
            annotationLabelService.updateUsageCount(labelId);
        }
        
        log.info("标注实例更新成功");
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotation(Long instanceId) {
        log.info("删除标注实例: instanceId={}", instanceId);
        
        AnnotationInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("标注实例不存在");
        }

        Long labelId = instance.getLabelId();

        int result = baseMapper.deleteById(instanceId);
        
        // 更新标签使用次数
        annotationLabelService.updateUsageCount(labelId);
        
        log.info("标注实例删除成功");
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations) {
        log.info("批量保存标注实例: annotationId={}, imageName={}, count={}", annotationId, imageId, annotations.size());
        
        // 先删除该图片的所有现有标注
        LambdaQueryWrapper<AnnotationInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationInstance::getAnnotationId, annotationId)
               .eq(AnnotationInstance::getImageId, imageId);
        
        List<AnnotationInstance> existingInstances = baseMapper.selectList(wrapper);
        if (!existingInstances.isEmpty()) {
            baseMapper.delete(wrapper);
            
            // 更新被删除标注的标签使用次数
            existingInstances.forEach(instance -> {
                annotationLabelService.updateUsageCount(instance.getLabelId());
            });
        }
        
        // 批量插入新标注
        for (AnnotationInstance annotation : annotations) {
            annotation.setCreatedTime(LocalDateTime.now());
            annotation.setUpdatedTime(LocalDateTime.now());
            baseMapper.insert(annotation);
            
            // 更新标签使用次数
            annotationLabelService.updateUsageCount(annotation.getLabelId());
        }
        
        log.info("批量保存标注实例成功");
        return true;
    }

    @Override
    public List<AnnotationInstance> getByAnnotationId(Long annotationId) {
        log.debug("查询标注项目的所有标注实例: annotationId={}", annotationId);
        return baseMapper.selectByAnnotationId(annotationId);
    }

    @Override
    public Integer countByLabelId(Long labelId) {
        log.debug("统计标签使用次数: labelId={}", labelId);
        return baseMapper.countByLabelId(labelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteImageAndRelatedData(Long annotationId, Long imageId) {
        log.info("开始删除图片及相关数据：annotationId={}, imageName={}", annotationId, imageId);

        try {
            // 1. 查询该图片的所有标注实例
            LambdaQueryWrapper<AnnotationInstance> instanceQuery = new LambdaQueryWrapper<>();
            instanceQuery.eq(AnnotationInstance::getAnnotationId, annotationId)
                        .eq(AnnotationInstance::getImageId, imageId);
            List<AnnotationInstance> instances = baseMapper.selectList(instanceQuery);

            log.info("找到 {} 个标注实例需要删除", instances.size());

            // 2. 统计每个标签的使用次数，用于后续更新标签计数
            Map<Long, Integer> labelCountMap = new HashMap<>();
            for (AnnotationInstance instance : instances) {
                Long labelId = instance.getLabelId();
                labelCountMap.put(labelId, labelCountMap.getOrDefault(labelId, 0) + 1);
            }

            // 3. 删除所有标注实例
            if (!instances.isEmpty()) {
                List<Long> instanceIds = instances.stream()
                    .map(AnnotationInstance::getId)
                    .collect(Collectors.toList());

                int deletedInstances = baseMapper.deleteBatchIds(instanceIds);
                log.info("删除了 {} 个标注实例", deletedInstances);
            }

            // 4. 删除图片记录（annotation_image表）
            try {
                // 获取该标注项目的所有图片
                List<AnnotationImage> allImages = annotationImageService.getImagesByAnnotationId(annotationId);

                // 找到要删除的图片记录
                AnnotationImage imageToDelete = null;
                for (AnnotationImage image : allImages) {
                    if (imageId.equals(image.getId())) {
                        imageToDelete = image;
                        break;
                    }
                }

                if (imageToDelete != null) {
                    annotationImageService.deleteImage(imageToDelete.getId());
                    log.info("删除图片记录: ID={}, imageId={}", imageToDelete.getId(), imageId);
                } else {
                    log.warn("未找到要删除的图片记录: imageId={}", imageId);
                }
            } catch (Exception e) {
                log.error("删除图片记录失败: imageId={}", imageId, e);
                // 不抛出异常，继续执行后续逻辑
            }

            // 5. 更新标签使用计数（annotation_label表）
            for (Map.Entry<Long, Integer> entry : labelCountMap.entrySet()) {
                Long labelId = entry.getKey();
                Integer count = entry.getValue();

                // 减少标签的使用计数
                annotationLabelService.updateUsageCount(labelId);
                log.info("更新标签 {} 的使用计数", labelId);
            }

            log.info("成功删除图片及相关数据：imageId={}", imageId);
            return true;

        } catch (Exception e) {
            log.error("删除图片及相关数据失败：imageId={}", imageId, e);
            throw new RuntimeException("删除图片及相关数据失败：" + e.getMessage());
        }
    }
}