/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import com.ruoyi.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.service.IVlsAlgorithmAnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service for the VLS algorithm annotation frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsAlgorithmAnnotationServiceImpl implements IVlsAlgorithmAnnotationService {

    private static final String STATUS_NONE = "none";
    private static final String STATUS_PARTIAL = "partial";
    private static final String STATUS_COMPLETED = "completed";

    private final VlsAlgorithmAnnotationMapper annotationMapper;

    /**
     * Return a filtered annotation page using frontend query names.
     */
    @Override
    public BladePage<AlgorithmAnnotation> getAnnotationPage(Long current, Long size, String annotationName,
                                                            String annotationType, String annotationStatus,
                                                            String startTime, String endTime) {
        Page<AlgorithmAnnotation> page = new Page<AlgorithmAnnotation>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<AlgorithmAnnotation> wrapper = buildQuery(annotationName, annotationType, annotationStatus);
        Date begin = parseDate(startTime, false);
        if (begin != null) {
            wrapper.ge(AlgorithmAnnotation::getCreateTime, begin);
        }
        Date end = parseDate(endTime, true);
        if (end != null) {
            wrapper.le(AlgorithmAnnotation::getCreateTime, end);
        }
        wrapper.orderByDesc(AlgorithmAnnotation::getCreateTime).orderByDesc(AlgorithmAnnotation::getId);
        Page<AlgorithmAnnotation> result = annotationMapper.selectPage(page, wrapper);
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    /**
     * Return an annotation by id.
     */
    @Override
    public AlgorithmAnnotation getAnnotationById(Long id) {
        return annotationMapper.selectById(id);
    }

    /**
     * Create an annotation after applying default counters and status.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmAnnotation createAnnotation(AlgorithmAnnotation annotation) {
        if (annotation == null || !StringUtils.hasText(annotation.getAnnotationName())) {
            throw new IllegalArgumentException("Annotation name is required");
        }
        if (!StringUtils.hasText(annotation.getAnnotationType())) {
            throw new IllegalArgumentException("Annotation type is required");
        }
        if (existsByName(annotation.getAnnotationName(), null)) {
            throw new IllegalArgumentException("Annotation name already exists");
        }
        annotation.setAnnotationName(annotation.getAnnotationName().trim());
        normalizeDefaults(annotation, true);
        annotationMapper.insert(annotation);
        return annotationMapper.selectById(annotation.getId());
    }

    /**
     * Update an annotation by merging unset frontend fields with existing values.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmAnnotation updateAnnotation(Long id, AlgorithmAnnotation annotation) {
        AlgorithmAnnotation existing = annotationMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Annotation does not exist");
        }
        if (annotation == null) {
            annotation = new AlgorithmAnnotation();
        }
        annotation.setId(id);
        mergeUnsetFields(annotation, existing);
        if (existsByName(annotation.getAnnotationName(), id)) {
            throw new IllegalArgumentException("Annotation name already exists");
        }
        annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));
        annotation.setAnnotationStatus(calculateAnnotationStatus(annotation.getProgress()));
        annotation.setUpdateTime(new Date());
        annotationMapper.updateById(annotation);
        return annotationMapper.selectById(id);
    }

    /**
     * Delete one annotation task through MyBatis-Plus logical deletion.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotation(Long id) {
        return id != null && annotationMapper.deleteById(id) > 0;
    }

    /**
     * Delete multiple annotation tasks through MyBatis-Plus logical deletion.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotations(List<Long> ids) {
        return ids != null && !ids.isEmpty() && annotationMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * Mark an annotation as partial.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startAnnotationTask(Long id) {
        AlgorithmAnnotation update = new AlgorithmAnnotation();
        update.setId(id);
        update.setAnnotationStatus(STATUS_PARTIAL);
        update.setUpdateTime(new Date());
        return id != null && annotationMapper.updateById(update) > 0;
    }

    /**
     * Mark an annotation as completed and align counts.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAnnotationTask(Long id) {
        AlgorithmAnnotation existing = annotationMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        AlgorithmAnnotation update = new AlgorithmAnnotation();
        update.setId(id);
        update.setAnnotationStatus(STATUS_COMPLETED);
        update.setProgress(100);
        update.setAnnotatedCount(nullToZero(existing.getTotalCount()));
        update.setUpdateTime(new Date());
        return annotationMapper.updateById(update) > 0;
    }

    /**
     * Reset an annotation to empty progress.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetAnnotationTask(Long id) {
        AlgorithmAnnotation update = new AlgorithmAnnotation();
        update.setId(id);
        update.setAnnotationStatus(STATUS_NONE);
        update.setProgress(0);
        update.setAnnotatedCount(0);
        update.setUpdateTime(new Date());
        return id != null && annotationMapper.updateById(update) > 0;
    }

    /**
     * Update progress counters from the frontend JSON body.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnnotationProgress(Long id, Integer annotatedCount) {
        AlgorithmAnnotation existing = annotationMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        int safeAnnotated = Math.max(0, annotatedCount == null ? 0 : annotatedCount);
        int progress = calculateProgress(safeAnnotated, existing.getTotalCount());
        AlgorithmAnnotation update = new AlgorithmAnnotation();
        update.setId(id);
        update.setAnnotatedCount(safeAnnotated);
        update.setProgress(progress);
        update.setAnnotationStatus(calculateAnnotationStatus(progress));
        update.setUpdateTime(new Date());
        return annotationMapper.updateById(update) > 0;
    }

    /**
     * Return a blob compatible with the frontend export download.
     */
    @Override
    public ResponseEntity<byte[]> exportAnnotationData(Long id) {
        AlgorithmAnnotation annotation = annotationMapper.selectById(id);
        if (annotation == null) {
            return textResponse(HttpStatus.NOT_FOUND, "Annotation not found");
        }
        byte[] bytes = readDatasetBytes(annotation);
        String fileName = safeFileName(annotation.getAnnotationName()) + "_annotation.zip";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeFileName(fileName));
        headers.setContentLength(bytes.length);
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Accept a dataset zip upload and record a compatibility import result.
     */
    @Override
    public Map<String, Object> importAnnotationZip(Long id, MultipartFile file) {
        AlgorithmAnnotation annotation = annotationMapper.selectById(id);
        if (annotation == null) {
            throw new IllegalArgumentException("Annotation does not exist");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Zip file is empty");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("message", "Import accepted");
        result.put("fileName", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("annotationId", id);
        return result;
    }

    /**
     * Build aggregate counters used by dashboard widgets.
     */
    @Override
    public Map<String, Object> getStatistics() {
        List<AlgorithmAnnotation> annotations = annotationMapper.selectList(new LambdaQueryWrapper<AlgorithmAnnotation>());
        Map<String, Object> statistics = new LinkedHashMap<String, Object>();
        long none = 0L;
        long partial = 0L;
        long completed = 0L;
        long totalCount = 0L;
        long annotatedCount = 0L;
        for (AlgorithmAnnotation annotation : annotations) {
            String status = normalizeStatus(annotation.getAnnotationStatus());
            if (STATUS_COMPLETED.equals(status)) {
                completed++;
            } else if (STATUS_PARTIAL.equals(status)) {
                partial++;
            } else {
                none++;
            }
            totalCount += nullToZero(annotation.getTotalCount());
            annotatedCount += nullToZero(annotation.getAnnotatedCount());
        }
        statistics.put("total", Long.valueOf(annotations.size()));
        statistics.put("none", none);
        statistics.put("partial", partial);
        statistics.put("completed", completed);
        statistics.put("totalCount", totalCount);
        statistics.put("annotatedCount", annotatedCount);
        statistics.put("overallProgress", totalCount == 0L ? 0 : Math.round((annotatedCount * 100.0d) / totalCount));
        return statistics;
    }

    /**
     * Group annotations by annotation type.
     */
    @Override
    public List<Map<String, Object>> getTypeStatistics() {
        return groupByField("annotationType");
    }

    /**
     * Group annotations by annotation status.
     */
    @Override
    public List<Map<String, Object>> getStatusStatistics() {
        return groupByField("annotationStatus");
    }

    /**
     * Group annotations into progress ranges.
     */
    @Override
    public List<Map<String, Object>> getProgressStatistics() {
        List<AlgorithmAnnotation> annotations = annotationMapper.selectList(new LambdaQueryWrapper<AlgorithmAnnotation>());
        Map<String, Long> buckets = new LinkedHashMap<String, Long>();
        buckets.put("0-25%", 0L);
        buckets.put("25-50%", 0L);
        buckets.put("50-75%", 0L);
        buckets.put("75-100%", 0L);
        buckets.put("100%", 0L);
        for (AlgorithmAnnotation annotation : annotations) {
            int progress = nullToZero(annotation.getProgress());
            String bucket = progress < 25 ? "0-25%" : progress < 50 ? "25-50%" : progress < 75 ? "50-75%" : progress < 100 ? "75-100%" : "100%";
            buckets.put(bucket, buckets.get(bucket) + 1L);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Long> entry : buckets.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("progressRange", entry.getKey());
            row.put("progress_range", entry.getKey());
            row.put("count", entry.getValue());
            result.add(row);
        }
        return result;
    }

    /**
     * Return workload totals for all annotation tasks.
     */
    @Override
    public Map<String, Object> getWorkloadStatistics() {
        Map<String, Object> statistics = getStatistics();
        Map<String, Object> workload = new LinkedHashMap<String, Object>();
        workload.put("totalCount", statistics.get("totalCount"));
        workload.put("annotatedCount", statistics.get("annotatedCount"));
        workload.put("overallProgress", statistics.get("overallProgress"));
        workload.put("overall_progress", statistics.get("overallProgress"));
        return workload;
    }

    /**
     * Search annotations using the same implementation as paged listing.
     */
    @Override
    public BladePage<AlgorithmAnnotation> searchAnnotations(Long current, Long size, String annotationName,
                                                           String annotationType, String annotationStatus) {
        return getAnnotationPage(current, size, annotationName, annotationType, annotationStatus, null, null);
    }

    /**
     * Execute supported frontend batch operations.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchOperation(String operation, List<Long> ids) {
        if (!StringUtils.hasText(operation) || ids == null || ids.isEmpty()) {
            return false;
        }
        String normalized = operation.trim().toLowerCase(Locale.ROOT);
        if ("delete".equals(normalized) || "remove".equals(normalized)) {
            return deleteAnnotations(ids);
        }
        boolean updated = false;
        for (Long id : ids) {
            if ("complete".equals(normalized) || "completed".equals(normalized)) {
                updated = completeAnnotationTask(id) || updated;
            } else if ("reset".equals(normalized)) {
                updated = resetAnnotationTask(id) || updated;
            } else if ("start".equals(normalized) || "partial".equals(normalized)) {
                updated = startAnnotationTask(id) || updated;
            }
        }
        return updated;
    }

    /**
     * Save dataset metadata without invoking remote SFTP generation.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDataset(Long id, String annotationData) {
        AlgorithmAnnotation existing = annotationMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        AlgorithmAnnotation update = new AlgorithmAnnotation();
        update.setId(id);
        update.setDatasetPath(StringUtils.hasText(existing.getDatasetPath())
            ? existing.getDatasetPath()
            : "/data/work/ultralytics_yolov8-main/datasets/vls/annotation_" + id + "/dataset.yaml");
        if (StringUtils.hasText(annotationData)) {
            update.setAnnotationRules(annotationData);
        }
        update.setUpdateTime(new Date());
        return annotationMapper.updateById(update) > 0;
    }

    private LambdaQueryWrapper<AlgorithmAnnotation> buildQuery(String annotationName, String annotationType, String annotationStatus) {
        LambdaQueryWrapper<AlgorithmAnnotation> wrapper = new LambdaQueryWrapper<AlgorithmAnnotation>();
        if (StringUtils.hasText(annotationName)) {
            wrapper.like(AlgorithmAnnotation::getAnnotationName, annotationName.trim());
        }
        if (StringUtils.hasText(annotationType)) {
            wrapper.eq(AlgorithmAnnotation::getAnnotationType, annotationType.trim());
        }
        if (StringUtils.hasText(annotationStatus)) {
            wrapper.eq(AlgorithmAnnotation::getAnnotationStatus, normalizeStatus(annotationStatus));
        }
        return wrapper;
    }

    private void normalizeDefaults(AlgorithmAnnotation annotation, boolean created) {
        if (!StringUtils.hasText(annotation.getTenantId())) {
            annotation.setTenantId("000000");
        }
        if (annotation.getTotalCount() == null) {
            annotation.setTotalCount(0);
        }
        if (annotation.getAnnotatedCount() == null) {
            annotation.setAnnotatedCount(0);
        }
        annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));
        if (!StringUtils.hasText(annotation.getAnnotationStatus())) {
            annotation.setAnnotationStatus(calculateAnnotationStatus(annotation.getProgress()));
        } else {
            annotation.setAnnotationStatus(normalizeStatus(annotation.getAnnotationStatus()));
        }
        if (annotation.getStatus() == null) {
            annotation.setStatus(1);
        }
        if (annotation.getIsDeleted() == null) {
            annotation.setIsDeleted(0);
        }
        if (created && annotation.getCreateTime() == null) {
            annotation.setCreateTime(new Date());
        }
    }

    private void mergeUnsetFields(AlgorithmAnnotation target, AlgorithmAnnotation existing) {
        if (!StringUtils.hasText(target.getTenantId())) {
            target.setTenantId(existing.getTenantId());
        }
        if (!StringUtils.hasText(target.getAnnotationName())) {
            target.setAnnotationName(existing.getAnnotationName());
        } else {
            target.setAnnotationName(target.getAnnotationName().trim());
        }
        if (!StringUtils.hasText(target.getAnnotationType())) {
            target.setAnnotationType(existing.getAnnotationType());
        }
        if (!StringUtils.hasText(target.getDatasetPath())) {
            target.setDatasetPath(existing.getDatasetPath());
        }
        if (target.getTotalCount() == null) {
            target.setTotalCount(existing.getTotalCount());
        }
        if (target.getAnnotatedCount() == null) {
            target.setAnnotatedCount(existing.getAnnotatedCount());
        }
        if (!StringUtils.hasText(target.getAnnotationRules())) {
            target.setAnnotationRules(existing.getAnnotationRules());
        }
        if (!StringUtils.hasText(target.getRemark())) {
            target.setRemark(existing.getRemark());
        }
        if (target.getCreateUser() == null) {
            target.setCreateUser(existing.getCreateUser());
        }
        if (!StringUtils.hasText(target.getCreateDept())) {
            target.setCreateDept(existing.getCreateDept());
        }
        if (target.getCreateTime() == null) {
            target.setCreateTime(existing.getCreateTime());
        }
        if (target.getStatus() == null) {
            target.setStatus(existing.getStatus());
        }
        if (target.getIsDeleted() == null) {
            target.setIsDeleted(existing.getIsDeleted());
        }
    }

    private boolean existsByName(String annotationName, Long excludeId) {
        if (!StringUtils.hasText(annotationName)) {
            return false;
        }
        LambdaQueryWrapper<AlgorithmAnnotation> wrapper = new LambdaQueryWrapper<AlgorithmAnnotation>()
            .eq(AlgorithmAnnotation::getAnnotationName, annotationName.trim());
        if (excludeId != null) {
            wrapper.ne(AlgorithmAnnotation::getId, excludeId);
        }
        return annotationMapper.selectCount(wrapper) > 0L;
    }

    private List<Map<String, Object>> groupByField(String field) {
        List<AlgorithmAnnotation> annotations = annotationMapper.selectList(new LambdaQueryWrapper<AlgorithmAnnotation>());
        Map<String, Long> counts = new LinkedHashMap<String, Long>();
        for (AlgorithmAnnotation annotation : annotations) {
            String key = "annotationType".equals(field) ? annotation.getAnnotationType() : normalizeStatus(annotation.getAnnotationStatus());
            if (!StringUtils.hasText(key)) {
                key = "unknown";
            }
            counts.put(key, counts.containsKey(key) ? counts.get(key) + 1L : 1L);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put(field, entry.getKey());
            row.put(toSnakeCase(field), entry.getKey());
            row.put("count", entry.getValue());
            result.add(row);
        }
        return result;
    }

    private byte[] readDatasetBytes(AlgorithmAnnotation annotation) {
        if (StringUtils.hasText(annotation.getDatasetPath())) {
            Path path = Paths.get(annotation.getDatasetPath());
            if (Files.isRegularFile(path)) {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException ignored) {
                    return fallbackExportBytes(annotation);
                }
            }
        }
        return fallbackExportBytes(annotation);
    }

    private byte[] fallbackExportBytes(AlgorithmAnnotation annotation) {
        String content = "annotationId=" + annotation.getId() + "\n"
            + "annotationName=" + annotation.getAnnotationName() + "\n"
            + "datasetPath=" + (annotation.getDatasetPath() == null ? "" : annotation.getDatasetPath()) + "\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private ResponseEntity<byte[]> textResponse(HttpStatus status, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<byte[]>(message.getBytes(StandardCharsets.UTF_8), headers, status);
    }

    private int calculateProgress(Integer annotatedCount, Integer totalCount) {
        int total = nullToZero(totalCount);
        if (total <= 0) {
            return 0;
        }
        int annotated = Math.max(0, nullToZero(annotatedCount));
        return Math.min(100, (annotated * 100) / total);
    }

    private String calculateAnnotationStatus(int progress) {
        if (progress <= 0) {
            return STATUS_NONE;
        }
        return progress >= 100 ? STATUS_COMPLETED : STATUS_PARTIAL;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return status;
        }
        String value = status.trim().toLowerCase(Locale.ROOT);
        if ("unannotated".equals(value) || "未标注".equals(status)) {
            return STATUS_NONE;
        }
        if ("processing".equals(value) || "in_progress".equals(value) || "标注中".equals(status)) {
            return STATUS_PARTIAL;
        }
        if ("complete".equals(value) || "done".equals(value) || "已完成".equals(status)) {
            return STATUS_COMPLETED;
        }
        return value;
    }

    private long normalizePage(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1L ? 10L : size;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Date parseDate(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        String pattern = text.length() <= 10 ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
        try {
            Date date = new SimpleDateFormat(pattern).parse(text);
            if (endOfDay && text.length() <= 10) {
                return new Date(date.getTime() + 86399000L);
            }
            return date;
        } catch (ParseException ignored) {
            return null;
        }
    }

    private String safeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "annotation";
        }
        return value.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String encodeFileName(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }

    private String toSnakeCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(ch));
        }
        return builder.toString();
    }
}
