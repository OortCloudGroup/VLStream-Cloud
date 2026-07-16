/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmRepository;
import com.ruoyi.vlstream.service.IVlsAlgorithmRepositoryService;
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

/**
 * Algorithm repository routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmRepository")
public class VlsAlgorithmRepositoryController extends VlsControllerSupport {

    private final IVlsAlgorithmRepositoryService repositoryService;

    @GetMapping("/page")
    public BladeResult<BladePage<AlgorithmRepository>> getRepositoryPage(@RequestParam(required = false) Long current,
                                                                         @RequestParam(required = false) Long size,
                                                                         @RequestParam(required = false) String name,
                                                                         @RequestParam(required = false) String repositoryType,
                                                                         @RequestParam(required = false) String status) {
        return BladeResult.success(repositoryService.getRepositoryPage(current, size, name, repositoryType, status));
    }

    @GetMapping("/enabled")
    public BladeResult<List<AlgorithmRepository>> getEnabledRepositories() {
        return BladeResult.success(repositoryService.getEnabledRepositories());
    }

    @GetMapping("/type/{repositoryType}")
    public BladeResult<List<AlgorithmRepository>> getRepositoriesByType(@PathVariable String repositoryType) {
        return BladeResult.success(repositoryService.getRepositoriesByType(repositoryType));
    }

    @GetMapping("/{id}")
    public BladeResult<AlgorithmRepository> getRepositoryById(@PathVariable Long id) {
        AlgorithmRepository repository = repositoryService.getRepositoryById(id);
        return repository == null ? BladeResult.<AlgorithmRepository>fail("Algorithm repository does not exist") : BladeResult.success(repository);
    }

    @PostMapping
    public BladeResult<AlgorithmRepository> createRepository(@RequestBody AlgorithmRepository repository) {
        try {
            return BladeResult.success(repositoryService.createRepository(repository));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<AlgorithmRepository> updateRepository(@PathVariable Long id,
                                                             @RequestBody AlgorithmRepository repository) {
        try {
            return BladeResult.success(repositoryService.updateRepository(id, repository));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteRepository(@PathVariable Long id) {
        return operationResult(repositoryService.deleteRepository(id), "Repository was not deleted");
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteRepositories(@RequestBody List<Long> ids) {
        return operationResult(repositoryService.deleteRepositories(ids), "No repositories were deleted");
    }

    @PutMapping("/{id}/status")
    public BladeResult<Boolean> updateRepositoryStatus(@PathVariable Long id,
                                                       @RequestParam String status) {
        return operationResult(repositoryService.updateRepositoryStatus(id, status), "Repository status was not updated");
    }

    @PutMapping("/batch/status")
    public BladeResult<Boolean> batchUpdateRepositoryStatus(@RequestBody List<Long> ids,
                                                            @RequestParam String status) {
        return operationResult(repositoryService.updateRepositoryStatus(ids, status), "Repository statuses were not updated");
    }

    @GetMapping("/count")
    public BladeResult<Long> countRepositories() {
        return BladeResult.success(repositoryService.countRepositories());
    }

    @PutMapping("/{id}/refresh-count")
    public BladeResult<Boolean> refreshAlgorithmCount(@PathVariable Long id) {
        return operationResult(repositoryService.refreshAlgorithmCount(id), "Repository algorithm count was not refreshed");
    }

    /** Return one repository through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<AlgorithmRepository> detail(@RequestParam Long id) {
        return getRepositoryById(id);
    }

    /** Return a repository page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<AlgorithmRepository>> list(@RequestParam(required = false) Long current,
                                                            @RequestParam(required = false) Long size,
                                                            @RequestParam(required = false) String name,
                                                            @RequestParam(required = false) String repositoryType,
                                                            @RequestParam(required = false) String status) {
        return getRepositoryPage(current, size, name, repositoryType, status);
    }

    /** Create a repository through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<AlgorithmRepository> save(@RequestBody AlgorithmRepository repository) {
        return createRepository(repository);
    }

    /** Update a repository through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<AlgorithmRepository> update(@RequestBody AlgorithmRepository repository) {
        if (repository == null || repository.getId() == null) {
            return BladeResult.fail("Algorithm repository ID is required");
        }
        return updateRepository(repository.getId(), repository);
    }

    /** Insert or update a repository through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<AlgorithmRepository> submit(@RequestBody AlgorithmRepository repository) {
        return repository != null && repository.getId() != null
            ? updateRepository(repository.getId(), repository)
            : createRepository(repository);
    }

    /** Delete repositories by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(repositoryService.deleteRepositories(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual filtered repository rows. */
    @GetMapping("/export-vlsAlgorithmRepository")
    public void exportVlsAlgorithmRepository(@RequestParam(required = false) String name,
                                             @RequestParam(required = false) String repositoryType,
                                             @RequestParam(required = false) String status,
                                             HttpServletResponse response) {
        BladePage<AlgorithmRepository> page = repositoryService.getRepositoryPage(
            Long.valueOf(1L), Long.valueOf(Integer.MAX_VALUE), name, repositoryType, status);
        ExcelUtil.exportExcel(page.getRecords(), "Algorithm Repositories", AlgorithmRepository.class, response);
    }
}
