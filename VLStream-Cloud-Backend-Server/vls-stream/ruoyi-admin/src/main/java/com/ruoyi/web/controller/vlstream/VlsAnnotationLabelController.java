/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnnotationLabel;
import com.ruoyi.vlstream.service.IVlsAnnotationLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Annotation label routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAnnotationLabel")
public class VlsAnnotationLabelController {

    private final IVlsAnnotationLabelService annotationLabelService;

    /**
     * Return labels for an annotation task.
     */
    @GetMapping("/{annotationId}/labels")
    public BladeResult<List<AnnotationLabel>> getLabels(@PathVariable Long annotationId,
                                                        @RequestParam(required = false) String keyword) {
        return BladeResult.success(annotationLabelService.getLabels(annotationId, keyword));
    }

    /**
     * Create a label under an annotation task.
     */
    @PostMapping("/{annotationId}/labels")
    public BladeResult<AnnotationLabel> createLabel(@PathVariable Long annotationId,
                                                    @RequestBody AnnotationLabel annotationLabel) {
        try {
            return BladeResult.success(annotationLabelService.createLabel(annotationId, annotationLabel));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Update a label by id using the frontend path shape.
     */
    @PutMapping("/{id}")
    public BladeResult<AnnotationLabel> updateLabel(@PathVariable Long id,
                                                    @RequestBody AnnotationLabel annotationLabel) {
        try {
            return BladeResult.success(annotationLabelService.updateLabel(id, annotationLabel));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Soft delete a label by id using the frontend path shape.
     */
    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteLabel(@PathVariable Long id) {
        try {
            return BladeResult.success(annotationLabelService.deleteLabel(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }
}
