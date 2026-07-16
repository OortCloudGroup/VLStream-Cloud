/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Algorithm model routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmModel")
public class VlsAlgorithmModelController extends VlsControllerSupport {

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
        return operationResult(modelService.deleteModel(id), "Model was not deleted");
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteModel(@RequestBody List<Long> ids) {
        return operationResult(modelService.deleteModels(ids), "No models were deleted");
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
        return operationResult(modelService.publishModel(id), "Model was not published");
    }

    @PostMapping("/unpublish/{id}")
    public BladeResult<Boolean> unpublishModel(@PathVariable Long id) {
        return operationResult(modelService.unpublishModel(id), "Model was not unpublished");
    }

    @PostMapping("/batch-publish")
    public BladeResult<Boolean> batchPublishModel(@RequestBody List<Long> ids) {
        return operationResult(modelService.publishModels(ids), "No models were published");
    }

    @GetMapping("/download/{id}")
    public BladeResult<String> downloadModel(@PathVariable Long id) {
        try {
            String filePath = modelService.downloadModel(id);
            return filePath == null ? BladeResult.<String>fail("Model does not exist") : BladeResult.success(filePath);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/deploy/{id}")
    public BladeResult<Boolean> deployModel(@PathVariable Long id) {
        try {
            return modelService.deployModel(id) ? BladeResult.success(Boolean.TRUE)
                : BladeResult.<Boolean>fail("Model was not deployed");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
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

    /** Return one model through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<AlgorithmModel> detail(@RequestParam Long id) {
        return getModelById(id);
    }

    /** Return a model page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<AlgorithmModel>> list(@RequestParam(required = false) Long current,
                                                       @RequestParam(required = false) Long size,
                                                       @RequestParam(required = false) String modelName,
                                                       @RequestParam(required = false) Long algorithmId,
                                                       @RequestParam(required = false) Long trainingId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String createdTimeBegin,
                                                       @RequestParam(required = false) String createdTimeEnd) {
        return getModelPage(current, size, modelName, algorithmId, trainingId, status, createdTimeBegin, createdTimeEnd);
    }

    /** Create a model through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<AlgorithmModel> save(@RequestBody AlgorithmModel model) {
        return createModel(model);
    }

    /** Insert or update a model through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<AlgorithmModel> submit(@RequestBody AlgorithmModel model) {
        return model != null && model.getId() != null ? updateModel(model) : createModel(model);
    }

    /** Delete models by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(modelService.deleteModels(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual filtered model rows. */
    @GetMapping("/export-vlsAlgorithmModel")
    public void exportVlsAlgorithmModel(@RequestParam(required = false) String modelName,
                                        @RequestParam(required = false) Long algorithmId,
                                        @RequestParam(required = false) Long trainingId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String createdTimeBegin,
                                        @RequestParam(required = false) String createdTimeEnd,
                                        HttpServletResponse response) {
        BladePage<AlgorithmModel> page = modelService.getModelPage(Long.valueOf(1L), Long.valueOf(Integer.MAX_VALUE),
            modelName, algorithmId, trainingId, status, createdTimeBegin, createdTimeEnd);
        ExcelUtil.exportExcel(page.getRecords(), "Algorithm Models", AlgorithmModel.class, response);
    }
}
