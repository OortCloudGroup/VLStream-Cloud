/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.domain.AnnotationImage;
import com.ruoyi.vlstream.service.IVlsAnnotationImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Annotation image routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAnnotationImage")
public class VlsAnnotationImageController {

    private final IVlsAnnotationImageService annotationImageService;

    /**
     * Return all images for an annotation task.
     */
    @GetMapping("/dataset/{annotationId}")
    public Map<String, Object> getImagesByDataset(@PathVariable Long annotationId) {
        return success(annotationImageService.getImagesByDataset(annotationId));
    }

    /**
     * Upload image files and save metadata records.
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadImages(@RequestPart("files") MultipartFile[] files,
                                            @RequestParam("annotationId") Long annotationId) {
        try {
            return success("Image uploaded successfully", annotationImageService.uploadImages(files, annotationId));
        } catch (RuntimeException ex) {
            return fail("Image upload failed:" + ex.getMessage());
        }
    }

    /**
     * Save a single image metadata record.
     */
    @PostMapping("/images")
    public Map<String, Object> saveImage(@RequestBody AnnotationImage annotationImage) {
        try {
            return success("Image info saved successfully", annotationImageService.saveImage(annotationImage));
        } catch (RuntimeException ex) {
            return fail("Failed to save image info:" + ex.getMessage());
        }
    }

    /**
     * Save multiple image metadata records.
     */
    @PostMapping("/images/batch")
    public Map<String, Object> batchSaveImages(@RequestBody List<AnnotationImage> annotationImages) {
        try {
            return success("Bulk saved image info successfully", annotationImageService.batchSaveImages(annotationImages));
        } catch (RuntimeException ex) {
            return fail("Failed to bulk save image info:" + ex.getMessage());
        }
    }

    private Map<String, Object> success(Object data) {
        return success(null, data);
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("success", true);
        if (message != null) {
            response.put("message", message);
        }
        response.put("data", data);
        return response;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
