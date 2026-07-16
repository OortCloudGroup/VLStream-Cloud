/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnnotationInstance;
import com.ruoyi.vlstream.mapper.VlsAnnotationInstanceMapper;
import com.ruoyi.vlstream.service.IVlsAnnotationInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;

/**
 * Annotation instance routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsAnnotationInstance")
public class VlsAnnotationInstanceController extends VlsControllerSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IVlsAnnotationInstanceService annotationInstanceService;
    private final VlsAnnotationInstanceMapper annotationInstanceMapper;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsAnnotationInstanceController(IVlsAnnotationInstanceService annotationInstanceService) {
        this.annotationInstanceService = annotationInstanceService;
        this.annotationInstanceMapper = null;
    }

    /**
     * Return instances for one image.
     */
    @GetMapping("/{annotationId}/instances")
    public BladeResult<List<AnnotationInstance>> getAnnotationInstances(@PathVariable Long annotationId,
                                                                        @RequestParam String imageName) {
        return BladeResult.success(annotationInstanceService.getAnnotationInstances(annotationId, imageName));
    }

    /**
     * Return all instances under an annotation task.
     */
    @GetMapping("/{annotationId}/instances/all")
    public BladeResult<List<AnnotationInstance>> getAllAnnotationInstances(@PathVariable Long annotationId) {
        return BladeResult.success(annotationInstanceService.getAllAnnotationInstances(annotationId));
    }

    /**
     * Replace all instances for one image from the frontend batch-save body.
     */
    @PostMapping("/{annotationId}/instances/batch")
    public BladeResult<Boolean> batchSaveAnnotationInstances(@PathVariable Long annotationId,
                                                             @RequestBody Map<String, Object> body) {
        try {
            String imageId = stringify(body == null ? null : body.get("imageId"));
            return BladeResult.success(annotationInstanceService.batchSaveAnnotationInstances(annotationId, imageId, parseInstances(body == null ? null : body.get("instances"))));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Soft delete one annotation instance.
     */
    @DeleteMapping("/instances/{instanceId}")
    public BladeResult<Boolean> deleteAnnotationInstance(@PathVariable Long instanceId) {
        try {
            return BladeResult.success(annotationInstanceService.deleteAnnotationInstance(instanceId));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Soft delete multiple annotation instances.
     */
    @DeleteMapping("/instances/batch")
    public BladeResult<Boolean> batchDeleteAnnotationInstances(@RequestBody List<Long> instanceIds) {
        try {
            return BladeResult.success(annotationInstanceService.batchDeleteAnnotationInstances(instanceIds));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Delete image records and their annotation instances by image id or image name.
     */
    @DeleteMapping("/instances/by-image")
    public BladeResult<Boolean> deleteAnnotationInstancesByImage(@RequestBody Map<String, Object> body) {
        try {
            Long annotationId = toLong(body == null ? null : body.get("annotationId"));
            return BladeResult.success(annotationInstanceService.deleteAnnotationInstancesByImage(annotationId, toStringList(body == null ? null : body.get("imageIds"))));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Return one actual annotation instance. */
    @GetMapping("/detail")
    public BladeResult<AnnotationInstance> detail(@RequestParam Long id) {
        AnnotationInstance instance = annotationInstanceMapper.selectById(id);
        return instance == null ? BladeResult.<AnnotationInstance>fail("Annotation instance does not exist")
            : BladeResult.success(instance);
    }

    /** Page actual annotation instances. */
    @GetMapping("/list")
    public BladeResult<BladePage<AnnotationInstance>> list(@RequestParam(required = false) Long current,
                                                           @RequestParam(required = false) Long size,
                                                           @RequestParam(required = false) Long annotationId,
                                                           @RequestParam(required = false) Long imageId,
                                                           @RequestParam(required = false) Long labelId) {
        Page<AnnotationInstance> page = new Page<AnnotationInstance>(current(current), size(size));
        Page<AnnotationInstance> result = annotationInstanceMapper.selectPage(page,
            instanceQuery(annotationId, imageId, labelId).orderByDesc(AnnotationInstance::getCreateTime));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Return the same actual instance page. */
    @GetMapping("/page")
    public BladeResult<BladePage<AnnotationInstance>> page(@RequestParam(required = false) Long current,
                                                           @RequestParam(required = false) Long size,
                                                           @RequestParam(required = false) Long annotationId,
                                                           @RequestParam(required = false) Long imageId,
                                                           @RequestParam(required = false) Long labelId) {
        return list(current, size, annotationId, imageId, labelId);
    }

    /** Persist one validated annotation instance. */
    @PostMapping("/save")
    public BladeResult<AnnotationInstance> save(@RequestBody AnnotationInstance instance) {
        try {
            normalizeInstance(instance);
            annotationInstanceMapper.insert(instance);
            return BladeResult.success(annotationInstanceMapper.selectById(instance.getId()));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Update one existing annotation instance. */
    @PostMapping("/update")
    public BladeResult<AnnotationInstance> update(@RequestBody AnnotationInstance instance) {
        if (instance == null || instance.getId() == null) {
            return BladeResult.fail("Annotation instance ID is required");
        }
        if (annotationInstanceMapper.selectById(instance.getId()) == null) {
            return BladeResult.fail("Annotation instance does not exist");
        }
        instance.setUpdateTime(new Date());
        annotationInstanceMapper.updateById(instance);
        return BladeResult.success(annotationInstanceMapper.selectById(instance.getId()));
    }

    /** Insert or update an actual annotation instance. */
    @PostMapping("/submit")
    public BladeResult<AnnotationInstance> submit(@RequestBody AnnotationInstance instance) {
        return instance != null && instance.getId() != null ? update(instance) : save(instance);
    }

    /** Create a single instance through the source frontend path. */
    @PostMapping("/{annotationId}/instances")
    public BladeResult<AnnotationInstance> saveForAnnotation(@PathVariable Long annotationId,
                                                             @RequestBody Map<String, Object> body) {
        AnnotationInstance instance = parseInstanceMap(body);
        instance.setAnnotationId(annotationId);
        return save(instance);
    }

    /** Delete actual instances through the SpringBlade remove route. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(annotationInstanceMapper.deleteBatchIds(parsed) > 0);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export actual filtered instance rows. */
    @GetMapping("/export-vlsAnnotationInstance")
    public void exportInstances(@RequestParam(required = false) Long annotationId,
                                @RequestParam(required = false) Long imageId,
                                @RequestParam(required = false) Long labelId,
                                HttpServletResponse response) {
        ExcelUtil.exportExcel(annotationInstanceMapper.selectList(instanceQuery(annotationId, imageId, labelId)),
            "VLS Annotation Instances", AnnotationInstance.class, response);
    }

    /** Build the shared instance query. */
    private LambdaQueryWrapper<AnnotationInstance> instanceQuery(Long annotationId, Long imageId, Long labelId) {
        LambdaQueryWrapper<AnnotationInstance> query = new LambdaQueryWrapper<AnnotationInstance>();
        if (annotationId != null) {
            query.eq(AnnotationInstance::getAnnotationId, annotationId);
        }
        if (imageId != null) {
            query.eq(AnnotationInstance::getImageId, imageId);
        }
        if (labelId != null) {
            query.eq(AnnotationInstance::getLabelId, labelId);
        }
        return query;
    }

    /** Validate and initialize required persistence fields. */
    private void normalizeInstance(AnnotationInstance instance) {
        if (instance == null || instance.getAnnotationId() == null || instance.getImageId() == null
            || instance.getLabelId() == null || instance.getAnnotationType() == null
            || instance.getAnnotationType().trim().isEmpty()) {
            throw new IllegalArgumentException("annotationId, imageId, labelId and annotationType are required");
        }
        Date now = new Date();
        if (instance.getTenantId() == null) {
            instance.setTenantId("000000");
        }
        if (instance.getStatus() == null) {
            instance.setStatus(1);
        }
        if (instance.getIsDeleted() == null) {
            instance.setIsDeleted(0);
        }
        if (instance.getCreateTime() == null) {
            instance.setCreateTime(now);
        }
        instance.setUpdateTime(now);
    }

    private List<AnnotationInstance> parseInstances(Object value) {
        List<AnnotationInstance> instances = new ArrayList<AnnotationInstance>();
        if (!(value instanceof Iterable<?>)) {
            return instances;
        }
        for (Object item : (Iterable<?>) value) {
            if (item instanceof AnnotationInstance) {
                instances.add((AnnotationInstance) item);
            } else if (item instanceof Map<?, ?>) {
                instances.add(parseInstanceMap((Map<?, ?>) item));
            }
        }
        return instances;
    }

    private AnnotationInstance parseInstanceMap(Map<?, ?> item) {
        AnnotationInstance instance = new AnnotationInstance();
        instance.setLabelId(toLong(item.get("labelId")));
        instance.setImageId(toLong(item.get("imageId")));
        instance.setAnnotationType(stringify(item.get("annotationType")));
        instance.setAnnotationData(toJsonString(item.get("annotationData")));
        return instance;
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<String>();
        if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) {
                String stringValue = stringify(item);
                if (stringValue != null) {
                    result.add(stringValue);
                }
            }
        } else {
            String stringValue = stringify(value);
            if (stringValue != null) {
                result.add(stringValue);
            }
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String stringValue = stringify(value);
        if (stringValue != null) {
            try {
                return Long.valueOf(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }

    private String toJsonString(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
