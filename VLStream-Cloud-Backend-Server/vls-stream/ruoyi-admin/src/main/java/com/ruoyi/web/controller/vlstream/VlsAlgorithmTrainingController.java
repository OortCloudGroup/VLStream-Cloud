/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.service.IVlsAlgorithmTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

/**
 * Algorithm training routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmTraining")
public class VlsAlgorithmTrainingController {

    private final IVlsAlgorithmTrainingService trainingService;

    @GetMapping("/page")
    public BladeResult<BladePage<AlgorithmTraining>> getTrainingPage(@RequestParam(required = false) Long current,
                                                                     @RequestParam(required = false) Long size,
                                                                     @RequestParam(required = false) String taskName,
                                                                     @RequestParam(required = false) String trainStatus,
                                                                     @RequestParam(required = false) String createdTimeBegin,
                                                                     @RequestParam(required = false) String createdTimeEnd) {
        return BladeResult.success(trainingService.getTrainingPage(current, size, taskName, trainStatus, createdTimeBegin, createdTimeEnd));
    }

    @GetMapping("/{id}")
    public BladeResult<AlgorithmTraining> getTrainingById(@PathVariable Long id) {
        AlgorithmTraining training = trainingService.getTrainingById(id);
        return training == null ? BladeResult.<AlgorithmTraining>fail("Training task does not exist") : BladeResult.success(training);
    }

    @PostMapping
    public BladeResult<AlgorithmTraining> createTraining(@RequestBody AlgorithmTraining training) {
        try {
            return BladeResult.success(trainingService.createTraining(training));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<AlgorithmTraining> updateTraining(@PathVariable Long id, @RequestBody AlgorithmTraining training) {
        try {
            return BladeResult.success(trainingService.updateTraining(id, training));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteTraining(@PathVariable Long id) {
        return BladeResult.success(trainingService.deleteTraining(id));
    }

    @PostMapping("/{id}/start")
    public BladeResult<Map<String, Object>> startTraining(@PathVariable Long id,
                                                         @RequestParam(required = false, defaultValue = "10") Integer epochs,
                                                         @RequestParam(required = false) Long datasetId,
                                                         @RequestParam(required = false, defaultValue = "16") Integer batchSize,
                                                         @RequestParam(required = false) Integer imgSize,
                                                         @RequestParam(required = false) String extraParams) {
        try {
            return BladeResult.success(trainingService.startTraining(id, epochs, datasetId, batchSize, imgSize, extraParams));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    public BladeResult<Boolean> stopTraining(@PathVariable Long id) {
        return BladeResult.success(trainingService.stopTraining(id));
    }

    @GetMapping("/{id}/logs")
    public BladeResult<Map<String, Object>> getTrainingLogs(@PathVariable Long id,
                                                           @RequestParam(required = false) String logPath,
                                                           @RequestParam(required = false, defaultValue = "200") Integer lines) {
        try {
            return BladeResult.success(trainingService.getTrainingLogs(id, logPath, lines));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public BladeResult<Map<String, Object>> getTrainingStatus(@PathVariable Long id,
                                                             @RequestParam(required = false) String logPath) {
        try {
            return BladeResult.success(trainingService.getTrainingStatus(id, logPath));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteTraining(@RequestBody List<Long> ids) {
        return BladeResult.success(trainingService.deleteTrainings(ids));
    }

    @PostMapping("/{id}/convert-model")
    public BladeResult<Map<String, Object>> convertModel(@PathVariable Long id) {
        try {
            return BladeResult.success(trainingService.convertModel(id));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/download-model")
    public ResponseEntity<byte[]> downloadModel(@RequestParam String id, @RequestParam String type) {
        return trainingService.downloadModel(id, type);
    }
}
