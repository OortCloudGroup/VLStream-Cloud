/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.Algorithm;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.domain.MobileSceneGovernance;
import com.ruoyi.vlstream.domain.MobileSceneGovernanceSubTask;
import com.ruoyi.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.mapper.VlsMobileSceneGovernanceMapper;
import com.ruoyi.vlstream.mapper.VlsMobileSceneGovernanceSubTaskMapper;
import com.ruoyi.vlstream.service.IVlsMobileSceneGovernanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real mobile-governance service including persisted cyclic child-task generation. */
@Service
public class VlsMobileSceneGovernanceServiceImpl
    extends AbstractVlsTenantCrudService<VlsMobileSceneGovernanceMapper, MobileSceneGovernance>
    implements IVlsMobileSceneGovernanceService {

    private static final String MODE_IMMEDIATE = "immediate";
    private static final String MODE_LOOP = "loop";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final VlsMobileSceneGovernanceSubTaskMapper subTaskMapper;
    private final VlsAlgorithmMapper algorithmMapper;
    private final VlsDeviceInfoMapper deviceInfoMapper;

    /** Inject the real child-task, algorithm and device mappers. */
    public VlsMobileSceneGovernanceServiceImpl(VlsMobileSceneGovernanceSubTaskMapper subTaskMapper,
                                               VlsAlgorithmMapper algorithmMapper,
                                               VlsDeviceInfoMapper deviceInfoMapper) {
        this.subTaskMapper = subTaskMapper;
        this.algorithmMapper = algorithmMapper;
        this.deviceInfoMapper = deviceInfoMapper;
    }

    /** Persist an immediate task after clearing loop-only fields. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileSceneGovernance saveImmediate(MobileSceneGovernance governance) {
        validateCommon(governance);
        governance.setGovernanceMode(MODE_IMMEDIATE);
        governance.setCycleType(null);
        governance.setIntervalDays(null);
        governance.setWeeklyDays(null);
        governance.setMonthlyDays(null);
        governance.setTriggerTimes(null);
        if (!save(governance)) {
            throw new IllegalStateException("Immediate governance task was not persisted");
        }
        return getById(governance.getId());
    }

    /** Persist a loop task and atomically generate every executable child row. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileSceneGovernance saveLoop(MobileSceneGovernance governance) {
        validateCommon(governance);
        validateLoop(governance);
        governance.setGovernanceMode(MODE_LOOP);
        if (!save(governance)) {
            throw new IllegalStateException("Loop governance task was not persisted");
        }
        List<MobileSceneGovernanceSubTask> subTasks = buildSubTasks(governance);
        for (MobileSceneGovernanceSubTask subTask : subTasks) {
            prepareSubTask(subTask);
            if (subTaskMapper.insert(subTask) != 1) {
                throw new IllegalStateException("Loop governance child task was not persisted");
            }
        }
        MobileSceneGovernance stored = getById(governance.getId());
        stored.setSubTaskList(subTasks);
        return stored;
    }

    /** Page immediate tasks and resolve real algorithm/device names. */
    @Override
    public BladePage<MobileSceneGovernance> listImmediate(Long current, Long size) {
        Page<MobileSceneGovernance> result = page(
            new Page<MobileSceneGovernance>(normalize(current, 1L), normalize(size, 10L)),
            new LambdaQueryWrapper<MobileSceneGovernance>()
                .eq(MobileSceneGovernance::getGovernanceMode, MODE_IMMEDIATE)
                .orderByDesc(MobileSceneGovernance::getCreateTime));
        hydrateNames(result.getRecords());
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    /** Page loop tasks, resolve names and attach their real persisted child tasks. */
    @Override
    public BladePage<MobileSceneGovernance> listLoop(Long current, Long size) {
        Page<MobileSceneGovernance> result = page(
            new Page<MobileSceneGovernance>(normalize(current, 1L), normalize(size, 10L)),
            new LambdaQueryWrapper<MobileSceneGovernance>()
                .eq(MobileSceneGovernance::getGovernanceMode, MODE_LOOP)
                .orderByDesc(MobileSceneGovernance::getCreateTime));
        hydrateNames(result.getRecords());
        for (MobileSceneGovernance governance : result.getRecords()) {
            governance.setSubTaskList(subTaskMapper.selectList(
                new LambdaQueryWrapper<MobileSceneGovernanceSubTask>()
                    .eq(MobileSceneGovernanceSubTask::getGovernanceId, governance.getId())
                    .orderByAsc(MobileSceneGovernanceSubTask::getExecuteTime)));
        }
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    /** Validate fields shared by immediate and loop modes. */
    private void validateCommon(MobileSceneGovernance governance) {
        if (governance == null) {
            throw new IllegalArgumentException("Request parameters cannot be empty");
        }
        requireText(governance.getName(), "Governance name cannot be empty");
        requireText(governance.getLocationIds(), "Analysis area cannot be empty");
        requireText(governance.getAlgorithmIds(), "AI algorithm cannot be empty");
        requireText(governance.getCameraIds(), "Camera cannot be empty");
    }

    /** Validate loop timing and supported cycle types. */
    private void validateLoop(MobileSceneGovernance governance) {
        requireText(governance.getCycleType(), "Loop cycle type cannot be empty");
        if (governance.getStartTime() == null || governance.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time cannot be empty");
        }
        if (governance.getEndTime().isBefore(governance.getStartTime())) {
            throw new IllegalArgumentException("End time cannot be earlier than start time");
        }
        requireText(governance.getTriggerTimes(), "Trigger time cannot be empty");
        if ("everyOtherDay".equals(governance.getCycleType())
            && (governance.getIntervalDays() == null || governance.getIntervalDays().intValue() <= 0)) {
            throw new IllegalArgumentException("Interval days in alternate days mode must be greater than 0");
        }
    }

    /** Generate child tasks for every matching date and configured trigger time. */
    private List<MobileSceneGovernanceSubTask> buildSubTasks(MobileSceneGovernance governance) {
        List<LocalTime> triggerTimes = parseTriggerTimes(governance.getTriggerTimes());
        List<MobileSceneGovernanceSubTask> result = new ArrayList<MobileSceneGovernanceSubTask>();
        LocalDate startDate = governance.getStartTime().toLocalDate();
        LocalDate endDate = governance.getEndTime().toLocalDate();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1L)) {
            if (!matches(governance, date, startDate)) {
                continue;
            }
            for (LocalTime time : triggerTimes) {
                LocalDateTime executeTime = LocalDateTime.of(date, time);
                if (executeTime.isBefore(governance.getStartTime()) || executeTime.isAfter(governance.getEndTime())) {
                    continue;
                }
                MobileSceneGovernanceSubTask subTask = new MobileSceneGovernanceSubTask();
                subTask.setGovernanceId(governance.getId());
                subTask.setName(governance.getName());
                subTask.setExecuteTime(executeTime);
                subTask.setTaskStatus("pending");
                subTask.setLocationIds(governance.getLocationIds());
                subTask.setAlgorithmIds(governance.getAlgorithmIds());
                subTask.setCameraIds(governance.getCameraIds());
                result.add(subTask);
            }
        }
        return result;
    }

    /** Determine whether a date belongs to the configured loop cycle. */
    private boolean matches(MobileSceneGovernance governance, LocalDate date, LocalDate startDate) {
        String cycleType = governance.getCycleType();
        if ("everyday".equals(cycleType)) {
            return true;
        }
        if ("everyOtherDay".equals(cycleType)) {
            long interval = governance.getIntervalDays() == null ? 1L : governance.getIntervalDays().longValue();
            return ChronoUnit.DAYS.between(startDate, date) % interval == 0L;
        }
        if ("weekly".equals(cycleType)) {
            Set<Integer> days = parseIntSet(governance.getWeeklyDays());
            if (days.isEmpty()) {
                days.add(Integer.valueOf(startDate.getDayOfWeek().getValue()));
            }
            return days.contains(Integer.valueOf(date.getDayOfWeek().getValue()));
        }
        if ("monthly".equals(cycleType)) {
            Set<Integer> days = parseIntSet(governance.getMonthlyDays());
            if (days.isEmpty()) {
                days.add(Integer.valueOf(startDate.getDayOfMonth()));
            }
            return days.contains(Integer.valueOf(date.getDayOfMonth()));
        }
        throw new IllegalArgumentException("Unsupported loop cycle type");
    }

    /** Parse and validate the comma-separated trigger-time list. */
    private List<LocalTime> parseTriggerTimes(String value) {
        List<LocalTime> result = new ArrayList<LocalTime>();
        for (String part : value.split(",")) {
            if (StringUtils.hasText(part)) {
                result.add(LocalTime.parse(part.trim(), TIME_FORMATTER));
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Trigger time cannot be empty");
        }
        return result;
    }

    /** Parse a comma-separated integer set. */
    private Set<Integer> parseIntSet(String value) {
        if (!StringUtils.hasText(value)) {
            return new HashSet<Integer>();
        }
        Set<Integer> result = new HashSet<Integer>();
        for (String part : value.split(",")) {
            if (StringUtils.hasText(part)) {
                result.add(Integer.valueOf(part.trim()));
            }
        }
        return result;
    }

    /** Resolve names from the actual algorithm and device tables. */
    private void hydrateNames(List<MobileSceneGovernance> records) {
        for (MobileSceneGovernance governance : records) {
            governance.setAlgorithmNames(resolveAlgorithmNames(parseIds(governance.getAlgorithmIds())));
            governance.setCameraNames(resolveCameraNames(parseIds(governance.getCameraIds())));
        }
    }

    /** Resolve algorithm names in request order. */
    private String resolveAlgorithmNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        Map<Long, String> names = new HashMap<Long, String>();
        for (Algorithm algorithm : algorithmMapper.selectBatchIds(ids)) {
            names.put(algorithm.getId(), algorithm.getName());
        }
        return joinNames(ids, names);
    }

    /** Resolve camera names in request order. */
    private String resolveCameraNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        Map<Long, String> names = new HashMap<Long, String>();
        for (DeviceInfo device : deviceInfoMapper.selectBatchIds(ids)) {
            names.put(device.getId(), device.getDeviceName());
        }
        return joinNames(ids, names);
    }

    /** Join resolved names without introducing placeholder values. */
    private String joinNames(List<Long> ids, Map<Long, String> names) {
        List<String> result = new ArrayList<String>();
        for (Long id : ids) {
            if (StringUtils.hasText(names.get(id))) {
                result.add(names.get(id));
            }
        }
        return String.join(",", result);
    }

    /** Parse a comma-separated long list while ignoring malformed tokens. */
    private List<Long> parseIds(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        for (String part : value.split(",")) {
            try {
                result.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
                // Names are resolved only for valid persisted IDs.
            }
        }
        return result;
    }

    /** Apply the single-tenant audit fields to a generated child row. */
    private void prepareSubTask(MobileSceneGovernanceSubTask subTask) {
        Date now = new Date();
        subTask.setTenantId(DEFAULT_TENANT_ID);
        subTask.setStatus(Integer.valueOf(1));
        subTask.setIsDeleted(Integer.valueOf(0));
        subTask.setCreateTime(now);
        subTask.setUpdateTime(now);
    }

    /** Require nonblank governance configuration text. */
    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Normalize pagination input. */
    private long normalize(Long value, long fallback) {
        return value == null || value.longValue() < 1L ? fallback : value.longValue();
    }
}
