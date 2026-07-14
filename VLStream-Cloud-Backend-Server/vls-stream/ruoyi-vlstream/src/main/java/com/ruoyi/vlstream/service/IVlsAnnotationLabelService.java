/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.domain.AnnotationLabel;

import java.util.List;

/**
 * Service for frontend-compatible VLS annotation label APIs.
 */
public interface IVlsAnnotationLabelService {

    /**
     * Return labels for an annotation task, optionally filtered by keyword.
     */
    List<AnnotationLabel> getLabels(Long annotationId, String keyword);

    /**
     * Create a label under an annotation task.
     */
    AnnotationLabel createLabel(Long annotationId, AnnotationLabel request);

    /**
     * Update a label by id.
     */
    AnnotationLabel updateLabel(Long id, AnnotationLabel request);

    /**
     * Soft delete a label by id.
     */
    Boolean deleteLabel(Long id);
}
