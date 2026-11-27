package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmRepositoryMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SharedAlgorithmCatalog {
    private final VlsAlgorithmMapper algorithms;
    private final VlsAlgorithmRepositoryMapper repositories;
    private final boolean multiTenant;

    public SharedAlgorithmCatalog(VlsAlgorithmMapper algorithms, VlsAlgorithmRepositoryMapper repositories,
                                  @Value("${token.tenant-type:${token.tenantType:single}}") String tenantType) {
        this.algorithms = algorithms;
        this.repositories = repositories;
        this.multiTenant = TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tenantType);
    }

    public boolean available() {
        return multiTenant && TenantContextHolder.getTenantId() != null;
    }

    public List<AlgorithmRepository> categories(List<AlgorithmRepository> owned) {
        if (!available()) return owned;
        Map<Long, Long> aliases = owned.stream().filter(row -> row.getSharedCatalogId() != null)
            .collect(Collectors.toMap(AlgorithmRepository::getId, AlgorithmRepository::getSharedCatalogId));
        List<AlgorithmRepository> result = owned.stream().filter(row -> row.getSharedCatalogId() == null)
            .collect(Collectors.toList());
        for (AlgorithmRepository row : result) {
            row.setParentId(aliases.getOrDefault(row.getParentId(), row.getParentId()));
        }
        result.addAll(repositories.selectSharedCatalogs());
        result.sort(Comparator.comparing(AlgorithmRepository::getSortOrder, Comparator.nullsFirst(Integer::compareTo))
            .thenComparing(AlgorithmRepository::getId));
        return result;
    }

    public Page<Algorithm> page(Page<Algorithm> page, Set<Long> repositoryIds, String keyword, String category) {
        if (!available()) throw new ServiceException("缺少可信算法库访问上下文");
        IPage<Algorithm> result = algorithms.selectSharedCatalogPage(page, TenantContextHolder.getTenantId(),
            repositoryIds, keyword, category);
        page.setRecords(result.getRecords());
        return page;
    }

    public Algorithm forTraining(Long id) {
        if (id == null) return null;
        Algorithm owned = algorithms.selectById(id);
        return owned == null ? visible(id) : owned;
    }

    public Algorithm visible(Long id) {
        return available() && id != null ? algorithms.selectSharedCatalogAlgorithm(id, TenantContextHolder.getTenantId()) : null;
    }

    public boolean assignable(Long id) {
        return available() && id != null && Objects.equals(id, repositories.selectAssignableSharedCatalog(id));
    }

    public boolean lockAssignable(Long id) {
        return available() && id != null && Objects.equals(id, repositories.lockAssignableSharedCatalog(id));
    }

    public Set<Long> ownedRepositoryIds(Long repositoryId) {
        Set<Long> ids = new HashSet<>();
        ids.add(repositoryId);
        if (!available()) return ids;
        AlgorithmRepository owned = repositories.selectById(repositoryId);
        Long canonicalId = owned == null ? repositoryId : owned.getSharedCatalogId();
        if (canonicalId != null && assignable(canonicalId)) {
            ids.add(canonicalId);
            repositories.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlgorithmRepository>()
                .eq(AlgorithmRepository::getSharedCatalogId, canonicalId)).forEach(row -> ids.add(row.getId()));
        }
        return ids;
    }

    public void checkAlgorithmWritable(Algorithm algorithm) {
        if (multiTenant && Boolean.TRUE.equals(algorithm.getCatalogSeed())) throw new ServiceException("系统预置算法为只读");
    }

    public void checkCatalogWritable(AlgorithmRepository repository) {
        if (multiTenant && repository.getSharedCatalogId() != null) throw new ServiceException("系统预置算法库为只读");
    }
}
