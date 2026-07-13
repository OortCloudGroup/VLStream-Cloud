/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmRepository;
import com.ruoyi.vlstream.mapper.VlsAlgorithmRepositoryMapper;
import com.ruoyi.vlstream.service.IVlsAlgorithmRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for the VLS algorithm repository frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsAlgorithmRepositoryServiceImpl implements IVlsAlgorithmRepositoryService {

    private final VlsAlgorithmRepositoryMapper algorithmRepositoryMapper;

    @Override
    public BladePage<AlgorithmRepository> getRepositoryPage(Long current, Long size, String name,
                                                            String repositoryType, String status) {
        Page<AlgorithmRepository> page = new Page<AlgorithmRepository>(
            current == null || current < 1L ? 1L : current,
            size == null || size < 1L ? 10L : size);
        LambdaQueryWrapper<AlgorithmRepository> wrapper = new LambdaQueryWrapper<AlgorithmRepository>();
        if (StringUtils.hasText(name)) {
            wrapper.like(AlgorithmRepository::getName, name.trim());
        }
        if (StringUtils.hasText(repositoryType)) {
            wrapper.eq(AlgorithmRepository::getRepositoryType, repositoryType.trim());
        }
        Integer statusValue = toStatusValue(status);
        if (statusValue != null) {
            wrapper.eq(AlgorithmRepository::getStatus, statusValue);
        }
        wrapper.orderByDesc(AlgorithmRepository::getCreateTime).orderByDesc(AlgorithmRepository::getId);
        Page<AlgorithmRepository> result = algorithmRepositoryMapper.selectPage(page, wrapper);
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public List<AlgorithmRepository> getEnabledRepositories() {
        return algorithmRepositoryMapper.selectEnabledRepositories();
    }

    @Override
    public List<AlgorithmRepository> getRepositoriesByType(String repositoryType) {
        return algorithmRepositoryMapper.selectByRepositoryType(repositoryType);
    }

    @Override
    public AlgorithmRepository getRepositoryById(Long id) {
        return algorithmRepositoryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmRepository createRepository(AlgorithmRepository repository) {
        if (repository == null || !StringUtils.hasText(repository.getName())) {
            throw new IllegalArgumentException("Algorithm repository name is required");
        }
        LambdaQueryWrapper<AlgorithmRepository> wrapper = new LambdaQueryWrapper<AlgorithmRepository>()
            .eq(AlgorithmRepository::getName, repository.getName().trim());
        if (algorithmRepositoryMapper.selectCount(wrapper) > 0L) {
            throw new IllegalArgumentException("Algorithm repository name already exists");
        }
        repository.setName(repository.getName().trim());
        normalizeDefaults(repository);
        algorithmRepositoryMapper.insert(repository);
        return repository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmRepository updateRepository(Long id, AlgorithmRepository repository) {
        AlgorithmRepository existing = algorithmRepositoryMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Algorithm repository does not exist");
        }
        if (repository == null) {
            repository = new AlgorithmRepository();
        }
        repository.setId(id);
        if (isBasic(existing)) {
            repository.setName(existing.getName());
            repository.setRepositoryType(existing.getRepositoryType());
        }
        algorithmRepositoryMapper.updateById(repository);
        return algorithmRepositoryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRepository(Long id) {
        AlgorithmRepository existing = algorithmRepositoryMapper.selectById(id);
        return existing != null && !isBasic(existing) && algorithmRepositoryMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRepositories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<AlgorithmRepository> repositories = algorithmRepositoryMapper.selectBatchIds(ids);
        List<Long> allowedIds = new ArrayList<Long>();
        for (AlgorithmRepository repository : repositories) {
            if (!isBasic(repository)) {
                allowedIds.add(repository.getId());
            }
        }
        return !allowedIds.isEmpty() && algorithmRepositoryMapper.deleteBatchIds(allowedIds) > 0;
    }

    @Override
    public boolean updateRepositoryStatus(Long id, String status) {
        Integer statusValue = toStatusValue(status);
        if (id == null || statusValue == null) {
            return false;
        }
        AlgorithmRepository repository = new AlgorithmRepository();
        repository.setId(id);
        repository.setStatus(statusValue);
        return algorithmRepositoryMapper.updateById(repository) > 0;
    }

    @Override
    public boolean updateRepositoryStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        Integer statusValue = toStatusValue(status);
        if (statusValue == null) {
            return false;
        }
        boolean updated = false;
        for (Long id : ids) {
            updated = updateRepositoryStatus(id, status) || updated;
        }
        return updated;
    }

    @Override
    public Long countRepositories() {
        Long count = algorithmRepositoryMapper.countRepositories();
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refreshAlgorithmCount(Long id) {
        AlgorithmRepository repository = algorithmRepositoryMapper.selectById(id);
        if (repository == null) {
            return false;
        }
        Long count = algorithmRepositoryMapper.countAlgorithmsByRepositoryId(id);
        repository.setAlgorithmCount(count == null ? 0 : count.intValue());
        return algorithmRepositoryMapper.updateById(repository) > 0;
    }

    private void normalizeDefaults(AlgorithmRepository repository) {
        if (!StringUtils.hasText(repository.getTenantId())) {
            repository.setTenantId("000000");
        }
        if (repository.getAlgorithmCount() == null) {
            repository.setAlgorithmCount(0);
        }
        if (!StringUtils.hasText(repository.getRepositoryType())) {
            repository.setRepositoryType("extended");
        }
        if (repository.getStatus() == null) {
            repository.setStatus(1);
        }
        if (repository.getIsDeleted() == null) {
            repository.setIsDeleted(0);
        }
    }

    private boolean isBasic(AlgorithmRepository repository) {
        return repository != null && "basic".equalsIgnoreCase(repository.getRepositoryType());
    }

    private Integer toStatusValue(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String value = status.trim().toLowerCase();
        if ("enabled".equals(value) || "enable".equals(value) || "true".equals(value) || "1".equals(value)) {
            return 1;
        }
        if ("disabled".equals(value) || "disable".equals(value) || "false".equals(value) || "0".equals(value)) {
            return 0;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
