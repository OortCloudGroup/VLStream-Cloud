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
import com.ruoyi.vlstream.domain.AnnotationLabel;
import com.ruoyi.vlstream.mapper.VlsAnnotationLabelMapper;
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
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

/**
 * Annotation label routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsAnnotationLabel")
public class VlsAnnotationLabelController extends VlsControllerSupport {

    private final IVlsAnnotationLabelService annotationLabelService;
    private final VlsAnnotationLabelMapper annotationLabelMapper;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsAnnotationLabelController(IVlsAnnotationLabelService annotationLabelService) {
        this.annotationLabelService = annotationLabelService;
        this.annotationLabelMapper = null;
    }

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

    /** Return one persisted label. */
    @GetMapping("/detail")
    public BladeResult<AnnotationLabel> detail(@RequestParam Long id) {
        AnnotationLabel label = annotationLabelMapper.selectById(id);
        return label == null ? BladeResult.<AnnotationLabel>fail("Label does not exist") : BladeResult.success(label);
    }

    /** Page persisted labels. */
    @GetMapping("/list")
    public BladeResult<BladePage<AnnotationLabel>> list(@RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) Long annotationId,
                                                        @RequestParam(required = false) String keyword) {
        Page<AnnotationLabel> page = new Page<AnnotationLabel>(current(current), size(size));
        Page<AnnotationLabel> result = annotationLabelMapper.selectPage(page, labelQuery(annotationId, keyword)
            .orderByAsc(AnnotationLabel::getSortOrder));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Return the same actual label page. */
    @GetMapping("/page")
    public BladeResult<BladePage<AnnotationLabel>> page(@RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) Long annotationId,
                                                        @RequestParam(required = false) String keyword) {
        return list(current, size, annotationId, keyword);
    }

    /** Create a label through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<AnnotationLabel> save(@RequestBody AnnotationLabel label) {
        if (label == null || label.getAnnotationId() == null) {
            return BladeResult.fail("annotationId is required");
        }
        return createLabel(label.getAnnotationId(), label);
    }

    /** Update a label through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<AnnotationLabel> update(@RequestBody AnnotationLabel label) {
        return label == null || label.getId() == null ? BladeResult.<AnnotationLabel>fail("Label ID is required")
            : updateLabel(label.getId(), label);
    }

    /** Insert or update a label. */
    @PostMapping("/submit")
    public BladeResult<AnnotationLabel> submit(@RequestBody AnnotationLabel label) {
        return label != null && label.getId() != null ? update(label) : save(label);
    }

    /** Update via the source label path. */
    @PutMapping("/labels/{labelId}")
    public BladeResult<AnnotationLabel> updateLabelCompat(@PathVariable Long labelId,
                                                           @RequestBody AnnotationLabel label) {
        return updateLabel(labelId, label);
    }

    /** Delete via the source label path. */
    @DeleteMapping("/labels/{labelId}")
    public BladeResult<Boolean> deleteLabelCompat(@PathVariable Long labelId) {
        return deleteLabel(labelId);
    }

    /** Persist the requested label order for one annotation task. */
    @PutMapping("/{annotationId}/labels/sort")
    public BladeResult<Boolean> sortLabels(@PathVariable Long annotationId, @RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("labelIds");
        if (!(raw instanceof Iterable<?>)) {
            return BladeResult.fail("labelIds is required");
        }
        int order = 0;
        for (Object item : (Iterable<?>) raw) {
            Long id = item instanceof Number ? ((Number) item).longValue() : Long.valueOf(String.valueOf(item));
            AnnotationLabel label = annotationLabelMapper.selectById(id);
            if (label == null || !annotationId.equals(label.getAnnotationId())) {
                return BladeResult.fail("Label does not belong to annotation: " + id);
            }
            label.setSortOrder(order++);
            annotationLabelMapper.updateById(label);
        }
        return BladeResult.success(true);
    }

    /** Delete labels through the SpringBlade remove route. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(annotationLabelMapper.deleteBatchIds(parsed) > 0);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export actual label rows. */
    @GetMapping("/export-vlsAnnotationLabel")
    public void exportLabels(@RequestParam(required = false) Long annotationId,
                             @RequestParam(required = false) String keyword,
                             HttpServletResponse response) {
        ExcelUtil.exportExcel(annotationLabelMapper.selectList(labelQuery(annotationId, keyword)),
            "VLS Annotation Labels", AnnotationLabel.class, response);
    }

    /** Build the shared label query. */
    private LambdaQueryWrapper<AnnotationLabel> labelQuery(Long annotationId, String keyword) {
        LambdaQueryWrapper<AnnotationLabel> query = new LambdaQueryWrapper<AnnotationLabel>();
        if (annotationId != null) {
            query.eq(AnnotationLabel::getAnnotationId, annotationId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(AnnotationLabel::getName, keyword.trim());
        }
        return query;
    }
}
