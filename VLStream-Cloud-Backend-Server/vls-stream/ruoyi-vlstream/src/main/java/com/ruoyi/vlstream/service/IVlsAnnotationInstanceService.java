/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.domain.AnnotationInstance;

import java.util.List;

/**
 * Service for frontend-compatible VLS annotation instance APIs.
 */
public interface IVlsAnnotationInstanceService {

    /**
     * Return annotation instances for one image.
     */
    List<AnnotationInstance> getAnnotationInstances(Long annotationId, String imageToken);

    /**
     * Return all annotation instances under an annotation task.
     */
    List<AnnotationInstance> getAllAnnotationInstances(Long annotationId);

    /**
     * Replace annotation instances for one image.
     */
    Boolean batchSaveAnnotationInstances(Long annotationId, String imageToken, List<AnnotationInstance> instances);

    /**
     * Soft delete one annotation instance.
     */
    Boolean deleteAnnotationInstance(Long instanceId);

    /**
     * Soft delete multiple annotation instances.
     */
    Boolean batchDeleteAnnotationInstances(List<Long> instanceIds);

    /**
     * Delete images and related annotation data by image id or image name.
     */
    Boolean deleteAnnotationInstancesByImage(Long annotationId, List<String> imageTokens);
}
