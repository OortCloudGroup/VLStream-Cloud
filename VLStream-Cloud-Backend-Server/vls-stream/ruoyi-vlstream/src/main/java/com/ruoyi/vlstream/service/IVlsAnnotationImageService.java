/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.domain.AnnotationImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for frontend annotation image metadata APIs.
 */
public interface IVlsAnnotationImageService {

    /**
     * Return all images for an annotation task.
     */
    List<AnnotationImage> getImagesByDataset(Long annotationId);

    /**
     * Save uploaded image metadata for an annotation task.
     */
    List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId);

    /**
     * Save one annotation image metadata record.
     */
    AnnotationImage saveImage(AnnotationImage annotationImage);

    /**
     * Save multiple annotation image metadata records.
     */
    List<AnnotationImage> batchSaveImages(List<AnnotationImage> annotationImages);
}
