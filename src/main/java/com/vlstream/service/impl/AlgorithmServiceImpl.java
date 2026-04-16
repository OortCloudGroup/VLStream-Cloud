package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.Algorithm;
import com.vlstream.mapper.AlgorithmMapper;
import com.vlstream.service.AlgorithmRepositoryService;
import com.vlstream.service.AlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm Service Implementation Class
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmServiceImpl extends ServiceImpl<AlgorithmMapper, Algorithm> implements AlgorithmService {

    private final AlgorithmMapper algorithmMapper;
    private final AlgorithmRepositoryService algorithmRepositoryService;

    @Override
    public IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page, 
                                              Long repositoryId, 
                                              String name, 
                                              String category,
                                              String deployStatus) {
        log.info("Query algorithm list by page, parameters: repositoryId={}, name={}, category={}, deployStatus={}",
                repositoryId, name, category, deployStatus);
        return algorithmMapper.selectAlgorithmPage(page, repositoryId, name, category, deployStatus);
    }

    @Override
    public List<Algorithm> getByRepositoryId(Long repositoryId) {
        log.info("Query algorithm list by repository ID: {}", repositoryId);
        return algorithmMapper.selectByRepositoryId(repositoryId);
    }

    @Override
    public List<Algorithm> getByCategory(String category) {
        log.info("Query algorithm list by category: {}", category);
        return algorithmMapper.selectByCategory(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAlgorithm(Algorithm algorithm) {
        log.info("Create algorithm: {}", algorithm.getName());
        
        // Check if name exists in the same repository
        QueryWrapper<Algorithm> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("repository_id", algorithm.getRepositoryId())
                   .eq("name", algorithm.getName())
                   .eq("deleted", 0);
        if (count(queryWrapper) > 0) {
            log.warn("Algorithm name already exists in the same repository: {}", algorithm.getName());
            return false;
        }
        
        // Set default values
        if (algorithm.getVersion() == null) {
            algorithm.setVersion("1.0.0");
        }
        if (algorithm.getDeployStatus() == null) {
            algorithm.setDeployStatus("ready");
        }
        if (algorithm.getDeployCount() == null) {
            algorithm.setDeployCount(0);
        }
        if (algorithm.getGpuRequired() == null) {
            algorithm.setGpuRequired(0);
        }
        boolean result = save(algorithm);
        
        // Update repository algorithm count
        if (result) {
            algorithmRepositoryService.updateAlgorithmCount(algorithm.getRepositoryId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAlgorithm(Algorithm algorithm) {
        log.info("Update algorithm: ID={}, Name={}", algorithm.getId(), algorithm.getName());
        
        // Get original algorithm information
        Algorithm existing = getById(algorithm.getId());
        if (existing == null) {
            log.warn("Algorithm does not exist: ID={}", algorithm.getId());
            return false;
        }
        
        // If repository changes, need to update algorithm count for both repositories
        Long oldRepositoryId = existing.getRepositoryId();
        Long newRepositoryId = algorithm.getRepositoryId();
        boolean result = updateById(algorithm);
        
        // Update algorithm count
        if (result && !oldRepositoryId.equals(newRepositoryId)) {
            algorithmRepositoryService.updateAlgorithmCount(oldRepositoryId);
            algorithmRepositoryService.updateAlgorithmCount(newRepositoryId);
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAlgorithm(Long id) {
        log.info("Delete algorithm: ID={}", id);
        
        Algorithm algorithm = getById(id);
        if (algorithm == null) {
            log.warn("Algorithm does not exist: ID={}", id);
            return false;
        }
        
        boolean result = removeById(id);
        
        // Update repository algorithm count
        if (result) {
            algorithmRepositoryService.updateAlgorithmCount(algorithm.getRepositoryId());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteAlgorithms(List<Long> ids) {
        log.info("Batch delete algorithms: IDs={}", ids);
        
        // Get repository information of algorithms to be deleted
        List<Algorithm> algorithms = listByIds(ids);
        Map<Long, Boolean> repositoryMap = new HashMap<>();
        algorithms.forEach(algo -> repositoryMap.put(algo.getRepositoryId(), true));
        
        boolean result = removeByIds(ids);
        
        // Update algorithm count for related repositories
        if (result) {
            repositoryMap.keySet().forEach(algorithmRepositoryService::updateAlgorithmCount);
        }
        
        return result;
    }

    @Override
    public boolean updateDeployStatus(Long id, String deployStatus) {
        log.info("Update algorithm deployment status: ID={}, Status={}", id, deployStatus);
        
        UpdateWrapper<Algorithm> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                    .set("deploy_status", deployStatus);
        
        // If deployment is successful, increment deployment count and update deployment time
        if ("deployed".equals(deployStatus)) {
            updateWrapper.setSql("deploy_count = deploy_count + 1")
                        .set("last_deploy_time", LocalDateTime.now());
        }
        
        return update(updateWrapper);
    }

    @Override
    public boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus) {
        log.info("Batch update algorithm deployment status: IDs={}, Status={}", ids, deployStatus);
        
        UpdateWrapper<Algorithm> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids)
                    .set("deploy_status", deployStatus);
        
        // If deployment is successful, increment deployment count and update deployment time
        if ("deployed".equals(deployStatus)) {
            updateWrapper.setSql("deploy_count = deploy_count + 1")
                        .set("last_deploy_time", LocalDateTime.now());
        }
        
        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds) {
        log.info("Deploy algorithm to devices: AlgorithmId={}, DeviceIds={}", algorithmId, deviceIds);
        
        // Update algorithm deployment status to deploying
        updateDeployStatus(algorithmId, "deploying");
        
        try {
            // Here should call actual deployment service
            // Simulate deployment process
            Thread.sleep(1000);
            
            // Deployment successful, update status
            updateDeployStatus(algorithmId, "deployed");
            
            log.info("Algorithm deployed successfully: AlgorithmId={}", algorithmId);
            return true;
            
        } catch (Exception e) {
            log.error("Algorithm deployment failed: AlgorithmId={}", algorithmId, e);
            
            // Deployment failed, update status
            updateDeployStatus(algorithmId, "failed");
            return false;
        }
    }

    @Override
    public Long countByRepositoryId(Long repositoryId) {
        log.info("Count algorithms in repository: RepositoryId={}", repositoryId);
        return algorithmMapper.countByRepositoryId(repositoryId);
    }

    @Override
    public List<Map<String, Object>> getCategoryStatistics() {
        log.info("Get algorithm category statistics");
        return algorithmMapper.selectCategoryStatistics();
    }

    @Override
    public List<Map<String, Object>> getTypeStatistics() {
        log.info("Get algorithm type statistics");
        return algorithmMapper.selectTypeStatistics();
    }

    @Override
    public List<Map<String, Object>> getDeployStatusStatistics() {
        log.info("Get deployment status statistics");
        return algorithmMapper.selectDeployStatusStatistics();
    }

    @Override
    public Map<String, Object> evaluateAlgorithm(Long algorithmId) {
        log.info("Evaluate algorithm: AlgorithmId={}", algorithmId);
        
        Algorithm algorithm = getById(algorithmId);
        if (algorithm == null) {
            log.warn("Algorithm does not exist: ID={}", algorithmId);
            return null;
        }
        
        // Simulate algorithm evaluation process
        Map<String, Object> result = new HashMap<>();
        result.put("algorithmId", algorithmId);
        result.put("algorithmName", algorithm.getName());
        result.put("accuracy", 0.95);
        result.put("precision", 0.92);
        result.put("recall", 0.89);
        result.put("f1Score", 0.905);
        result.put("evaluationTime", LocalDateTime.now());
        result.put("status", "completed");
        
        log.info("Algorithm evaluation completed: AlgorithmId={}, Result={}", algorithmId, result);
        return result;
    }
} 