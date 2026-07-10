package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.domain.AnnotationLabel;
import com.ruoyi.vlstream.mapper.VlsAnnotationLabelMapper;
import com.ruoyi.vlstream.service.IVlsAnnotationLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Local annotation label service for VLStream-Web compatibility.
 */
@Service
@RequiredArgsConstructor
public class VlsAnnotationLabelServiceImpl implements IVlsAnnotationLabelService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final VlsAnnotationLabelMapper annotationLabelMapper;

    /**
     * Return labels in frontend display order.
     */
    @Override
    public List<AnnotationLabel> getLabels(Long annotationId, String keyword) {
        if (annotationId == null) {
            return new ArrayList<AnnotationLabel>();
        }
        return annotationLabelMapper.selectByAnnotationIdWithUsageCount(annotationId, normalize(keyword));
    }

    /**
     * Create a label and assign the next sort order under the annotation task.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationLabel createLabel(Long annotationId, AnnotationLabel request) {
        if (annotationId == null) {
            throw new IllegalArgumentException("Annotation ID cannot be empty");
        }
        AnnotationLabel label = copyRequest(request);
        label.setAnnotationId(annotationId);
        validateLabel(label);
        assertDuplicateNameNotExists(annotationId, label.getName(), null);
        if (label.getSortOrder() == null) {
            label.setSortOrder(nextSortOrder(annotationId));
        }
        normalizeDefaults(label);
        annotationLabelMapper.insert(label);
        AnnotationLabel saved = annotationLabelMapper.selectById(label.getId());
        return saved == null ? label : saved;
    }

    /**
     * Update the frontend-editable label fields.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationLabel updateLabel(Long id, AnnotationLabel request) {
        AnnotationLabel current = annotationLabelMapper.selectById(id);
        if (current == null) {
            throw new IllegalArgumentException("Label does not exist");
        }
        AnnotationLabel update = copyRequest(request);
        update.setId(id);
        update.setAnnotationId(current.getAnnotationId());
        validateLabel(update);
        assertDuplicateNameNotExists(current.getAnnotationId(), update.getName(), id);
        current.setName(update.getName());
        current.setColor(update.getColor());
        current.setDescription(update.getDescription());
        if (update.getSortOrder() != null) {
            current.setSortOrder(update.getSortOrder());
        }
        if (update.getStatus() != null) {
            current.setStatus(update.getStatus());
        }
        current.setUpdateTime(new Date());
        annotationLabelMapper.updateById(current);
        AnnotationLabel saved = annotationLabelMapper.selectById(id);
        return saved == null ? current : saved;
    }

    /**
     * Soft delete an unused label.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteLabel(Long id) {
        if (id == null) {
            return false;
        }
        AnnotationLabel current = annotationLabelMapper.selectById(id);
        if (current == null) {
            return false;
        }
        Integer usageCount = annotationLabelMapper.countActiveInstancesByLabelId(id);
        if (usageCount != null && usageCount > 0) {
            throw new IllegalStateException("The label is in use and cannot be deleted");
        }
        return annotationLabelMapper.deleteById(id) > 0;
    }

    private AnnotationLabel copyRequest(AnnotationLabel request) {
        if (request == null) {
            throw new IllegalArgumentException("Label cannot be empty");
        }
        AnnotationLabel label = new AnnotationLabel();
        label.setName(normalize(request.getName()));
        label.setColor(normalize(request.getColor()));
        label.setDescription(request.getDescription());
        label.setSortOrder(request.getSortOrder());
        label.setUsageCount(request.getUsageCount());
        label.setStatus(request.getStatus());
        return label;
    }

    private void validateLabel(AnnotationLabel label) {
        if (!StringUtils.hasText(label.getName())) {
            throw new IllegalArgumentException("Label name cannot be empty");
        }
        if (!StringUtils.hasText(label.getColor())) {
            throw new IllegalArgumentException("Label color cannot be empty");
        }
    }

    private void assertDuplicateNameNotExists(Long annotationId, String name, Long excludeId) {
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<AnnotationLabel>()
            .eq(AnnotationLabel::getAnnotationId, annotationId)
            .eq(AnnotationLabel::getName, name);
        if (excludeId != null) {
            wrapper.ne(AnnotationLabel::getId, excludeId);
        }
        Long count = annotationLabelMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Label name already exists");
        }
    }

    private Integer nextSortOrder(Long annotationId) {
        AnnotationLabel last = annotationLabelMapper.selectOne(new LambdaQueryWrapper<AnnotationLabel>()
            .eq(AnnotationLabel::getAnnotationId, annotationId)
            .orderByDesc(AnnotationLabel::getSortOrder)
            .orderByDesc(AnnotationLabel::getId)
            .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private void normalizeDefaults(AnnotationLabel label) {
        if (!StringUtils.hasText(label.getTenantId())) {
            label.setTenantId(DEFAULT_TENANT_ID);
        }
        if (label.getUsageCount() == null) {
            label.setUsageCount(0);
        }
        if (label.getStatus() == null) {
            label.setStatus(1);
        }
        if (label.getIsDeleted() == null) {
            label.setIsDeleted(0);
        }
        if (label.getCreateTime() == null) {
            label.setCreateTime(new Date());
        }
        label.setUpdateTime(new Date());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
