/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Algorithm routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithm")
public class VlsAlgorithmController extends VlsControllerSupport {

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
        return operationResult(algorithmService.deleteAlgorithm(id), "Algorithm was not deleted");
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteAlgorithms(@RequestBody List<Long> ids) {
        return operationResult(algorithmService.deleteAlgorithms(ids), "No algorithms were deleted");
    }

    @PutMapping("/{id}/deploy-status")
    public BladeResult<Boolean> updateDeployStatus(@PathVariable Long id,
                                                   @RequestParam String deployStatus) {
        try {
            return algorithmService.updateDeployStatus(id, deployStatus)
                ? BladeResult.success(Boolean.TRUE)
                : BladeResult.<Boolean>fail("Deployment status was not updated");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/batch/deploy-status")
    public BladeResult<Boolean> batchUpdateDeployStatus(@RequestBody List<Long> ids,
                                                        @RequestParam String deployStatus) {
        try {
            return algorithmService.updateDeployStatus(ids, deployStatus)
                ? BladeResult.success(Boolean.TRUE)
                : BladeResult.<Boolean>fail("Deployment statuses were not updated");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/{algorithmId}/deploy")
    public BladeResult<Map<String, Object>> deployAlgorithmToDevices(@PathVariable Long algorithmId,
                                                                     @RequestBody List<Long> deviceIds) {
        Map<String, Object> result = algorithmService.deployAlgorithmToDevices(algorithmId, deviceIds);
        if (result == null || Boolean.FALSE.equals(result.get("success"))) {
            String message = result == null || result.get("message") == null
                ? "Algorithm deployment failed"
                : String.valueOf(result.get("message"));
            return BladeResult.fail(message);
        }
        return BladeResult.success(result);
    }

    @PostMapping("/{algorithmId}/evaluate")
    public BladeResult<Map<String, Object>> evaluateAlgorithm(@PathVariable Long algorithmId) {
        try {
            Map<String, Object> result = algorithmService.evaluateAlgorithm(algorithmId);
            return result == null ? BladeResult.<Map<String, Object>>fail("Algorithm evaluation failed") : BladeResult.success(result);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
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

    /** Return one algorithm through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<Algorithm> detail(@RequestParam Long id) {
        return getAlgorithmById(id);
    }

    /** Return a SpringBlade-compatible algorithm list page. */
    @GetMapping("/list")
    public BladeResult<BladePage<Algorithm>> list(@RequestParam(required = false) Long current,
                                                  @RequestParam(required = false) Long size,
                                                  @RequestParam(required = false) Long repositoryId,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String deployStatus) {
        return getAlgorithmPage(current, size, repositoryId, name, category, type, deployStatus);
    }

    /** Create an algorithm through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<Algorithm> save(@RequestBody Algorithm algorithm) {
        return createAlgorithm(algorithm);
    }

    /** Update an algorithm through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<Algorithm> update(@RequestBody Algorithm algorithm) {
        if (algorithm == null || algorithm.getId() == null) {
            return BladeResult.fail("Algorithm ID is required");
        }
        return updateAlgorithm(algorithm.getId(), algorithm);
    }

    /** Insert or update an algorithm through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<Algorithm> submit(@RequestBody Algorithm algorithm) {
        return algorithm != null && algorithm.getId() != null
            ? updateAlgorithm(algorithm.getId(), algorithm)
            : createAlgorithm(algorithm);
    }

    /** Delete algorithms by the SpringBlade comma-separated ID convention. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(algorithmService.deleteAlgorithms(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual filtered algorithm rows. */
    @GetMapping("/export-vlsAlgorithm")
    public void exportVlsAlgorithm(@RequestParam(required = false) Long repositoryId,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String deployStatus,
                                   HttpServletResponse response) {
        BladePage<Algorithm> page = algorithmService.getAlgorithmPage(
            Long.valueOf(1L), Long.valueOf(Integer.MAX_VALUE), repositoryId, name, category, type, deployStatus);
        ExcelUtil.exportExcel(page.getRecords(), "Algorithms", Algorithm.class, response);
    }
}
