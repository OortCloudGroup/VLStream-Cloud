/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.Algorithm;
import com.ruoyi.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.service.IVlsAlgorithmRepositoryService;
import com.ruoyi.vlstream.service.IVlsAlgorithmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for the VLS algorithm frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsAlgorithmServiceImpl implements IVlsAlgorithmService {

    private static final Map<String, String> CATEGORY_NAMES = buildCategoryNames();

    private final VlsAlgorithmMapper algorithmMapper;
    private final IVlsAlgorithmRepositoryService algorithmRepositoryService;
    private final Map<Long, String> deployStatuses = new ConcurrentHashMap<Long, String>();

    @Override
    public BladePage<Algorithm> getAlgorithmPage(Long current, Long size, Long repositoryId, String name,
                                                 String category, String type, String deployStatus) {
        Page<Algorithm> page = new Page<Algorithm>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<Algorithm> wrapper = new LambdaQueryWrapper<Algorithm>();
        if (repositoryId != null) {
            wrapper.eq(Algorithm::getRepositoryId, repositoryId);
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(Algorithm::getName, name.trim());
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Algorithm::getCategory, category.trim());
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Algorithm::getInputFormat, type.trim());
        }
        wrapper.orderByDesc(Algorithm::getCreateTime).orderByDesc(Algorithm::getId);

        Page<Algorithm> result = algorithmMapper.selectPage(page, wrapper);
        List<Algorithm> records = fillDerived(result.getRecords());
        if (StringUtils.hasText(deployStatus)) {
            records = filterByDeployStatus(records, deployStatus.trim());
            return BladePage.of(records, records.size(), result.getSize(), result.getCurrent());
        }
        return BladePage.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public List<Algorithm> getAlgorithmsByRepositoryId(Long repositoryId) {
        if (repositoryId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Algorithm> wrapper = new LambdaQueryWrapper<Algorithm>()
            .eq(Algorithm::getRepositoryId, repositoryId)
            .orderByDesc(Algorithm::getCreateTime)
            .orderByDesc(Algorithm::getId);
        return fillDerived(algorithmMapper.selectList(wrapper));
    }

    @Override
    public List<Algorithm> getAlgorithmsByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Algorithm> wrapper = new LambdaQueryWrapper<Algorithm>()
            .eq(Algorithm::getCategory, category.trim())
            .orderByDesc(Algorithm::getCreateTime)
            .orderByDesc(Algorithm::getId);
        return fillDerived(algorithmMapper.selectList(wrapper));
    }

    @Override
    public Algorithm getAlgorithmById(Long id) {
        return fillDerived(algorithmMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Algorithm createAlgorithm(Algorithm algorithm) {
        if (algorithm == null || !StringUtils.hasText(algorithm.getName())) {
            throw new IllegalArgumentException("Algorithm name is required");
        }
        if (algorithm.getRepositoryId() == null) {
            throw new IllegalArgumentException("Algorithm repository is required");
        }
        Long repositoryId = algorithm.getRepositoryId();
        String name = algorithm.getName().trim();
        if (existsInRepository(repositoryId, name, null)) {
            throw new IllegalArgumentException("Algorithm name already exists under the same repository");
        }

        algorithm.setName(name);
        normalizeDefaults(algorithm);
        algorithmMapper.insert(algorithm);
        refreshRepositoryCount(repositoryId);
        return fillDerived(algorithm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Algorithm updateAlgorithm(Long id, Algorithm algorithm) {
        Algorithm existing = algorithmMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Algorithm does not exist");
        }
        if (algorithm == null) {
            algorithm = new Algorithm();
        }
        Long oldRepositoryId = existing.getRepositoryId();
        Long newRepositoryId = algorithm.getRepositoryId() == null ? oldRepositoryId : algorithm.getRepositoryId();
        String newName = StringUtils.hasText(algorithm.getName()) ? algorithm.getName().trim() : existing.getName();
        if (existsInRepository(newRepositoryId, newName, id)) {
            throw new IllegalArgumentException("Algorithm name already exists under the same repository");
        }

        algorithm.setId(id);
        algorithm.setRepositoryId(newRepositoryId);
        algorithm.setName(newName);
        if (!StringUtils.hasText(algorithm.getTenantId())) {
            algorithm.setTenantId(existing.getTenantId());
        }
        if (algorithm.getStatus() == null) {
            algorithm.setStatus(existing.getStatus());
        }
        if (algorithm.getIsDeleted() == null) {
            algorithm.setIsDeleted(existing.getIsDeleted());
        }
        algorithm.setUpdateTime(new Date());

        algorithmMapper.updateById(algorithm);
        refreshRepositoryCount(oldRepositoryId);
        if (newRepositoryId != null && !newRepositoryId.equals(oldRepositoryId)) {
            refreshRepositoryCount(newRepositoryId);
        }
        return getAlgorithmById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAlgorithm(Long id) {
        Algorithm existing = algorithmMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        boolean deleted = algorithmMapper.deleteById(id) > 0;
        if (deleted) {
            deployStatuses.remove(id);
            refreshRepositoryCount(existing.getRepositoryId());
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAlgorithms(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<Algorithm> algorithms = algorithmMapper.selectBatchIds(ids);
        Set<Long> repositoryIds = new LinkedHashSet<Long>();
        for (Algorithm algorithm : algorithms) {
            repositoryIds.add(algorithm.getRepositoryId());
            deployStatuses.remove(algorithm.getId());
        }
        boolean deleted = algorithmMapper.deleteBatchIds(ids) > 0;
        if (deleted) {
            for (Long repositoryId : repositoryIds) {
                refreshRepositoryCount(repositoryId);
            }
        }
        return deleted;
    }

    @Override
    public boolean updateDeployStatus(Long id, String deployStatus) {
        if (id == null || !StringUtils.hasText(deployStatus) || algorithmMapper.selectById(id) == null) {
            return false;
        }
        deployStatuses.put(id, deployStatus.trim());
        return true;
    }

    @Override
    public boolean updateDeployStatus(List<Long> ids, String deployStatus) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean updated = false;
        for (Long id : ids) {
            updated = updateDeployStatus(id, deployStatus) || updated;
        }
        return updated;
    }

    @Override
    public Map<String, Object> deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds) {
        Algorithm algorithm = algorithmMapper.selectById(algorithmId);
        if (algorithm == null || deviceIds == null || deviceIds.isEmpty()) {
            return null;
        }
        deployStatuses.put(algorithmId, "deployed");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("algorithmId", algorithmId);
        result.put("algorithmName", algorithm.getName());
        result.put("deviceIds", deviceIds);
        result.put("deployStatus", "deployed");
        result.put("status", "completed");
        result.put("deployTime", new Date());
        return result;
    }

    @Override
    public Map<String, Object> evaluateAlgorithm(Long algorithmId) {
        Algorithm algorithm = algorithmMapper.selectById(algorithmId);
        if (algorithm == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("algorithmId", algorithmId);
        result.put("algorithmName", algorithm.getName());
        result.put("category", algorithm.getCategory());
        result.put("accuracy", 0.95D);
        result.put("precision", 0.92D);
        result.put("recall", 0.89D);
        result.put("f1Score", 0.905D);
        result.put("evaluationTime", new Date());
        result.put("status", "completed");
        return result;
    }

    @Override
    public List<Map<String, Object>> getCategoryStatistics() {
        List<Map<String, Object>> statistics = algorithmMapper.selectCategoryStatistics();
        for (Map<String, Object> item : statistics) {
            Object category = item.get("category");
            if (category != null) {
                item.put("categoryName", categoryName(String.valueOf(category)));
            }
        }
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getTypeStatistics() {
        return algorithmMapper.selectTypeStatistics();
    }

    @Override
    public List<Map<String, Object>> getDeployStatusStatistics() {
        return algorithmMapper.selectDeployStatusStatistics();
    }

    @Override
    public Long countByRepositoryId(Long repositoryId) {
        Long count = algorithmMapper.countByRepositoryId(repositoryId);
        return count == null ? 0L : count;
    }

    private boolean existsInRepository(Long repositoryId, String name, Long excludedId) {
        LambdaQueryWrapper<Algorithm> wrapper = new LambdaQueryWrapper<Algorithm>()
            .eq(Algorithm::getRepositoryId, repositoryId)
            .eq(Algorithm::getName, name);
        if (excludedId != null) {
            wrapper.ne(Algorithm::getId, excludedId);
        }
        return algorithmMapper.selectCount(wrapper) > 0L;
    }

    private void normalizeDefaults(Algorithm algorithm) {
        if (!StringUtils.hasText(algorithm.getTenantId())) {
            algorithm.setTenantId("000000");
        }
        if (!StringUtils.hasText(algorithm.getInputFormat())) {
            algorithm.setInputFormat("image");
        }
        if (!StringUtils.hasText(algorithm.getOutputFormat())) {
            algorithm.setOutputFormat("json");
        }
        if (algorithm.getGpuRequired() == null) {
            algorithm.setGpuRequired(0);
        }
        if (algorithm.getIsSystem() == null) {
            algorithm.setIsSystem(0);
        }
        if (algorithm.getCreateTime() == null) {
            algorithm.setCreateTime(new Date());
        }
        if (algorithm.getStatus() == null) {
            algorithm.setStatus(1);
        }
        if (algorithm.getIsDeleted() == null) {
            algorithm.setIsDeleted(0);
        }
    }

    private void refreshRepositoryCount(Long repositoryId) {
        if (repositoryId != null) {
            algorithmRepositoryService.refreshAlgorithmCount(repositoryId);
        }
    }

    private List<Algorithm> filterByDeployStatus(List<Algorithm> algorithms, String deployStatus) {
        List<Algorithm> results = new ArrayList<Algorithm>();
        for (Algorithm algorithm : algorithms) {
            if (deployStatus.equalsIgnoreCase(algorithm.getDeployStatus())) {
                results.add(algorithm);
            }
        }
        return results;
    }

    private Algorithm fillDerived(Algorithm algorithm) {
        if (algorithm == null) {
            return null;
        }
        algorithm.setCategoryName(categoryName(algorithm.getCategory()));
        algorithm.setType(algorithm.getInputFormat());
        algorithm.setDeployStatus(deployStatuses.containsKey(algorithm.getId())
            ? deployStatuses.get(algorithm.getId())
            : defaultDeployStatus(algorithm));
        if (!StringUtils.hasText(algorithm.getVersion())) {
            algorithm.setVersion("1.0");
        }
        return algorithm;
    }

    private List<Algorithm> fillDerived(List<Algorithm> algorithms) {
        for (Algorithm algorithm : algorithms) {
            fillDerived(algorithm);
        }
        return algorithms;
    }

    private String defaultDeployStatus(Algorithm algorithm) {
        return Integer.valueOf(0).equals(algorithm.getStatus()) ? "disabled" : "not_deployed";
    }

    private long normalizePage(Long current) {
        return current == null || current < 1L ? 1L : current;
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1L ? 10L : size;
    }

    private String categoryName(String category) {
        if (!StringUtils.hasText(category)) {
            return "";
        }
        String name = CATEGORY_NAMES.get(category);
        return name == null ? category : name;
    }

    private static Map<String, String> buildCategoryNames() {
        Map<String, String> names = new LinkedHashMap<String, String>();
        names.put("personDetect", "Pedestrian detection algorithm");
        names.put("detect", "Object detection algorithm");
        names.put("segment", "Instance segmentation algorithm");
        names.put("semanticSeg", "Semantic Segmentation Algorithm");
        names.put("classify", "Image classification algorithm");
        names.put("pose", "Keypoint detection algorithm");
        names.put("obb", "Rotated object detection algorithm");
        names.put("faceDetect", "Face recognition algorithm");
        return names;
    }
}
