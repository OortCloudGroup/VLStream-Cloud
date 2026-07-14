/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.Algorithm;
import com.ruoyi.vlstream.domain.AlgorithmModel;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.service.IVlsAlgorithmTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
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
import java.util.Map;

/**
 * Service for the VLS algorithm training frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsAlgorithmTrainingServiceImpl implements IVlsAlgorithmTrainingService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_TRAINING = "training";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_STOP = "stop";

    private final VlsAlgorithmTrainingMapper trainingMapper;
    private final VlsAlgorithmMapper algorithmMapper;
    private final VlsAlgorithmModelMapper algorithmModelMapper;

    @Override
    public BladePage<AlgorithmTraining> getTrainingPage(Long current, Long size, String taskName, String trainStatus,
                                                        String createdTimeBegin, String createdTimeEnd) {
        Page<AlgorithmTraining> page = new Page<AlgorithmTraining>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<AlgorithmTraining> wrapper = new LambdaQueryWrapper<AlgorithmTraining>();
        if (StringUtils.hasText(taskName)) {
            wrapper.like(AlgorithmTraining::getTaskName, taskName.trim());
        }
        if (StringUtils.hasText(trainStatus)) {
            wrapper.eq(AlgorithmTraining::getTrainStatus, normalizeStatus(trainStatus));
        }
        Date begin = parseDate(createdTimeBegin, false);
        if (begin != null) {
            wrapper.ge(AlgorithmTraining::getCreateTime, begin);
        }
        Date end = parseDate(createdTimeEnd, true);
        if (end != null) {
            wrapper.le(AlgorithmTraining::getCreateTime, end);
        }
        wrapper.orderByDesc(AlgorithmTraining::getCreateTime).orderByDesc(AlgorithmTraining::getId);

        Page<AlgorithmTraining> result = trainingMapper.selectPage(page, wrapper);
        return BladePage.of(fillDerived(result.getRecords()), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public AlgorithmTraining getTrainingById(Long id) {
        return fillDerived(trainingMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmTraining createTraining(AlgorithmTraining training) {
        if (training == null || !StringUtils.hasText(training.getTaskName())) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (training.getAlgorithmId() == null) {
            throw new IllegalArgumentException("Algorithm ID is required");
        }
        training.setTaskName(training.getTaskName().trim());
        normalizeDefaults(training, true);
        trainingMapper.insert(training);
        return getTrainingById(training.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmTraining updateTraining(Long id, AlgorithmTraining training) {
        AlgorithmTraining existing = trainingMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Training task does not exist");
        }
        if (training == null) {
            training = new AlgorithmTraining();
        }
        training.setId(id);
        mergeUnsetFields(training, existing);
        training.setUpdateTime(new Date());
        trainingMapper.updateById(training);
        return getTrainingById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTraining(Long id) {
        return id != null && trainingMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTrainings(List<Long> ids) {
        return ids != null && !ids.isEmpty() && trainingMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startTraining(Long id, Integer epochs, Long datasetId, Integer batchSize,
                                             Integer imgSize, String extraParams) {
        AlgorithmTraining training = requireTraining(id);
        int resolvedEpochs = epochs == null || epochs < 1 ? nullToDefault(training.getEpochTotal(), 10) : epochs;
        String logPath = StringUtils.hasText(training.getLogPath())
            ? training.getLogPath()
            : "/logs/training_" + id + ".log";

        AlgorithmTraining update = new AlgorithmTraining();
        update.setId(id);
        update.setTrainStatus(STATUS_TRAINING);
        update.setProgress(0);
        update.setEpochCurrent(0);
        update.setEpochTotal(resolvedEpochs);
        update.setDatasetId(datasetId == null ? training.getDatasetId() : datasetId);
        update.setLogPath(logPath);
        update.setStartTime(new Date());
        update.setErrorMessage("");
        update.setStatus(training.getStatus());
        trainingMapper.updateById(update);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", STATUS_TRAINING);
        result.put("trainStatus", STATUS_TRAINING);
        result.put("logPath", logPath);
        result.put("datasetId", update.getDatasetId());
        result.put("epochs", resolvedEpochs);
        result.put("batchSize", batchSize == null ? 16 : batchSize);
        result.put("imgSize", imgSize == null ? 640 : imgSize);
        result.put("extraParams", extraParams);
        result.put("message", "Training request accepted");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopTraining(Long id) {
        AlgorithmTraining training = trainingMapper.selectById(id);
        if (training == null) {
            return false;
        }
        AlgorithmTraining update = new AlgorithmTraining();
        update.setId(id);
        update.setTrainStatus(STATUS_STOP);
        update.setEndTime(new Date());
        update.setProgress(training.getProgress());
        return trainingMapper.updateById(update) > 0;
    }

    @Override
    public Map<String, Object> getTrainingLogs(Long id, String logPath, Integer lines) {
        AlgorithmTraining training = requireTraining(id);
        String resolvedLogPath = firstNonBlank(logPath, training.getLogPath(), "/logs/training_" + id + ".log");
        String logContent = readLocalLogTail(resolvedLogPath, lines == null ? 200 : lines);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("logPath", resolvedLogPath);
        result.put("logContent", logContent);
        result.put("currentEpoch", nullToDefault(training.getEpochCurrent(), 0));
        result.put("totalEpoch", nullToDefault(training.getEpochTotal(), 0));
        result.put("progress", nullToDefault(training.getProgress(), 0));
        result.put("completed", STATUS_COMPLETED.equals(training.getTrainStatus()));
        result.put("modelPath", firstNonBlank(training.getModelOutputPath(), training.getModelPath()));
        result.put("modelOutputPath", firstNonBlank(training.getModelOutputPath(), training.getModelPath()));
        result.put("status", training.getTrainStatus());
        result.put("trainStatus", training.getTrainStatus());
        result.put("message", StringUtils.hasText(logContent) ? "Log loaded" : "No local training log available");
        return result;
    }

    @Override
    public Map<String, Object> getTrainingStatus(Long id, String logPath) {
        AlgorithmTraining training = requireTraining(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskId", id);
        result.put("status", training.getTrainStatus());
        result.put("trainStatus", training.getTrainStatus());
        result.put("currentEpoch", nullToDefault(training.getEpochCurrent(), 0));
        result.put("totalEpochs", nullToDefault(training.getEpochTotal(), 0));
        result.put("percentage", nullToDefault(training.getProgress(), 0));
        result.put("completed", STATUS_COMPLETED.equals(training.getTrainStatus()));
        result.put("modelPath", firstNonBlank(training.getModelOutputPath(), training.getModelPath()));
        result.put("modelOutputPath", firstNonBlank(training.getModelOutputPath(), training.getModelPath()));
        result.put("logPath", firstNonBlank(logPath, training.getLogPath()));
        result.put("latestLoss", training.getLossValue() == null ? null : training.getLossValue().floatValue());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> convertModel(Long id) {
        AlgorithmTraining training = requireTraining(id);
        String basePath = firstNonBlank(training.getModelOutputPath(), training.getModelPath());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("trainingId", id);
        if (!StringUtils.hasText(basePath)) {
            result.put("status", "skipped");
            result.put("message", "Model path is empty");
            return result;
        }

        AlgorithmTraining update = new AlgorithmTraining();
        update.setId(id);
        update.setOnnxModelOutputPath(firstNonBlank(training.getOnnxModelOutputPath(), replaceExtension(basePath, ".onnx")));
        update.setRknnModelOutputPath(firstNonBlank(training.getRknnModelOutputPath(), replaceExtension(basePath, "-rk3588.rknn")));
        update.setInt8RknnModelOutputPath(firstNonBlank(training.getInt8RknnModelOutputPath(), replaceExtension(basePath, "-rk3588-int8.rknn")));
        update.setUpdateTime(new Date());
        trainingMapper.updateById(update);

        result.put("status", "submitted");
        result.put("onnxModelOutputPath", update.getOnnxModelOutputPath());
        result.put("rknnModelOutputPath", update.getRknnModelOutputPath());
        result.put("int8RknnModelOutputPath", update.getInt8RknnModelOutputPath());
        result.put("message", "Model conversion paths recorded");
        return result;
    }

    @Override
    public ResponseEntity<byte[]> downloadModel(String id, String type) {
        String downloadPath = resolveDownloadPath(id, type);
        if (!StringUtils.hasText(downloadPath)) {
            return textResponse(HttpStatus.NOT_FOUND, "Model file path is empty");
        }

        Path path = Paths.get(downloadPath);
        if (!Files.isRegularFile(path)) {
            return textResponse(HttpStatus.NOT_FOUND, "Model file not found: " + downloadPath);
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String fileName = path.getFileName() == null ? "model" : path.getFileName().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
            headers.setContentLength(bytes.length);
            return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
        } catch (IOException ex) {
            return textResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Download failed: " + ex.getMessage());
        }
    }

    private AlgorithmTraining requireTraining(Long id) {
        AlgorithmTraining training = trainingMapper.selectById(id);
        if (training == null) {
            throw new IllegalArgumentException("Training task does not exist");
        }
        return fillDerived(training);
    }

    private void normalizeDefaults(AlgorithmTraining training, boolean created) {
        if (!StringUtils.hasText(training.getTenantId())) {
            training.setTenantId("000000");
        }
        if (!StringUtils.hasText(training.getTrainStatus())) {
            training.setTrainStatus(STATUS_PENDING);
        } else {
            training.setTrainStatus(normalizeStatus(training.getTrainStatus()));
        }
        if (training.getProgress() == null) {
            training.setProgress(0);
        }
        if (training.getEpochCurrent() == null) {
            training.setEpochCurrent(0);
        }
        if (training.getEpochTotal() == null || training.getEpochTotal() < 1) {
            training.setEpochTotal(100);
        }
        if (training.getStatus() == null) {
            training.setStatus(1);
        }
        if (training.getIsDeleted() == null) {
            training.setIsDeleted(0);
        }
        if (created && training.getCreateTime() == null) {
            training.setCreateTime(new Date());
        }
    }

    private void mergeUnsetFields(AlgorithmTraining target, AlgorithmTraining existing) {
        if (!StringUtils.hasText(target.getTenantId())) {
            target.setTenantId(existing.getTenantId());
        }
        if (!StringUtils.hasText(target.getTaskName())) {
            target.setTaskName(existing.getTaskName());
        } else {
            target.setTaskName(target.getTaskName().trim());
        }
        if (target.getAlgorithmId() == null) {
            target.setAlgorithmId(existing.getAlgorithmId());
        }
        if (!StringUtils.hasText(target.getTrainStatus())) {
            target.setTrainStatus(existing.getTrainStatus());
        } else {
            target.setTrainStatus(normalizeStatus(target.getTrainStatus()));
        }
        if (target.getProgress() == null) {
            target.setProgress(existing.getProgress());
        }
        if (target.getEpochCurrent() == null) {
            target.setEpochCurrent(existing.getEpochCurrent());
        }
        if (target.getEpochTotal() == null) {
            target.setEpochTotal(existing.getEpochTotal());
        }
        if (target.getAccuracy() == null) {
            target.setAccuracy(existing.getAccuracy());
        }
        if (target.getPrecisionValue() == null) {
            target.setPrecisionValue(existing.getPrecisionValue());
        }
        if (target.getRecallValue() == null) {
            target.setRecallValue(existing.getRecallValue());
        }
        if (target.getMapValue() == null) {
            target.setMapValue(existing.getMapValue());
        }
        if (target.getLossValue() == null) {
            target.setLossValue(existing.getLossValue());
        }
        if (!StringUtils.hasText(target.getGpuUsage())) {
            target.setGpuUsage(existing.getGpuUsage());
        }
        if (target.getStartTime() == null) {
            target.setStartTime(existing.getStartTime());
        }
        if (target.getEndTime() == null) {
            target.setEndTime(existing.getEndTime());
        }
        if (!StringUtils.hasText(target.getEstimatedTime())) {
            target.setEstimatedTime(existing.getEstimatedTime());
        }
        if (!StringUtils.hasText(target.getModelOutputPath())) {
            target.setModelOutputPath(existing.getModelOutputPath());
        }
        if (!StringUtils.hasText(target.getOnnxModelOutputPath())) {
            target.setOnnxModelOutputPath(existing.getOnnxModelOutputPath());
        }
        if (!StringUtils.hasText(target.getRknnModelOutputPath())) {
            target.setRknnModelOutputPath(existing.getRknnModelOutputPath());
        }
        if (!StringUtils.hasText(target.getInt8RknnModelOutputPath())) {
            target.setInt8RknnModelOutputPath(existing.getInt8RknnModelOutputPath());
        }
        if (!StringUtils.hasText(target.getLogPath())) {
            target.setLogPath(existing.getLogPath());
        }
        if (!StringUtils.hasText(target.getConfigParams())) {
            target.setConfigParams(existing.getConfigParams());
        }
        if (!StringUtils.hasText(target.getErrorMessage())) {
            target.setErrorMessage(existing.getErrorMessage());
        }
        if (!StringUtils.hasText(target.getModelPath())) {
            target.setModelPath(existing.getModelPath());
        }
        if (!StringUtils.hasText(target.getCompletedAt())) {
            target.setCompletedAt(existing.getCompletedAt());
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

    private AlgorithmTraining fillDerived(AlgorithmTraining training) {
        if (training == null) {
            return null;
        }
        if (training.getAlgorithmId() != null) {
            Algorithm algorithm = algorithmMapper.selectById(training.getAlgorithmId());
            if (algorithm != null) {
                training.setAlgorithmName(algorithm.getName());
                training.setTrainType(algorithm.getCategory());
                training.setTargetModel(algorithm.getPtModelFilePath());
            }
        }
        training.setTrainStatusDesc(statusDescription(training.getTrainStatus()));
        training.setDurationMinutes(durationMinutes(training.getStartTime(), training.getEndTime()));
        return training;
    }

    private List<AlgorithmTraining> fillDerived(List<AlgorithmTraining> trainings) {
        List<AlgorithmTraining> result = new ArrayList<AlgorithmTraining>();
        if (trainings == null) {
            return result;
        }
        for (AlgorithmTraining training : trainings) {
            result.add(fillDerived(training));
        }
        return result;
    }

    private String resolveDownloadPath(String id, String type) {
        Long numericId = parseLong(id);
        if (numericId == null) {
            return null;
        }
        AlgorithmModel model = algorithmModelMapper.selectById(numericId);
        if (model != null) {
            return chooseModelPath(type, model.getModelPath(), model.getOnnxModelPath(),
                model.getRknnModelPath(), model.getInt8RknnModelOutputPath());
        }
        AlgorithmTraining training = trainingMapper.selectById(numericId);
        if (training == null) {
            return null;
        }
        return chooseModelPath(type, firstNonBlank(training.getModelPath(), training.getModelOutputPath()),
            training.getOnnxModelOutputPath(), training.getRknnModelOutputPath(), training.getInt8RknnModelOutputPath());
    }

    private String chooseModelPath(String type, String ptPath, String onnxPath, String rknnPath, String int8RknnPath) {
        String normalized = StringUtils.hasText(type) ? type.trim().toLowerCase() : "pt";
        if ("onnx".equals(normalized)) {
            return onnxPath;
        }
        if ("rknn".equals(normalized)) {
            return rknnPath;
        }
        if ("int8-rknn".equals(normalized) || "int8_rknn".equals(normalized)) {
            return int8RknnPath;
        }
        return ptPath;
    }

    private String readLocalLogTail(String logPath, int lineCount) {
        if (!StringUtils.hasText(logPath)) {
            return "";
        }
        Path path = Paths.get(logPath);
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int fromIndex = Math.max(0, lines.size() - Math.max(1, lineCount));
            return String.join("\n", lines.subList(fromIndex, lines.size()));
        } catch (IOException ex) {
            return "Failed to read local log: " + ex.getMessage();
        }
    }

    private ResponseEntity<byte[]> textResponse(HttpStatus status, String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<byte[]>(bytes, headers, status);
    }

    private String replaceExtension(String path, String suffix) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot > slash) {
            return path.substring(0, dot) + suffix;
        }
        return path + suffix;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return status;
        }
        String value = status.trim();
        if ("waiting".equalsIgnoreCase(value) || "wait".equalsIgnoreCase(value) || "等待".equals(value)) {
            return STATUS_PENDING;
        }
        if ("running".equalsIgnoreCase(value) || "训练中".equals(value)) {
            return STATUS_TRAINING;
        }
        if ("success".equalsIgnoreCase(value) || "done".equalsIgnoreCase(value) || "训练完成".equals(value)) {
            return STATUS_COMPLETED;
        }
        if ("error".equalsIgnoreCase(value) || "训练失败".equals(value)) {
            return STATUS_FAILED;
        }
        if ("stopped".equalsIgnoreCase(value) || "停止".equals(value) || "训练终止".equals(value)) {
            return STATUS_STOP;
        }
        return value;
    }

    private String statusDescription(String status) {
        if (STATUS_PENDING.equals(status)) {
            return "Wait";
        }
        if (STATUS_TRAINING.equals(status)) {
            return "Training";
        }
        if (STATUS_COMPLETED.equals(status)) {
            return "Completed";
        }
        if (STATUS_FAILED.equals(status)) {
            return "Failure";
        }
        if (STATUS_STOP.equals(status)) {
            return "Stop";
        }
        return status;
    }

    private long normalizePage(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1L ? 10L : size;
    }

    private Integer nullToDefault(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Long durationMinutes(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Math.max(0L, (endTime.getTime() - startTime.getTime()) / 60000L);
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

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
