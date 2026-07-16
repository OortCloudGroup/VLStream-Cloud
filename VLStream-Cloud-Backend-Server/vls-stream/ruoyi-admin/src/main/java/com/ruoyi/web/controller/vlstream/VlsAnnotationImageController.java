/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnnotationImage;
import com.ruoyi.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.service.IVlsAnnotationImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import java.util.Date;
import javax.servlet.http.HttpServletResponse;

/**
 * Annotation image routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsAnnotationImage")
public class VlsAnnotationImageController extends VlsControllerSupport {

    private final IVlsAnnotationImageService annotationImageService;
    private final VlsAnnotationImageMapper annotationImageMapper;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsAnnotationImageController(IVlsAnnotationImageService annotationImageService) {
        this.annotationImageService = annotationImageService;
        this.annotationImageMapper = null;
    }

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

    /** Return one stored image through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<AnnotationImage> detail(@RequestParam Long id) {
        return imageResult(annotationImageMapper.selectById(id));
    }

    /** Page actual image rows with common source filters. */
    @GetMapping("/list")
    public BladeResult<BladePage<AnnotationImage>> list(@RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) Long annotationId,
                                                        @RequestParam(required = false) String imageName) {
        Page<AnnotationImage> page = new Page<AnnotationImage>(current(current), size(size));
        Page<AnnotationImage> result = annotationImageMapper.selectPage(page, imageQuery(annotationId, imageName)
            .orderByDesc(AnnotationImage::getCreateTime));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Return the same real image page through the custom page route. */
    @GetMapping("/page")
    public BladeResult<BladePage<AnnotationImage>> page(@RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) Long annotationId,
                                                        @RequestParam(required = false) String imageName) {
        return list(current, size, annotationId, imageName);
    }

    /** Load one image by its source-compatible path. */
    @GetMapping("/{id}")
    public BladeResult<AnnotationImage> getImage(@PathVariable Long id) {
        return imageResult(annotationImageMapper.selectById(id));
    }

    /** Save image metadata using the validated image service. */
    @PostMapping("/save")
    public BladeResult<AnnotationImage> save(@RequestBody AnnotationImage image) {
        try {
            return BladeResult.success(annotationImageService.saveImage(image));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Update an existing image row. */
    @PostMapping("/update")
    public BladeResult<AnnotationImage> update(@RequestBody AnnotationImage image) {
        return image == null || image.getId() == null ? BladeResult.<AnnotationImage>fail("Image ID is required")
            : updateImage(image.getId(), image);
    }

    /** Insert or update actual image metadata. */
    @PostMapping("/submit")
    public BladeResult<AnnotationImage> submit(@RequestBody AnnotationImage image) {
        return image != null && image.getId() != null ? update(image) : save(image);
    }

    /** Update an existing image using the frontend path shape. */
    @PutMapping("/{id}")
    public BladeResult<AnnotationImage> updateImage(@PathVariable Long id, @RequestBody AnnotationImage image) {
        AnnotationImage existing = annotationImageMapper.selectById(id);
        if (existing == null) {
            return BladeResult.fail("Image does not exist");
        }
        image.setId(id);
        image.setUpdateTime(new Date());
        annotationImageMapper.updateById(image);
        return imageResult(annotationImageMapper.selectById(id));
    }

    /** Delete one stored image row. */
    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteImage(@PathVariable Long id) {
        return BladeResult.success(annotationImageMapper.deleteById(id) > 0);
    }

    /** Delete multiple stored image rows. */
    @DeleteMapping("/batch")
    public BladeResult<Boolean> deleteImages(@RequestBody List<Long> ids) {
        return BladeResult.success(ids != null && !ids.isEmpty() && annotationImageMapper.deleteBatchIds(ids) > 0);
    }

    /** Delete stored image rows through the SpringBlade remove route. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(annotationImageMapper.deleteBatchIds(parsed) > 0);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Calculate statistics from actual image rows. */
    @GetMapping("/dataset/{datasetId}/stats")
    public BladeResult<Map<String, Object>> datasetStats(@PathVariable Long datasetId) {
        List<AnnotationImage> images = annotationImageService.getImagesByDataset(datasetId);
        long imported = 0L;
        long totalSize = 0L;
        for (AnnotationImage image : images) {
            imported += Integer.valueOf(1).equals(image.getIsImported()) ? 1L : 0L;
            totalSize += image.getFileSize() == null ? 0L : image.getFileSize();
        }
        Map<String, Object> importStats = new LinkedHashMap<String, Object>();
        importStats.put("IMPORTED", imported);
        importStats.put("NOT_IMPORTED", images.size() - imported);
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("totalImages", images.size());
        stats.put("importStats", importStats);
        stats.put("totalSize", totalSize);
        return BladeResult.success(stats);
    }

    /** Export actual filtered annotation images. */
    @GetMapping("/export-vlsAnnotationImage")
    public void exportImages(@RequestParam(required = false) Long annotationId,
                             @RequestParam(required = false) String imageName,
                             HttpServletResponse response) {
        ExcelUtil.exportExcel(annotationImageMapper.selectList(imageQuery(annotationId, imageName)),
            "VLS Annotation Images", AnnotationImage.class, response);
    }

    /** Build the shared image query. */
    private LambdaQueryWrapper<AnnotationImage> imageQuery(Long annotationId, String imageName) {
        LambdaQueryWrapper<AnnotationImage> query = new LambdaQueryWrapper<AnnotationImage>();
        if (annotationId != null) {
            query.eq(AnnotationImage::getAnnotationId, annotationId);
        }
        if (imageName != null && !imageName.trim().isEmpty()) {
            query.like(AnnotationImage::getImageName, imageName.trim());
        }
        return query;
    }

    /** Convert a nullable image row into an explicit API result. */
    private BladeResult<AnnotationImage> imageResult(AnnotationImage image) {
        return image == null ? BladeResult.<AnnotationImage>fail("Image does not exist") : BladeResult.success(image);
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
