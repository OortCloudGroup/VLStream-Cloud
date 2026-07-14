/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.Algorithm;
import com.ruoyi.vlstream.service.IVlsAlgorithmService;
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

/**
 * Algorithm routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithm")
public class VlsAlgorithmController {

    private final IVlsAlgorithmService algorithmService;

    @GetMapping("/page")
    public BladeResult<BladePage<Algorithm>> getAlgorithmPage(@RequestParam(required = false) Long current,
                                                              @RequestParam(required = false) Long size,
                                                              @RequestParam(required = false) Long repositoryId,
                                                              @RequestParam(required = false) String name,
                                                              @RequestParam(required = false) String category,
                                                              @RequestParam(required = false) String type,
                                                              @RequestParam(required = false) String deployStatus) {
        return BladeResult.success(algorithmService.getAlgorithmPage(current, size, repositoryId, name, category, type, deployStatus));
    }

    @GetMapping("/repository/{repositoryId}")
    public BladeResult<List<Algorithm>> getAlgorithmsByRepositoryId(@PathVariable Long repositoryId) {
        return BladeResult.success(algorithmService.getAlgorithmsByRepositoryId(repositoryId));
    }

    @GetMapping("/category/{category}")
    public BladeResult<List<Algorithm>> getAlgorithmsByCategory(@PathVariable String category) {
        return BladeResult.success(algorithmService.getAlgorithmsByCategory(category));
    }

    @GetMapping("/{id}")
    public BladeResult<Algorithm> getAlgorithmById(@PathVariable Long id) {
        Algorithm algorithm = algorithmService.getAlgorithmById(id);
        return algorithm == null ? BladeResult.<Algorithm>fail("Algorithm does not exist") : BladeResult.success(algorithm);
    }

    @PostMapping
    public BladeResult<Algorithm> createAlgorithm(@RequestBody Algorithm algorithm) {
        try {
            return BladeResult.success(algorithmService.createAlgorithm(algorithm));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<Algorithm> updateAlgorithm(@PathVariable Long id,
                                                  @RequestBody Algorithm algorithm) {
        try {
            return BladeResult.success(algorithmService.updateAlgorithm(id, algorithm));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteAlgorithm(@PathVariable Long id) {
        return BladeResult.success(algorithmService.deleteAlgorithm(id));
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteAlgorithms(@RequestBody List<Long> ids) {
        return BladeResult.success(algorithmService.deleteAlgorithms(ids));
    }

    @PutMapping("/{id}/deploy-status")
    public BladeResult<Boolean> updateDeployStatus(@PathVariable Long id,
                                                   @RequestParam String deployStatus) {
        return BladeResult.success(algorithmService.updateDeployStatus(id, deployStatus));
    }

    @PutMapping("/batch/deploy-status")
    public BladeResult<Boolean> batchUpdateDeployStatus(@RequestBody List<Long> ids,
                                                        @RequestParam String deployStatus) {
        return BladeResult.success(algorithmService.updateDeployStatus(ids, deployStatus));
    }

    @PostMapping("/{algorithmId}/deploy")
    public BladeResult<Map<String, Object>> deployAlgorithmToDevices(@PathVariable Long algorithmId,
                                                                     @RequestBody List<Long> deviceIds) {
        Map<String, Object> result = algorithmService.deployAlgorithmToDevices(algorithmId, deviceIds);
        return result == null ? BladeResult.<Map<String, Object>>fail("Algorithm deployment failed") : BladeResult.success(result);
    }

    @PostMapping("/{algorithmId}/evaluate")
    public BladeResult<Map<String, Object>> evaluateAlgorithm(@PathVariable Long algorithmId) {
        Map<String, Object> result = algorithmService.evaluateAlgorithm(algorithmId);
        return result == null ? BladeResult.<Map<String, Object>>fail("Algorithm evaluation failed") : BladeResult.success(result);
    }

    @GetMapping("/statistics/category")
    public BladeResult<List<Map<String, Object>>> getCategoryStatistics() {
        return BladeResult.success(algorithmService.getCategoryStatistics());
    }

    @GetMapping("/statistics/type")
    public BladeResult<List<Map<String, Object>>> getTypeStatistics() {
        return BladeResult.success(algorithmService.getTypeStatistics());
    }

    @GetMapping("/statistics/deploy-status")
    public BladeResult<List<Map<String, Object>>> getDeployStatusStatistics() {
        return BladeResult.success(algorithmService.getDeployStatusStatistics());
    }

    @GetMapping("/count/repository/{repositoryId}")
    public BladeResult<Long> countByRepositoryId(@PathVariable Long repositoryId) {
        return BladeResult.success(algorithmService.countByRepositoryId(repositoryId));
    }
}
