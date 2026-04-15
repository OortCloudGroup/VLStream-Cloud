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
 * 算法仓库服务实现类
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
        log.info("分页查询算法仓库列表，参数：name={}, repositoryType={}, status={}", name, repositoryType, status);
        return algorithmRepositoryMapper.selectRepositoryPage(page, name, repositoryType, status);
    }

    @Override
    public List<AlgorithmRepository> getEnabledRepositories() {
        log.info("查询所有启用的算法仓库");
        return algorithmRepositoryMapper.selectEnabledRepositories();
    }

    @Override
    public List<AlgorithmRepository> getByRepositoryType(String repositoryType) {
        log.info("根据类型查询算法仓库：{}", repositoryType);
        return algorithmRepositoryMapper.selectByRepositoryType(repositoryType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRepository(AlgorithmRepository repository) {
        log.info("创建算法仓库：{}", repository.getName());
        
        // 检查名称是否重复
        QueryWrapper<AlgorithmRepository> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", repository.getName())
                   .eq("deleted", 0);
        if (count(queryWrapper) > 0) {
            log.warn("算法仓库名称已存在：{}", repository.getName());
            return false;
        }
        
        // 设置默认值
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
        log.info("更新算法仓库：ID={}, Name={}", repository.getId(), repository.getName());
        
        // 检查是否为基础预置算法库（不允许修改某些字段）
        AlgorithmRepository existing = getById(repository.getId());
        if (existing != null && "basic".equals(existing.getRepositoryType())) {
            // 基础预置算法库只允许修改备注和状态
            repository.setName(existing.getName());
            repository.setRepositoryType(existing.getRepositoryType());
        }
        
        return updateById(repository);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRepository(Long id) {
        log.info("删除算法仓库：ID={}", id);
        
        // 检查是否为基础预置算法库（不允许删除）
        AlgorithmRepository repository = getById(id);
        if (repository != null && "basic".equals(repository.getRepositoryType())) {
            log.warn("不允许删除基础预置算法库：ID={}", id);
            return false;
        }
        
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteRepositories(List<Long> ids) {
        log.info("批量删除算法仓库：IDs={}", ids);
        
        // 过滤掉基础预置算法库
        List<AlgorithmRepository> repositories = listByIds(ids);
        List<Long> allowedIds = repositories.stream()
                .filter(repo -> !"basic".equals(repo.getRepositoryType()))
                .map(AlgorithmRepository::getId)
                .collect(Collectors.toList());
        
        if (allowedIds.isEmpty()) {
            log.warn("没有可删除的算法仓库");
            return false;
        }
        
        return removeByIds(allowedIds);
    }

    @Override
    public boolean updateRepositoryStatus(Long id, String status) {
        log.info("更新算法仓库状态：ID={}, Status={}", id, status);
        
        UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                    .set("status", status);
        
        return update(updateWrapper);
    }

    @Override
    public boolean batchUpdateRepositoryStatus(List<Long> ids, String status) {
        log.info("批量更新算法仓库状态：IDs={}, Status={}", ids, status);
        
        UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids)
                    .set("status", status);
        
        return update(updateWrapper);
    }

    @Override
    public Long countRepositories() {
        log.info("统计算法仓库数量");
        return algorithmRepositoryMapper.countRepositories();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlgorithmCount(Long repositoryId) {
        log.info("更新算法仓库的算法数量：ID={}", repositoryId);
        
        Long count = algorithmMapper.countByRepositoryId(repositoryId);
        
        UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", repositoryId)
                    .set("algorithm_count", count);
        
        update(updateWrapper);
    }
} 