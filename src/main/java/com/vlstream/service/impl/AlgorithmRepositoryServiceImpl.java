package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.AlgorithmRepository;
import com.vlstream.mapper.AlgorithmMapper;
import com.vlstream.mapper.AlgorithmRepositoryMapper;
import com.vlstream.service.AlgorithmRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Algorithm Repository Service Implementation Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmRepositoryServiceImpl extends ServiceImpl<AlgorithmRepositoryMapper, AlgorithmRepository> 
        implements AlgorithmRepositoryService {

    private final AlgorithmRepositoryMapper algorithmRepositoryMapper;
    private final AlgorithmMapper algorithmMapper;

    @Override
    public IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page, 
                                                         String name, 
                                                         String repositoryType, 
                                                         String status) {
        log.info("Query algorithm repository list by page, parameters: name={}, repositoryType={}, status={}", name, repositoryType, status);
        return algorithmRepositoryMapper.selectRepositoryPage(page, name, repositoryType, status);
    }

    @Override
    public List<AlgorithmRepository> getEnabledRepositories() {
        log.info("Query all enabled algorithm repositories");
        return algorithmRepositoryMapper.selectEnabledRepositories();
    }

    @Override
    public List<AlgorithmRepository> getByRepositoryType(String repositoryType) {
        log.info("Query algorithm repository by type: {}", repositoryType);
        return algorithmRepositoryMapper.selectByRepositoryType(repositoryType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRepository(AlgorithmRepository repository) {
        log.info("Create algorithm repository: {}", repository.getName());
        
        // Check if name exists
        QueryWrapper<AlgorithmRepository> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", repository.getName())
                   .eq("deleted", 0);
        if (count(queryWrapper) > 0) {
            log.warn("Algorithm repository name already exists: {}", repository.getName());
            return false;
        }
        
        // Set default values
        if (repository.getAlgorithmCount() == null) {
            repository.setAlgorithmCount(0);
        }
        if (repository.getRepositoryType() == null) {
            repository.setRepositoryType("extended");
        }
        if (repository.getStatus() == null) {
            repository.setStatus("enabled");
        }
        
        return save(repository);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRepository(AlgorithmRepository repository) {
        log.info("Update algorithm repository: ID={}, Name={}", repository.getId(), repository.getName());
        
        // Check if it is a basic preset algorithm repository (certain fields cannot be modified)
        AlgorithmRepository existing = getById(repository.getId());
        if (existing != null && "basic".equals(existing.getRepositoryType())) {
            // Basic preset algorithm repository only allows modification of remarks and status
            repository.setName(existing.getName());
            repository.setRepositoryType(existing.getRepositoryType());
        }
        
        return updateById(repository);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRepository(Long id) {
        log.info("Delete algorithm repository: ID={}", id);
        
        // Check if it is a basic preset algorithm repository (cannot be deleted)
        AlgorithmRepository repository = getById(id);
        if (repository != null && "basic".equals(repository.getRepositoryType())) {
            log.warn("Cannot delete basic preset algorithm repository: ID={}", id);
            return false;
        }
        
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteRepositories(List<Long> ids) {
        log.info("Batch delete algorithm repositories: IDs={}", ids);
        
        // Filter out basic preset algorithm repositories
        List<AlgorithmRepository> repositories = listByIds(ids);
        List<Long> allowedIds = repositories.stream()
                .filter(repo -> !"basic".equals(repo.getRepositoryType()))
                .map(AlgorithmRepository::getId)
                .collect(Collectors.toList());
        
        if (allowedIds.isEmpty()) {
            log.warn("No algorithm repositories available for deletion");
            return false;
        }
        
        return removeByIds(allowedIds);
    }

    @Override
    public boolean updateRepositoryStatus(Long id, String status) {
        log.info("Update algorithm repository status: ID={}, Status={}", id, status);
        
        UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                    .set("status", status);
        
        return update(updateWrapper);
    }

    @Override
    public boolean batchUpdateRepositoryStatus(List<Long> ids, String status) {
        log.info("Batch update algorithm repository status: IDs={}, Status={}", ids, status);
        
        UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids)
                    .set("status", status);
        
        return update(updateWrapper);
    }

    @Override
    public Long countRepositories() {
        log.info("Count algorithm repositories");
        return algorithmRepositoryMapper.countRepositories();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlgorithmCount(Long repositoryId) {
        log.info("Update algorithm count for algorithm repository: ID={}", repositoryId);
        
        Long count = algorithmMapper.countByRepositoryId(repositoryId);
        
        UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", repositoryId)
                    .set("algorithm_count", count);
        
        update(updateWrapper);
    }
} 