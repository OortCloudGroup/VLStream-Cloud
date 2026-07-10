package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.ContainerInstance;
import com.ruoyi.vlstream.mapper.VlsContainerInstanceMapper;
import com.ruoyi.vlstream.service.IVlsContainerInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for VLS container instance compatibility routes.
 */
@Service
@RequiredArgsConstructor
public class VlsContainerInstanceServiceImpl implements IVlsContainerInstanceService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_STARTING = "starting";
    private static final String STATUS_STOPPED = "stopped";
    private static final String STATUS_STOPPING = "stopping";
    private static final String STATUS_ERROR = "error";
    private static final String HEALTH_UNKNOWN = "unknown";
    private static final String HEALTH_UNHEALTHY = "unhealthy";

    private final VlsContainerInstanceMapper containerInstanceMapper;

    @Override
    public BladePage<ContainerInstance> getContainerInstancePage(Long current, Long size, String name, String status,
                                                                 Long algorithmId, String healthStatus,
                                                                 String startTime, String endTime) {
        Page<ContainerInstance> page = new Page<ContainerInstance>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<ContainerInstance> wrapper = baseQuery();
        applyKeywordFilter(wrapper, name);
        if (StringUtils.hasText(status)) {
            wrapper.eq(ContainerInstance::getInstanceStatus, status.trim());
        }
        if (algorithmId != null) {
            wrapper.eq(ContainerInstance::getAlgorithmId, algorithmId);
        }
        if (StringUtils.hasText(healthStatus)) {
            wrapper.eq(ContainerInstance::getHealthStatus, healthStatus.trim());
        }
        Date begin = parseDate(startTime, false);
        if (begin != null) {
            wrapper.ge(ContainerInstance::getCreateTime, begin);
        }
        Date end = parseDate(endTime, true);
        if (end != null) {
            wrapper.le(ContainerInstance::getCreateTime, end);
        }
        wrapper.orderByDesc(ContainerInstance::getCreateTime).orderByDesc(ContainerInstance::getId);
        Page<ContainerInstance> result = containerInstanceMapper.selectPage(page, wrapper);
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public ContainerInstance getContainerInstanceById(Long id) {
        return id == null ? null : containerInstanceMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContainerInstance createContainerInstance(ContainerInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Container instance is required");
        }
        normalizeDefaults(instance, true);
        if (checkContainerInstanceName(instance.getInstanceName(), null)) {
            throw new IllegalArgumentException("Container instance name already exists");
        }
        containerInstanceMapper.insert(instance);
        return getContainerInstanceById(instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContainerInstance updateContainerInstance(ContainerInstance instance) {
        if (instance == null || instance.getId() == null) {
            throw new IllegalArgumentException("Container instance ID is required");
        }
        ContainerInstance existing = getContainerInstanceById(instance.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Container instance does not exist");
        }
        if (StringUtils.hasText(instance.getInstanceName())
            && checkContainerInstanceName(instance.getInstanceName(), instance.getId())) {
            throw new IllegalArgumentException("Container instance name already exists");
        }
        mergeUnsetFields(instance, existing);
        normalizeDefaults(instance, false);
        instance.setUpdateTime(new Date());
        containerInstanceMapper.updateById(instance);
        return getContainerInstanceById(instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteContainerInstance(Long id) {
        ContainerInstance instance = requireInstance(id);
        assertDeletable(instance);
        return containerInstanceMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteContainerInstances(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<ContainerInstance> instances = containerInstanceMapper.selectBatchIds(ids);
        for (ContainerInstance instance : instances) {
            assertDeletable(instance);
        }
        return containerInstanceMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startContainerInstance(Long id) {
        ContainerInstance instance = requireInstance(id);
        if (STATUS_RUNNING.equals(instance.getInstanceStatus())) {
            throw new IllegalArgumentException("Container instance is already running");
        }
        instance.setInstanceStatus(STATUS_STARTING);
        instance.setHealthStatus(HEALTH_UNKNOWN);
        if (!StringUtils.hasText(instance.getContainerId())) {
            instance.setContainerId("container-" + id);
        }
        instance.setStartTime(new Date());
        instance.setStopTime(null);
        instance.setUpdateTime(new Date());
        return containerInstanceMapper.updateById(instance) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopContainerInstance(Long id) {
        ContainerInstance instance = requireInstance(id);
        if (STATUS_STOPPED.equals(instance.getInstanceStatus())) {
            throw new IllegalArgumentException("Container instance is already stopped");
        }
        instance.setInstanceStatus(STATUS_STOPPING);
        instance.setHealthStatus(HEALTH_UNKNOWN);
        instance.setStopTime(new Date());
        instance.setUpdateTime(new Date());
        return containerInstanceMapper.updateById(instance) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restartContainerInstance(Long id) {
        ContainerInstance instance = requireInstance(id);
        instance.setRestartCount(nullToZero(instance.getRestartCount()) + 1);
        instance.setInstanceStatus(STATUS_STARTING);
        instance.setHealthStatus(HEALTH_UNKNOWN);
        instance.setStartTime(new Date());
        instance.setStopTime(null);
        instance.setUpdateTime(new Date());
        return containerInstanceMapper.updateById(instance) > 0;
    }

    @Override
    public Map<String, Object> getContainerInstanceStatistics() {
        List<ContainerInstance> instances = containerInstanceMapper.selectList(baseQuery());
        Map<String, Object> statistics = new LinkedHashMap<String, Object>();
        long total = instances.size();
        long running = countByStatus(instances, STATUS_RUNNING);
        long stopped = countByStatus(instances, STATUS_STOPPED);
        long error = countByStatus(instances, STATUS_ERROR);
        long starting = countByStatus(instances, STATUS_STARTING);
        long stopping = countByStatus(instances, STATUS_STOPPING);
        long healthy = countByHealth(instances, "healthy");
        long unhealthy = countByHealth(instances, HEALTH_UNHEALTHY);
        long unknown = countByHealth(instances, HEALTH_UNKNOWN);

        statistics.put("totalInstances", total);
        statistics.put("runningInstances", running);
        statistics.put("stoppedInstances", stopped);
        statistics.put("errorInstances", error);
        statistics.put("startingInstances", starting);
        statistics.put("stoppingInstances", stopping);
        statistics.put("healthyInstances", healthy);
        statistics.put("unhealthyInstances", unhealthy);
        statistics.put("unknownInstances", unknown);
        statistics.put("avgCpuUsage", average(instances, "cpu"));
        statistics.put("avgMemoryUsage", average(instances, "memory"));
        statistics.put("avgGpuUsage", average(instances, "gpu"));
        statistics.put("total_instances", total);
        statistics.put("running_instances", running);
        statistics.put("stopped_instances", stopped);
        statistics.put("error_instances", error);
        statistics.put("healthy_instances", healthy);
        statistics.put("unhealthy_instances", unhealthy);
        return statistics;
    }

    @Override
    public List<ContainerInstance> getRunningContainerInstances() {
        return listByStatus(STATUS_RUNNING);
    }

    @Override
    public List<ContainerInstance> getErrorContainerInstances() {
        return listByStatus(STATUS_ERROR);
    }

    @Override
    public List<ContainerInstance> getUnhealthyContainerInstances() {
        LambdaQueryWrapper<ContainerInstance> wrapper = baseQuery()
            .eq(ContainerInstance::getHealthStatus, HEALTH_UNHEALTHY)
            .orderByDesc(ContainerInstance::getCreateTime)
            .orderByDesc(ContainerInstance::getId);
        return containerInstanceMapper.selectList(wrapper);
    }

    @Override
    public boolean checkContainerInstanceName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        LambdaQueryWrapper<ContainerInstance> wrapper = baseQuery()
            .eq(ContainerInstance::getInstanceName, name.trim());
        if (excludeId != null) {
            wrapper.ne(ContainerInstance::getId, excludeId);
        }
        return containerInstanceMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<ContainerInstance> getContainerInstancesByAlgorithm(Long algorithmId) {
        if (algorithmId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ContainerInstance> wrapper = baseQuery()
            .eq(ContainerInstance::getAlgorithmId, algorithmId)
            .orderByDesc(ContainerInstance::getCreateTime)
            .orderByDesc(ContainerInstance::getId);
        return containerInstanceMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContainerInstanceMonitoring(Long id, BigDecimal cpuUsage, BigDecimal memoryUsage, BigDecimal gpuUsage) {
        ContainerInstance instance = requireInstance(id);
        instance.setCpuUsage(cpuUsage);
        instance.setMemoryUsage(memoryUsage);
        instance.setGpuUsage(gpuUsage);
        instance.setUpdateTime(new Date());
        return containerInstanceMapper.updateById(instance) > 0;
    }

    private LambdaQueryWrapper<ContainerInstance> baseQuery() {
        return new LambdaQueryWrapper<ContainerInstance>()
            .eq(ContainerInstance::getIsDeleted, 0);
    }

    private void applyKeywordFilter(LambdaQueryWrapper<ContainerInstance> wrapper, String name) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        String keyword = name.trim();
        Long id = parseLong(keyword);
        wrapper.and(item -> {
            item.like(ContainerInstance::getInstanceName, keyword);
            if (id != null) {
                item.or().eq(ContainerInstance::getId, id);
            }
        });
    }

    private List<ContainerInstance> listByStatus(String status) {
        LambdaQueryWrapper<ContainerInstance> wrapper = baseQuery()
            .eq(ContainerInstance::getInstanceStatus, status)
            .orderByDesc(ContainerInstance::getCreateTime)
            .orderByDesc(ContainerInstance::getId);
        return containerInstanceMapper.selectList(wrapper);
    }

    private ContainerInstance requireInstance(Long id) {
        ContainerInstance instance = getContainerInstanceById(id);
        if (instance == null) {
            throw new IllegalArgumentException("Container instance does not exist");
        }
        return instance;
    }

    private void assertDeletable(ContainerInstance instance) {
        if (instance == null) {
            return;
        }
        if (STATUS_RUNNING.equals(instance.getInstanceStatus()) || STATUS_STARTING.equals(instance.getInstanceStatus())) {
            throw new IllegalArgumentException("Running container instance cannot be deleted");
        }
    }

    private void normalizeDefaults(ContainerInstance instance, boolean creating) {
        if (!StringUtils.hasText(instance.getInstanceName()) && StringUtils.hasText(instance.getName())) {
            instance.setInstanceName(instance.getName().trim());
        }
        if (!StringUtils.hasText(instance.getInstanceName())) {
            throw new IllegalArgumentException("Container instance name is required");
        }
        instance.setInstanceName(instance.getInstanceName().trim());
        if (!StringUtils.hasText(instance.getImageName()) && StringUtils.hasText(instance.getImage())) {
            instance.setImage(instance.getImage());
        }
        if (!StringUtils.hasText(instance.getImageName())) {
            throw new IllegalArgumentException("Container image is required");
        }
        if (!StringUtils.hasText(instance.getImageTag())) {
            instance.setImageTag("latest");
        }
        if (!StringUtils.hasText(instance.getInstanceType())) {
            instance.setInstanceType(firstNonBlank(instance.getImageType(), "custom"));
        }
        if (!StringUtils.hasText(instance.getInstanceStatus())) {
            instance.setInstanceStatus(STATUS_STOPPED);
        }
        if (!StringUtils.hasText(instance.getHealthStatus())) {
            instance.setHealthStatus(HEALTH_UNKNOWN);
        }
        if (instance.getInstanceCount() == null) {
            instance.setInstanceCount(1);
        }
        if (instance.getRestartCount() == null) {
            instance.setRestartCount(0);
        }
        if (instance.getRecordStatus() == null) {
            instance.setRecordStatus(1);
        }
        if (instance.getIsDeleted() == null) {
            instance.setIsDeleted(0);
        }
        if (!StringUtils.hasText(instance.getTenantId())) {
            instance.setTenantId(DEFAULT_TENANT_ID);
        }
        if (!StringUtils.hasText(instance.getPortConfig())) {
            instance.setPortConfig("{}");
        }
        if (!StringUtils.hasText(instance.getEnvConfig())) {
            instance.setEnvConfig("{}");
        }
        if (!StringUtils.hasText(instance.getVolumeConfig())) {
            instance.setVolumeConfig("{}");
        }
        Date now = new Date();
        if (creating && instance.getCreateTime() == null) {
            instance.setCreateTime(now);
        }
        if (instance.getUpdateTime() == null) {
            instance.setUpdateTime(now);
        }
    }

    private void mergeUnsetFields(ContainerInstance target, ContainerInstance existing) {
        if (!StringUtils.hasText(target.getTenantId())) target.setTenantId(existing.getTenantId());
        if (!StringUtils.hasText(target.getInstanceName())) target.setInstanceName(existing.getInstanceName());
        if (!StringUtils.hasText(target.getContainerId())) target.setContainerId(existing.getContainerId());
        if (!StringUtils.hasText(target.getImageName())) target.setImageName(existing.getImageName());
        if (!StringUtils.hasText(target.getImageType())) target.setImageType(existing.getImageType());
        if (!StringUtils.hasText(target.getImageTag())) target.setImageTag(existing.getImageTag());
        if (target.getResourceTypeId() == null) target.setResourceTypeId(existing.getResourceTypeId());
        if (target.getResourceSpecId() == null) target.setResourceSpecId(existing.getResourceSpecId());
        if (target.getInstanceCount() == null) target.setInstanceCount(existing.getInstanceCount());
        if (target.getAlgorithmId() == null) target.setAlgorithmId(existing.getAlgorithmId());
        if (!StringUtils.hasText(target.getInstanceType())) target.setInstanceType(existing.getInstanceType());
        if (!StringUtils.hasText(target.getCpuLimit())) target.setCpuLimit(existing.getCpuLimit());
        if (!StringUtils.hasText(target.getMemoryLimit())) target.setMemoryLimit(existing.getMemoryLimit());
        if (!StringUtils.hasText(target.getGpuLimit())) target.setGpuLimit(existing.getGpuLimit());
        if (!StringUtils.hasText(target.getPortConfig())) target.setPortConfig(existing.getPortConfig());
        if (!StringUtils.hasText(target.getEnvConfig())) target.setEnvConfig(existing.getEnvConfig());
        if (!StringUtils.hasText(target.getVolumeConfig())) target.setVolumeConfig(existing.getVolumeConfig());
        if (!StringUtils.hasText(target.getInstanceStatus())) target.setInstanceStatus(existing.getInstanceStatus());
        if (!StringUtils.hasText(target.getHealthStatus())) target.setHealthStatus(existing.getHealthStatus());
        if (target.getStartTime() == null) target.setStartTime(existing.getStartTime());
        if (target.getStopTime() == null) target.setStopTime(existing.getStopTime());
        if (target.getRestartCount() == null) target.setRestartCount(existing.getRestartCount());
        if (target.getCpuUsage() == null) target.setCpuUsage(existing.getCpuUsage());
        if (target.getMemoryUsage() == null) target.setMemoryUsage(existing.getMemoryUsage());
        if (target.getGpuUsage() == null) target.setGpuUsage(existing.getGpuUsage());
        if (!StringUtils.hasText(target.getLogsPath())) target.setLogsPath(existing.getLogsPath());
        if (target.getCreateUser() == null) target.setCreateUser(existing.getCreateUser());
        if (!StringUtils.hasText(target.getCreateDept())) target.setCreateDept(existing.getCreateDept());
        if (target.getCreateTime() == null) target.setCreateTime(existing.getCreateTime());
        if (target.getUpdateUser() == null) target.setUpdateUser(existing.getUpdateUser());
        if (target.getRecordStatus() == null) target.setRecordStatus(existing.getRecordStatus());
        if (target.getIsDeleted() == null) target.setIsDeleted(existing.getIsDeleted());
    }

    private long countByStatus(List<ContainerInstance> instances, String status) {
        long count = 0L;
        for (ContainerInstance instance : instances) {
            if (status.equals(instance.getInstanceStatus())) {
                count++;
            }
        }
        return count;
    }

    private long countByHealth(List<ContainerInstance> instances, String healthStatus) {
        long count = 0L;
        for (ContainerInstance instance : instances) {
            if (healthStatus.equals(instance.getHealthStatus())) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal average(List<ContainerInstance> instances, String metric) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (ContainerInstance instance : instances) {
            BigDecimal value = metricValue(instance, metric);
            if (value != null) {
                sum = sum.add(value);
                count++;
            }
        }
        return count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal metricValue(ContainerInstance instance, String metric) {
        if ("cpu".equals(metric)) {
            return instance.getCpuUsage();
        }
        if ("memory".equals(metric)) {
            return instance.getMemoryUsage();
        }
        if ("gpu".equals(metric)) {
            return instance.getGpuUsage();
        }
        return null;
    }

    private Date parseDate(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        String[] patterns = new String[] {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                Date date = new SimpleDateFormat(pattern).parse(trimmed);
                if (endOfDay && "yyyy-MM-dd".equals(pattern)) {
                    return new Date(date.getTime() + 86399999L);
                }
                return date;
            } catch (ParseException ignored) {
                // Try the next frontend date format.
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        try {
            return StringUtils.hasText(value) ? Long.valueOf(value.trim()) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long normalizePage(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1 ? 20L : size;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String first, String fallback) {
        return StringUtils.hasText(first) ? first.trim() : fallback;
    }
}
