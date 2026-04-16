package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.AnnotationLabel;
import com.vlstream.mapper.AnnotationInstanceMapper;
import com.vlstream.mapper.AnnotationLabelMapper;
import com.vlstream.service.AnnotationLabelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Annotation Label Service Implementation Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class AnnotationLabelServiceImpl extends ServiceImpl<AnnotationLabelMapper, AnnotationLabel> implements AnnotationLabelService {

    @Autowired
    private AnnotationInstanceMapper annotationInstanceMapper;

    @Override
    public List<AnnotationLabel> getByAnnotationIdWithUsageCount(Long annotationId) {
        log.info("Query label list for annotation project[{}]", annotationId);
        return baseMapper.selectByAnnotationIdWithUsageCount(annotationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationLabel createLabel(Long annotationId, String name, String color, String description) {
        log.info("Creating label: annotationId={}, name={}, color={}", annotationId, name, color);
        
        // Check if label with the same name exists in the same annotation project
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationLabel::getAnnotationId, annotationId)
               .eq(AnnotationLabel::getName, name);
        
        AnnotationLabel existingLabel = baseMapper.selectOne(wrapper);
        if (existingLabel != null) {
            throw new RuntimeException("Label name already exists");
        }

        // Get current maximum sort order
        LambdaQueryWrapper<AnnotationLabel> sortWrapper = new LambdaQueryWrapper<>();
        sortWrapper.eq(AnnotationLabel::getAnnotationId, annotationId)
                   .orderByDesc(AnnotationLabel::getSortOrder)
                   .last("LIMIT 1");
        
        AnnotationLabel lastLabel = baseMapper.selectOne(sortWrapper);
        int nextSortOrder = lastLabel != null ? lastLabel.getSortOrder() + 1 : 1;

        // Create new label
        AnnotationLabel label = new AnnotationLabel();
        label.setAnnotationId(annotationId);
        label.setName(name);
        label.setColor(color);
        label.setDescription(description);
        label.setSortOrder(nextSortOrder);
        label.setUsageCount(0);
        label.setCreatedTime(LocalDateTime.now());
        label.setUpdatedTime(LocalDateTime.now());

        baseMapper.insert(label);
        log.info("Label created successfully, ID: {}", label.getId());
        
        return label;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationLabel updateLabel(Long labelId, String name, String color, String description) {
        log.info("Updating label: labelId={}, name={}, color={}", labelId, name, color);
        
        AnnotationLabel label = baseMapper.selectById(labelId);
        if (label == null) {
            throw new RuntimeException("Label does not exist");
        }

        // Check if label with the same name exists in the same annotation project
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationLabel::getAnnotationId, label.getAnnotationId())
               .eq(AnnotationLabel::getName, name)
               .ne(AnnotationLabel::getId, labelId);
        
        AnnotationLabel existingLabel = baseMapper.selectOne(wrapper);
        if (existingLabel != null) {
            throw new RuntimeException("Label name already exists");
        }

        // Update label information
        label.setName(name);
        label.setColor(color);
        label.setDescription(description);
        label.setUpdatedTime(LocalDateTime.now());

        baseMapper.updateById(label);
        log.info("Label updated successfully");
        
        return label;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLabel(Long labelId) {
        log.info("Deleting label: labelId={}", labelId);
        
        AnnotationLabel label = baseMapper.selectById(labelId);
        if (label == null) {
            throw new RuntimeException("Label does not exist");
        }

        // Check if the label is being used by annotation instances
        Integer usageCount = annotationInstanceMapper.countByLabelId(labelId);
        if (usageCount > 0) {
            throw new RuntimeException("Label is in use, cannot delete");
        }

        // Perform soft delete
        int result = baseMapper.deleteById(labelId);
        log.info("Label deleted successfully");
        
        return result > 0;
    }

    @Override
    public boolean updateUsageCount(Long labelId) {
        log.debug("Updating label usage count: labelId={}", labelId);
        
        Integer usageCount = annotationInstanceMapper.countByLabelId(labelId);
        int result = baseMapper.updateUsageCount(labelId, usageCount);
        
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSortOrder(Long annotationId, List<Long> labelIds) {
        log.info("Updating label sort order: annotationId={}, labelIds={}", annotationId, labelIds);
        
        int result = baseMapper.updateSortOrder(annotationId, labelIds);
        
        return result > 0;
    }

    @Override
    public List<AnnotationLabel> searchLabels(Long annotationId, String keyword) {
        log.debug("Searching labels: annotationId={}, keyword={}", annotationId, keyword);
        
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationLabel::getAnnotationId, annotationId);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AnnotationLabel::getName, keyword);
        }
        
        wrapper.orderByAsc(AnnotationLabel::getSortOrder)
               .orderByAsc(AnnotationLabel::getId);

        List<AnnotationLabel> labels = baseMapper.selectList(wrapper);
        
        // Update usage counts
        labels.forEach(label -> {
            Integer usageCount = annotationInstanceMapper.countByLabelId(label.getId());
            label.setUsageCount(usageCount);
        });
        
        return labels;
    }
} 