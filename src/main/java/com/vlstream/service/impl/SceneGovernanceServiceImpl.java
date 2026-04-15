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
 * 场景治理服务层实现类
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
            // 检查场景名称是否已存在
            if (checkSceneNameExists(sceneGovernance.getName(), null)) {
                log.error("场景名称已存在: {}", sceneGovernance.getName());
                return false;
            }
            
            // 设置默认值
            setDefaultValues(sceneGovernance);
            
            return save(sceneGovernance);
        } catch (Exception e) {
            log.error("新增场景治理失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSceneGovernance(SceneGovernance sceneGovernance) {
        try {
            // 检查场景名称是否已存在（排除自己）
            if (checkSceneNameExists(sceneGovernance.getName(), sceneGovernance.getId())) {
                log.error("场景名称已存在: {}", sceneGovernance.getName());
                return false;
            }
            
            return updateById(sceneGovernance);
        } catch (Exception e) {
            log.error("更新场景治理失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSceneGovernance(Long id) {
        try {
            // 软删除：设置deleted字段为1
            SceneGovernance sceneGovernance = new SceneGovernance();
            sceneGovernance.setId(id);
            sceneGovernance.setDeleted(1);
            return updateById(sceneGovernance);
        } catch (Exception e) {
            log.error("删除场景治理失败，ID: {}", id, e);
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
            log.error("批量删除场景治理失败", e);
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
            log.error("更新场景治理状态失败，ID: {}, 状态: {}", id, status, e);
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
            log.error("批量更新场景治理状态失败", e);
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
            log.error("获取场景治理统计信息失败", e);
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
                result.put("message", "场景不存在");
                return result;
            }
            
            if (!"enabled".equals(sceneGovernance.getStatus())) {
                result.put("success", false);
                result.put("message", "场景未启用");
                return result;
            }
            
            // TODO: 实现实际的场景治理执行逻辑
            log.info("执行场景治理: {}", sceneGovernance.getName());
            
            result.put("success", true);
            result.put("message", "场景治理执行成功");
            result.put("executeTime", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("执行场景治理失败，ID: {}", id, e);
            result.put("success", false);
            result.put("message", "执行失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> validateSceneGovernance(SceneGovernance sceneGovernance) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        // 验证场景名称
        if (StringUtils.isBlank(sceneGovernance.getName())) {
            errors.add("场景名称不能为空");
        } else if (sceneGovernance.getName().length() > 100) {
            errors.add("场景名称长度不能超过100个字符");
        }
        
        // 验证状态
        if (StringUtils.isNotBlank(sceneGovernance.getStatus()) && 
            !"enabled".equals(sceneGovernance.getStatus()) && 
            !"disabled".equals(sceneGovernance.getStatus())) {
            errors.add("状态值无效，必须为enabled或disabled");
        }
        
        // 验证执行类型
        if (StringUtils.isNotBlank(sceneGovernance.getExecuteType()) && 
            !"daily".equals(sceneGovernance.getExecuteType()) && 
            !"alternate".equals(sceneGovernance.getExecuteType()) && 
            !"weekly".equals(sceneGovernance.getExecuteType()) && 
            !"monthly".equals(sceneGovernance.getExecuteType())) {
            errors.add("执行类型无效");
        }
        
        // 验证间隔数量
        if (sceneGovernance.getIntervalNum() != null && sceneGovernance.getIntervalNum() < 1) {
            errors.add("间隔数量必须大于等于1");
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
                // 验证数据
                Map<String, Object> validateResult = validateSceneGovernance(sceneGovernance);
                if (!(Boolean) validateResult.get("valid")) {
                    failureList.add(sceneGovernance.getName() + ": " + validateResult.get("errors"));
                    continue;
                }
                
                // 设置默认值
                setDefaultValues(sceneGovernance);
                
                if (save(sceneGovernance)) {
                    successList.add(sceneGovernance.getName());
                } else {
                    failureList.add(sceneGovernance.getName() + ": 保存失败");
                }
            } catch (Exception e) {
                log.error("导入场景治理失败: {}", sceneGovernance.getName(), e);
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
        // TODO: 实现执行历史查询逻辑
        List<Map<String, Object>> history = new ArrayList<>();
        
        Map<String, Object> record = new HashMap<>();
        record.put("executeTime", LocalDateTime.now());
        record.put("status", "success");
        record.put("message", "执行成功");
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
                log.error("源场景不存在，ID: {}", id);
                return false;
            }
            
            // 检查新名称是否已存在
            if (checkSceneNameExists(name, null)) {
                log.error("场景名称已存在: {}", name);
                return false;
            }
            
            // 创建副本
            SceneGovernance copy = new SceneGovernance();
            copy.setName(name);
            copy.setDescription(original.getDescription());
            copy.setDevices(original.getDevices());
            copy.setRules(original.getRules());
            copy.setStatus("disabled"); // 新复制的场景默认为禁用状态
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
            log.error("复制场景治理失败，ID: {}, 新名称: {}", id, name, e);
            return false;
        }
    }

    /**
     * 设置默认值
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
        
        // 设置默认的选择天数
        if (StringUtils.isBlank(sceneGovernance.getSelectedDays())) {
            sceneGovernance.setSelectedDays("[\"monday\"]");
        }
    }
} 