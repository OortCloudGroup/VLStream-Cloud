package com.ruoyi.web.controller.vlstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnnotationInstance;
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

/**
 * Annotation instance routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAnnotationInstance")
public class VlsAnnotationInstanceController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IVlsAnnotationInstanceService annotationInstanceService;

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
