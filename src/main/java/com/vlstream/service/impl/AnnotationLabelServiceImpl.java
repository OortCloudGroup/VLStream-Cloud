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
 * 标注标签Service实现类
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
        log.info("查询标注项目[{}]的标签列表", annotationId);
        return baseMapper.selectByAnnotationIdWithUsageCount(annotationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationLabel createLabel(Long annotationId, String name, String color, String description) {
        log.info("创建标签: annotationId={}, name={}, color={}", annotationId, name, color);
        
        // 检查同一标注项目下是否存在相同名称的标签
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationLabel::getAnnotationId, annotationId)
               .eq(AnnotationLabel::getName, name);
        
        AnnotationLabel existingLabel = baseMapper.selectOne(wrapper);
        if (existingLabel != null) {
            throw new RuntimeException("标签名称已存在");
        }

        // 获取当前最大排序值
        LambdaQueryWrapper<AnnotationLabel> sortWrapper = new LambdaQueryWrapper<>();
        sortWrapper.eq(AnnotationLabel::getAnnotationId, annotationId)
                   .orderByDesc(AnnotationLabel::getSortOrder)
                   .last("LIMIT 1");
        
        AnnotationLabel lastLabel = baseMapper.selectOne(sortWrapper);
        int nextSortOrder = lastLabel != null ? lastLabel.getSortOrder() + 1 : 1;

        // 创建新标签
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
        log.info("标签创建成功，ID: {}", label.getId());
        
        return label;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationLabel updateLabel(Long labelId, String name, String color, String description) {
        log.info("更新标签: labelId={}, name={}, color={}", labelId, name, color);
        
        AnnotationLabel label = baseMapper.selectById(labelId);
        if (label == null) {
            throw new RuntimeException("标签不存在");
        }

        // 检查同一标注项目下是否存在相同名称的其他标签
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationLabel::getAnnotationId, label.getAnnotationId())
               .eq(AnnotationLabel::getName, name)
               .ne(AnnotationLabel::getId, labelId);
        
        AnnotationLabel existingLabel = baseMapper.selectOne(wrapper);
        if (existingLabel != null) {
            throw new RuntimeException("标签名称已存在");
        }

        // 更新标签信息
        label.setName(name);
        label.setColor(color);
        label.setDescription(description);
        label.setUpdatedTime(LocalDateTime.now());

        baseMapper.updateById(label);
        log.info("标签更新成功");
        
        return label;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLabel(Long labelId) {
        log.info("删除标签: labelId={}", labelId);
        
        AnnotationLabel label = baseMapper.selectById(labelId);
        if (label == null) {
            throw new RuntimeException("标签不存在");
        }

        // 检查是否有标注实例使用该标签
        Integer usageCount = annotationInstanceMapper.countByLabelId(labelId);
        if (usageCount > 0) {
            throw new RuntimeException("该标签已被使用，无法删除");
        }

        // 执行软删除
        int result = baseMapper.deleteById(labelId);
        log.info("标签删除成功");
        
        return result > 0;
    }

    @Override
    public boolean updateUsageCount(Long labelId) {
        log.debug("更新标签使用次数: labelId={}", labelId);
        
        Integer usageCount = annotationInstanceMapper.countByLabelId(labelId);
        int result = baseMapper.updateUsageCount(labelId, usageCount);
        
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSortOrder(Long annotationId, List<Long> labelIds) {
        log.info("更新标签排序: annotationId={}, labelIds={}", annotationId, labelIds);
        
        int result = baseMapper.updateSortOrder(annotationId, labelIds);
        
        return result > 0;
    }

    @Override
    public List<AnnotationLabel> searchLabels(Long annotationId, String keyword) {
        log.debug("搜索标签: annotationId={}, keyword={}", annotationId, keyword);
        
        LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationLabel::getAnnotationId, annotationId);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AnnotationLabel::getName, keyword);
        }
        
        wrapper.orderByAsc(AnnotationLabel::getSortOrder)
               .orderByAsc(AnnotationLabel::getId);

        List<AnnotationLabel> labels = baseMapper.selectList(wrapper);
        
        // 更新使用次数
        labels.forEach(label -> {
            Integer usageCount = annotationInstanceMapper.countByLabelId(label.getId());
            label.setUsageCount(usageCount);
        });
        
        return labels;
    }
} 