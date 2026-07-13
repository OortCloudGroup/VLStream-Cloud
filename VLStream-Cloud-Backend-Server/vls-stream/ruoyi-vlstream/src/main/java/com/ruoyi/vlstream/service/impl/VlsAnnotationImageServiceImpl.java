/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import com.ruoyi.vlstream.domain.AnnotationImage;
import com.ruoyi.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.service.IVlsAnnotationImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local metadata adapter for VLS annotation images.
 */
@Service
@RequiredArgsConstructor
public class VlsAnnotationImageServiceImpl implements IVlsAnnotationImageService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final VlsAnnotationImageMapper annotationImageMapper;
    private final VlsAlgorithmAnnotationMapper algorithmAnnotationMapper;

    /**
     * Return image metadata sorted like the source endpoint.
     */
    @Override
    public List<AnnotationImage> getImagesByDataset(Long annotationId) {
        if (annotationId == null) {
            return new ArrayList<AnnotationImage>();
        }
        return annotationImageMapper.selectList(new LambdaQueryWrapper<AnnotationImage>()
            .eq(AnnotationImage::getAnnotationId, annotationId)
            .orderByDesc(AnnotationImage::getCreateTime)
            .orderByDesc(AnnotationImage::getId));
    }

    /**
     * Save upload metadata without binding the old SpringBlade file upload service.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId) {
        if (annotationId == null) {
            throw new IllegalArgumentException("Annotation ID cannot be empty");
        }
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }
        List<AnnotationImage> saved = new ArrayList<AnnotationImage>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = sanitizeFileName(file.getOriginalFilename());
            AnnotationImage image = new AnnotationImage();
            image.setAnnotationId(annotationId);
            image.setImageName(originalName);
            image.setOriginalName(originalName);
            image.setLocalPath("/vlsAnnotationImage/local/" + annotationId + "/" + originalName);
            image.setFileSize(file.getSize());
            image.setIsImported(1);
            image.setImportTime(new Date());
            saved.add(insertImage(image));
        }
        incrementAnnotationTotalCounts(countByAnnotation(saved));
        return saved;
    }

    /**
     * Save one image metadata record and return it with generated id.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnotationImage saveImage(AnnotationImage annotationImage) {
        AnnotationImage saved = insertImage(annotationImage);
        incrementAnnotationTotalCounts(countByAnnotation(java.util.Collections.singletonList(saved)));
        return saved;
    }

    /**
     * Save multiple image metadata records and return generated ids for frontend backfill.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AnnotationImage> batchSaveImages(List<AnnotationImage> annotationImages) {
        List<AnnotationImage> saved = new ArrayList<AnnotationImage>();
        if (annotationImages == null) {
            return saved;
        }
        for (AnnotationImage image : annotationImages) {
            if (image != null) {
                saved.add(insertImage(image));
            }
        }
        incrementAnnotationTotalCounts(countByAnnotation(saved));
        return saved;
    }

    private AnnotationImage insertImage(AnnotationImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image metadata cannot be empty");
        }
        if (image.getAnnotationId() == null) {
            throw new IllegalArgumentException("Annotation ID cannot be empty");
        }
        if (!StringUtils.hasText(image.getImageName())) {
            throw new IllegalArgumentException("Image name cannot be empty");
        }
        normalizeDefaults(image);
        annotationImageMapper.insert(image);
        AnnotationImage saved = annotationImageMapper.selectById(image.getId());
        return saved == null ? image : saved;
    }

    private void normalizeDefaults(AnnotationImage image) {
        if (!StringUtils.hasText(image.getTenantId())) {
            image.setTenantId(DEFAULT_TENANT_ID);
        }
        image.setImageName(sanitizeFileName(image.getImageName()));
        if (!StringUtils.hasText(image.getOriginalName())) {
            image.setOriginalName(image.getImageName());
        } else {
            image.setOriginalName(sanitizeFileName(image.getOriginalName()));
        }
        if (!StringUtils.hasText(image.getLocalPath())) {
            image.setLocalPath("/vlsAnnotationImage/local/" + image.getAnnotationId() + "/" + image.getImageName());
        }
        if (image.getIsImported() == null) {
            image.setIsImported(1);
        }
        if (image.getImportTime() == null) {
            image.setImportTime(new Date());
        }
        if (image.getStatus() == null) {
            image.setStatus(1);
        }
        if (image.getIsDeleted() == null) {
            image.setIsDeleted(0);
        }
        if (image.getCreateTime() == null) {
            image.setCreateTime(new Date());
        }
        image.setUpdateTime(new Date());
    }

    private Map<Long, Integer> countByAnnotation(List<AnnotationImage> images) {
        Map<Long, Integer> counts = new LinkedHashMap<Long, Integer>();
        for (AnnotationImage image : images) {
            Long annotationId = image.getAnnotationId();
            if (annotationId != null) {
                counts.put(annotationId, counts.containsKey(annotationId) ? counts.get(annotationId) + 1 : 1);
            }
        }
        return counts;
    }

    private void incrementAnnotationTotalCounts(Map<Long, Integer> counts) {
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            AlgorithmAnnotation annotation = algorithmAnnotationMapper.selectById(entry.getKey());
            if (annotation != null) {
                int currentTotal = annotation.getTotalCount() == null ? 0 : annotation.getTotalCount();
                AlgorithmAnnotation update = new AlgorithmAnnotation();
                update.setId(annotation.getId());
                update.setTotalCount(currentTotal + entry.getValue());
                update.setProgress(calculateProgress(annotation.getAnnotatedCount(), currentTotal + entry.getValue()));
                update.setUpdateTime(new Date());
                algorithmAnnotationMapper.updateById(update);
            }
        }
    }

    private int calculateProgress(Integer annotatedCount, Integer totalCount) {
        int total = totalCount == null ? 0 : totalCount;
        if (total <= 0) {
            return 0;
        }
        int annotated = annotatedCount == null ? 0 : Math.max(0, annotatedCount);
        return Math.min(100, (annotated * 100) / total);
    }

    private String sanitizeFileName(String value) {
        String fileName = StringUtils.hasText(value) ? Paths.get(value).getFileName().toString() : "image";
        return fileName.replace("\\", "_").replace("/", "_").replaceAll("[<>:\"|?*]", "_");
    }
}
