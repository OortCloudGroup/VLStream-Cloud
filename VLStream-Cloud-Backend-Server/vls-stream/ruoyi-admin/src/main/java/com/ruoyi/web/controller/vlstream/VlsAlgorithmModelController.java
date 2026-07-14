/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmModel;
import com.ruoyi.vlstream.service.IVlsAlgorithmModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Algorithm model routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmModel")
public class VlsAlgorithmModelController {

    private final IVlsAlgorithmModelService modelService;

    @GetMapping("/page")
    public BladeResult<BladePage<AlgorithmModel>> getModelPage(@RequestParam(required = false) Long current,
                                                               @RequestParam(required = false) Long size,
                                                               @RequestParam(required = false) String modelName,
                                                               @RequestParam(required = false) Long algorithmId,
                                                               @RequestParam(required = false) Long trainingId,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String createdTimeBegin,
                                                               @RequestParam(required = false) String createdTimeEnd) {
        return BladeResult.success(modelService.getModelPage(current, size, modelName, algorithmId, trainingId, status, createdTimeBegin, createdTimeEnd));
    }

    @GetMapping("/{id}")
    public BladeResult<AlgorithmModel> getModelById(@PathVariable Long id) {
        AlgorithmModel model = modelService.getModelById(id);
        return model == null ? BladeResult.<AlgorithmModel>fail("Model does not exist") : BladeResult.success(model);
    }

    @PostMapping("/create")
    public BladeResult<AlgorithmModel> createModel(@RequestBody AlgorithmModel model) {
        try {
            return BladeResult.success(modelService.createModel(model));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public BladeResult<AlgorithmModel> updateModel(@RequestBody AlgorithmModel model) {
        try {
            return BladeResult.success(modelService.updateModel(model));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteModel(@PathVariable Long id) {
        return BladeResult.success(modelService.deleteModel(id));
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteModel(@RequestBody List<Long> ids) {
        return BladeResult.success(modelService.deleteModels(ids));
    }

    @GetMapping("/algorithm/{algorithmId}")
    public BladeResult<List<AlgorithmModel>> getModelsByAlgorithmId(@PathVariable Long algorithmId) {
        return BladeResult.success(modelService.getModelsByAlgorithmId(algorithmId));
    }

    @GetMapping("/training/{trainingId}")
    public BladeResult<List<AlgorithmModel>> getModelsByTrainingId(@PathVariable Long trainingId) {
        return BladeResult.success(modelService.getModelsByTrainingId(trainingId));
    }

    @GetMapping("/status/{status}")
    public BladeResult<List<AlgorithmModel>> getModelsByStatus(@PathVariable String status) {
        return BladeResult.success(modelService.getModelsByStatus(status));
    }

    @PostMapping("/publish/{id}")
    public BladeResult<Boolean> publishModel(@PathVariable Long id) {
        return BladeResult.success(modelService.publishModel(id));
    }

    @PostMapping("/unpublish/{id}")
    public BladeResult<Boolean> unpublishModel(@PathVariable Long id) {
        return BladeResult.success(modelService.unpublishModel(id));
    }

    @PostMapping("/batch-publish")
    public BladeResult<Boolean> batchPublishModel(@RequestBody List<Long> ids) {
        return BladeResult.success(modelService.publishModels(ids));
    }

    @GetMapping("/download/{id}")
    public BladeResult<String> downloadModel(@PathVariable Long id) {
        String filePath = modelService.downloadModel(id);
        return filePath == null ? BladeResult.<String>fail("Model does not exist") : BladeResult.success(filePath);
    }

    @PostMapping("/deploy/{id}")
    public BladeResult<Boolean> deployModel(@PathVariable Long id) {
        return BladeResult.success(modelService.deployModel(id));
    }

    @GetMapping("/statistics")
    public BladeResult<Map<String, Object>> getModelStatistics() {
        return BladeResult.success(modelService.getModelStatistics());
    }

    @GetMapping("/check-name-version")
    public BladeResult<Boolean> checkModelNameAndVersion(@RequestParam String modelName,
                                                         @RequestParam Integer version,
                                                         @RequestParam(required = false) Long excludeId) {
        return BladeResult.success(modelService.checkModelNameAndVersion(modelName, version, excludeId));
    }

    @GetMapping("/algorithm/{algorithmId}/version/{version}")
    public BladeResult<AlgorithmModel> getModelByAlgorithmIdAndVersion(@PathVariable Long algorithmId,
                                                                       @PathVariable Integer version) {
        return BladeResult.success(modelService.getModelByAlgorithmIdAndVersion(algorithmId, version));
    }

    @GetMapping("/algorithm/{algorithmId}/latest")
    public BladeResult<AlgorithmModel> getLatestModelByAlgorithmId(@PathVariable Long algorithmId) {
        return BladeResult.success(modelService.getLatestModelByAlgorithmId(algorithmId));
    }

    @GetMapping("/popular")
    public BladeResult<List<AlgorithmModel>> getPopularModels(@RequestParam(defaultValue = "10") Integer limit) {
        return BladeResult.success(modelService.getPopularModels(limit));
    }

    @GetMapping("/count/creator/{createdBy}")
    public BladeResult<Long> countModelsByCreatedBy(@PathVariable Long createdBy) {
        return BladeResult.success(modelService.countModelsByCreatedBy(createdBy));
    }

    @GetMapping("/total-size")
    public BladeResult<Long> getTotalModelSize() {
        return BladeResult.success(modelService.getTotalModelSize());
    }
}
