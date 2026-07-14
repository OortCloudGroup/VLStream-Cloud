/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

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

import java.util.List;

/**
 * Algorithm repository routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmRepository")
public class VlsAlgorithmRepositoryController {

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
        return BladeResult.success(repositoryService.deleteRepository(id));
    }

    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteRepositories(@RequestBody List<Long> ids) {
        return BladeResult.success(repositoryService.deleteRepositories(ids));
    }

    @PutMapping("/{id}/status")
    public BladeResult<Boolean> updateRepositoryStatus(@PathVariable Long id,
                                                       @RequestParam String status) {
        return BladeResult.success(repositoryService.updateRepositoryStatus(id, status));
    }

    @PutMapping("/batch/status")
    public BladeResult<Boolean> batchUpdateRepositoryStatus(@RequestBody List<Long> ids,
                                                            @RequestParam String status) {
        return BladeResult.success(repositoryService.updateRepositoryStatus(ids, status));
    }

    @GetMapping("/count")
    public BladeResult<Long> countRepositories() {
        return BladeResult.success(repositoryService.countRepositories());
    }

    @PutMapping("/{id}/refresh-count")
    public BladeResult<Boolean> refreshAlgorithmCount(@PathVariable Long id) {
        return BladeResult.success(repositoryService.refreshAlgorithmCount(id));
    }
}
