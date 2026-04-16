package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.SceneGovernance;
import com.vlstream.mapper.SceneGovernanceMapper;
import com.vlstream.service.SceneGovernanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scene Governance Service Implementation
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
@Transactional
public class SceneGovernanceServiceImpl extends ServiceImpl<SceneGovernanceMapper, SceneGovernance> implements SceneGovernanceService {

    @Override
    public IPage<SceneGovernance> getSceneGovernancePage(Page<SceneGovernance> page, String name, String status, String startDate, String endDate) {
        QueryWrapper<SceneGovernance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("created_at", startDate);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("created_at", endDate);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        return this.page(page, queryWrapper);
    }

    @Override
    public SceneGovernance getByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return baseMapper.selectByName(name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSceneGovernance(SceneGovernance sceneGovernance) {
        try {
            // Check if scene name already exists
            if (checkSceneNameExists(sceneGovernance.getName(), null)) {
                log.error("Scene name already exists: {}", sceneGovernance.getName());
                return false;
            }
            
            // Set default values
            setDefaultValues(sceneGovernance);
            
            return save(sceneGovernance);
        } catch (Exception e) {
            log.error("Failed to add scene governance", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSceneGovernance(SceneGovernance sceneGovernance) {
        try {
            // Check if scene name already exists (excluding itself)
            if (checkSceneNameExists(sceneGovernance.getName(), sceneGovernance.getId())) {
                log.error("Scene name already exists: {}", sceneGovernance.getName());
                return false;
            }
            
            return updateById(sceneGovernance);
        } catch (Exception e) {
            log.error("Failed to update scene governance", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSceneGovernance(Long id) {
        try {
            // Soft delete: set deleted field to 1
            SceneGovernance sceneGovernance = new SceneGovernance();
            sceneGovernance.setId(id);
            sceneGovernance.setDeleted(1);
            return updateById(sceneGovernance);
        } catch (Exception e) {
            log.error("Failed to delete scene governance, ID: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSceneGovernanceBatch(List<Long> ids) {
        try {
            List<SceneGovernance> updateList = new ArrayList<>();
            
            for (Long id : ids) {
                SceneGovernance sceneGovernance = new SceneGovernance();
                sceneGovernance.setId(id);
                sceneGovernance.setDeleted(1);
                updateList.add(sceneGovernance);
            }
            
            return updateBatchById(updateList);
        } catch (Exception e) {
            log.error("Failed to batch delete scene governance", e);
            return false;
        }
    }

    @Override
    public boolean updateSceneGovernanceStatus(Long id, String status) {
        try {
            SceneGovernance sceneGovernance = new SceneGovernance();
            sceneGovernance.setId(id);
            sceneGovernance.setStatus(status);
            return updateById(sceneGovernance);
        } catch (Exception e) {
            log.error("Failed to update scene governance status, ID: {}, status: {}", id, status, e);
            return false;
        }
    }

    @Override
    public boolean updateSceneGovernanceStatusBatch(List<Long> ids, String status) {
        try {
            UpdateWrapper<SceneGovernance> updateWrapper = new UpdateWrapper<>();
            updateWrapper.in("id", ids);
            updateWrapper.eq("deleted", 0);
            updateWrapper.set("status", status);
            return this.update(updateWrapper);
        } catch (Exception e) {
            log.error("Failed to batch update scene governance status", e);
            return false;
        }
    }

    @Override
    public List<SceneGovernance> getSceneGovernancesByStatus(String status) {
        return baseMapper.selectByStatus(status);
    }

    @Override
    public List<SceneGovernance> getSceneGovernancesByExecuteType(String executeType) {
        QueryWrapper<SceneGovernance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("execute_type", executeType);
        queryWrapper.eq("deleted", 0);
        queryWrapper.orderByDesc("created_at");
        return this.list(queryWrapper);
    }

    @Override
    public boolean checkSceneNameExists(String name, Long id) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        QueryWrapper<SceneGovernance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        queryWrapper.eq("deleted", 0);
        
        if (id != null) {
            queryWrapper.ne("id", id);
        }
        
        return this.count(queryWrapper) > 0;
    }

    @Override
    public Map<String, Object> getSceneGovernanceStatistics() {
        try {
            Long totalCount = baseMapper.getTotalCount();
            Long enabledCount = baseMapper.getEnabledCount();
            Long disabledCount = baseMapper.getDisabledCount();
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", totalCount);
            result.put("enabled", enabledCount);
            result.put("disabled", disabledCount);
            
            return result;
        } catch (Exception e) {
            log.error("Failed to get scene governance statistics", e);
            return new HashMap<>();
        }
    }

    @Override
    public List<String> getAllExecuteTypes() {
        QueryWrapper<SceneGovernance> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("execute_type");
        queryWrapper.ne("execute_type", "");
        queryWrapper.eq("deleted", 0);
        queryWrapper.select("DISTINCT execute_type");
        
        List<SceneGovernance> list = this.list(queryWrapper);
        return list.stream()
                   .map(SceneGovernance::getExecuteType)
                   .distinct()
                   .collect(Collectors.toList());
    }

    @Override
    public boolean enableSceneGovernance(Long id) {
        return updateSceneGovernanceStatus(id, "enabled");
    }

    @Override
    public boolean disableSceneGovernance(Long id) {
        return updateSceneGovernanceStatus(id, "disabled");
    }

    @Override
    public boolean enableSceneGovernanceBatch(List<Long> ids) {
        return updateSceneGovernanceStatusBatch(ids, "enabled");
    }

    @Override
    public boolean disableSceneGovernanceBatch(List<Long> ids) {
        return updateSceneGovernanceStatusBatch(ids, "disabled");
    }

    @Override
    public Map<String, Object> executeSceneGovernance(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            SceneGovernance sceneGovernance = getById(id);
            if (sceneGovernance == null) {
                result.put("success", false);
                result.put("message", "Scene does not exist");
                return result;
            }
            
            if (!"enabled".equals(sceneGovernance.getStatus())) {
                result.put("success", false);
                result.put("message", "Scene is not enabled");
                return result;
            }
            
            // TODO: Implement actual scene governance execution logic
            log.info("Executing scene governance: {}", sceneGovernance.getName());
            
            result.put("success", true);
            result.put("message", "Scene governance executed successfully");
            result.put("executeTime", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Failed to execute scene governance, ID: {}", id, e);
            result.put("success", false);
            result.put("message", "Execution failed: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> validateSceneGovernance(SceneGovernance sceneGovernance) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        // Validate scene name
        if (StringUtils.isBlank(sceneGovernance.getName())) {
            errors.add("Scene name cannot be empty");
        } else if (sceneGovernance.getName().length() > 100) {
            errors.add("Scene name length cannot exceed 100 characters");
        }
        
        // Validate status
        if (StringUtils.isNotBlank(sceneGovernance.getStatus()) && 
            !"enabled".equals(sceneGovernance.getStatus()) && 
            !"disabled".equals(sceneGovernance.getStatus())) {
            errors.add("Invalid status value, must be enabled or disabled");
        }
        
        // Validate execution type
        if (StringUtils.isNotBlank(sceneGovernance.getExecuteType()) && 
            !"daily".equals(sceneGovernance.getExecuteType()) && 
            !"alternate".equals(sceneGovernance.getExecuteType()) && 
            !"weekly".equals(sceneGovernance.getExecuteType()) && 
            !"monthly".equals(sceneGovernance.getExecuteType())) {
            errors.add("Invalid execution type");
        }
        
        // Validate interval number
        if (sceneGovernance.getIntervalNum() != null && sceneGovernance.getIntervalNum() < 1) {
            errors.add("Interval number must be greater than or equal to 1");
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        
        return result;
    }

    @Override
    public List<SceneGovernance> exportSceneGovernances(List<Long> sceneIds) {
        if (sceneIds == null || sceneIds.isEmpty()) {
            return list();
        } else {
            return listByIds(sceneIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportSceneGovernances(List<SceneGovernance> sceneGovernanceList) {
        Map<String, Object> result = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> failureList = new ArrayList<>();
        
        for (SceneGovernance sceneGovernance : sceneGovernanceList) {
            try {
                // Validate data
                Map<String, Object> validateResult = validateSceneGovernance(sceneGovernance);
                if (!(Boolean) validateResult.get("valid")) {
                    failureList.add(sceneGovernance.getName() + ": " + validateResult.get("errors"));
                    continue;
                }
                
                // Set default values
                setDefaultValues(sceneGovernance);
                
                if (save(sceneGovernance)) {
                    successList.add(sceneGovernance.getName());
                } else {
                    failureList.add(sceneGovernance.getName() + ": Save failed");
                }
            } catch (Exception e) {
                log.error("Failed to import scene governance: {}", sceneGovernance.getName(), e);
                failureList.add(sceneGovernance.getName() + ": " + e.getMessage());
            }
        }
        
        result.put("totalCount", sceneGovernanceList.size());
        result.put("successCount", successList.size());
        result.put("failureCount", failureList.size());
        result.put("successList", successList);
        result.put("failureList", failureList);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getSceneGovernanceExecuteHistory(Long id) {
        // TODO: Implement execution history query logic
        List<Map<String, Object>> history = new ArrayList<>();
        
        Map<String, Object> record = new HashMap<>();
        record.put("executeTime", LocalDateTime.now());
        record.put("status", "success");
        record.put("message", "Execution successful");
        record.put("duration", "1.2s");
        history.add(record);
        
        return history;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copySceneGovernance(Long id, String name) {
        try {
            SceneGovernance original = getById(id);
            if (original == null) {
                log.error("Source scene does not exist, ID: {}", id);
                return false;
            }
            
            // Check if new name already exists
            if (checkSceneNameExists(name, null)) {
                log.error("Scene name already exists: {}", name);
                return false;
            }
            
            // Create copy
            SceneGovernance copy = new SceneGovernance();
            copy.setName(name);
            copy.setDescription(original.getDescription());
            copy.setDevices(original.getDevices());
            copy.setRules(original.getRules());
            copy.setStatus("disabled"); // New copied scene is disabled by default
            copy.setExecuteType(original.getExecuteType());
            copy.setSelectedDays(original.getSelectedDays());
            copy.setIntervalNum(original.getIntervalNum());
            copy.setAlgorithm(original.getAlgorithm());
            copy.setLocation(original.getLocation());
            copy.setCameras(original.getCameras());
            copy.setStartTime(original.getStartTime());
            copy.setEndTime(original.getEndTime());
            
            setDefaultValues(copy);
            
            return save(copy);
        } catch (Exception e) {
            log.error("Failed to copy scene governance, ID: {}, new name: {}", id, name, e);
            return false;
        }
    }

    /**
     * Set default values
     */
    private void setDefaultValues(SceneGovernance sceneGovernance) {
        if (StringUtils.isBlank(sceneGovernance.getStatus())) {
            sceneGovernance.setStatus("enabled");
        }
        
        if (StringUtils.isBlank(sceneGovernance.getExecuteType())) {
            sceneGovernance.setExecuteType("weekly");
        }
        
        if (sceneGovernance.getIntervalNum() == null) {
            sceneGovernance.setIntervalNum(1);
        }
        
        if (StringUtils.isBlank(sceneGovernance.getDevices())) {
            sceneGovernance.setDevices("-");
        }
        
        if (StringUtils.isBlank(sceneGovernance.getRules())) {
            sceneGovernance.setRules("-");
        }
        
        // Set default selected days
        if (StringUtils.isBlank(sceneGovernance.getSelectedDays())) {
            sceneGovernance.setSelectedDays("[\"monday\"]");
        }
    }
} 