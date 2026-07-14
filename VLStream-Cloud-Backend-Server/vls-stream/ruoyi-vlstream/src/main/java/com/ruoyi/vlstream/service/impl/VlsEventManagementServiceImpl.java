/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.EventManagement;
import com.ruoyi.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.service.IVlsEventManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for VLS event management compatibility routes.
 */
@Service
@RequiredArgsConstructor
public class VlsEventManagementServiceImpl implements IVlsEventManagementService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_LEVEL = "medium";
    private static final String DEFAULT_STATUS = "pending";

    private final VlsEventManagementMapper eventManagementMapper;

    @Override
    public BladePage<EventManagement> getEventPage(Long current, Long size, String eventType, String eventStatus,
                                                   String eventLevel, String keyword, String startTime, String endTime) {
        Page<EventManagement> page = new Page<EventManagement>(normalizePage(current), normalizeSize(size));
        LambdaQueryWrapper<EventManagement> wrapper = baseQuery();
        if (StringUtils.hasText(eventType)) {
            wrapper.eq(EventManagement::getEventType, eventType.trim());
        }
        if (StringUtils.hasText(eventStatus)) {
            wrapper.eq(EventManagement::getEventStatus, eventStatus.trim());
        }
        if (StringUtils.hasText(eventLevel)) {
            wrapper.eq(EventManagement::getEventLevel, eventLevel.trim());
        }
        Date begin = parseDate(startTime, false);
        if (begin != null) {
            wrapper.ge(EventManagement::getReportTime, begin);
        }
        Date end = parseDate(endTime, true);
        if (end != null) {
            wrapper.le(EventManagement::getReportTime, end);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(EventManagement::getEventDesc, value)
                .or().like(EventManagement::getReportLocation, value)
                .or().like(EventManagement::getReportDevice, value));
        }
        wrapper.orderByDesc(EventManagement::getReportTime).orderByDesc(EventManagement::getCreateTime).orderByDesc(EventManagement::getId);
        Page<EventManagement> result = eventManagementMapper.selectPage(page, wrapper);
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public EventManagement getEventById(Long id) {
        if (id == null) {
            return null;
        }
        return eventManagementMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventManagement createEvent(EventManagement eventManagement) {
        if (eventManagement == null) {
            throw new IllegalArgumentException("Event is required");
        }
        normalizeDefaults(eventManagement, true);
        eventManagementMapper.insert(eventManagement);
        EventManagement stored = eventManagementMapper.selectById(eventManagement.getId());
        return stored == null ? eventManagement : stored;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventManagement updateEvent(EventManagement eventManagement) {
        if (eventManagement == null || eventManagement.getId() == null) {
            throw new IllegalArgumentException("Event id is required");
        }
        EventManagement existing = eventManagementMapper.selectById(eventManagement.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Event does not exist");
        }
        mergeUnsetFields(eventManagement, existing);
        normalizeDefaults(eventManagement, false);
        eventManagement.setUpdateTime(new Date());
        eventManagementMapper.updateById(eventManagement);
        EventManagement stored = eventManagementMapper.selectById(eventManagement.getId());
        return stored == null ? eventManagement : stored;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventManagement updateEventStatus(Long id, String status, String executor, String handleResult) {
        EventManagement event = getEventById(id);
        if (event == null) {
            throw new IllegalArgumentException("Event does not exist");
        }
        if (StringUtils.hasText(status)) {
            event.setEventStatus(status.trim());
        }
        if (StringUtils.hasText(handleResult)) {
            event.setHandleResult(handleResult.trim());
        }
        event.setUpdateTime(new Date());
        eventManagementMapper.updateById(event);
        EventManagement stored = eventManagementMapper.selectById(id);
        return stored == null ? event : stored;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEvent(Long id) {
        if (id == null) {
            return false;
        }
        return eventManagementMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteEvents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return eventManagementMapper.deleteBatchIds(ids) > 0;
    }

    private LambdaQueryWrapper<EventManagement> baseQuery() {
        return new LambdaQueryWrapper<EventManagement>().eq(EventManagement::getIsDeleted, 0);
    }

    private void normalizeDefaults(EventManagement event, boolean create) {
        if (!StringUtils.hasText(event.getTenantId())) {
            event.setTenantId(DEFAULT_TENANT_ID);
        }
        if (!StringUtils.hasText(event.getEventLevel())) {
            event.setEventLevel(DEFAULT_LEVEL);
        }
        if (!StringUtils.hasText(event.getEventStatus())) {
            event.setEventStatus(DEFAULT_STATUS);
        }
        if (event.getStatus() == null) {
            event.setStatus(Integer.valueOf(1));
        }
        if (event.getIsDeleted() == null) {
            event.setIsDeleted(Integer.valueOf(0));
        }
        if (event.getIsReport() == null) {
            event.setIsReport(Integer.valueOf(0));
        }
        Date now = new Date();
        if (event.getReportTime() == null) {
            event.setReportTime(now);
        }
        if (create && event.getCreateTime() == null) {
            event.setCreateTime(now);
        }
        if (event.getUpdateTime() == null) {
            event.setUpdateTime(now);
        }
    }

    private void mergeUnsetFields(EventManagement target, EventManagement existing) {
        if (!StringUtils.hasText(target.getTenantId())) target.setTenantId(existing.getTenantId());
        if (!StringUtils.hasText(target.getEventDesc())) target.setEventDesc(existing.getEventDesc());
        if (!StringUtils.hasText(target.getEventType())) target.setEventType(existing.getEventType());
        if (!StringUtils.hasText(target.getReportLocation())) target.setReportLocation(existing.getReportLocation());
        if (!StringUtils.hasText(target.getReportDevice())) target.setReportDevice(existing.getReportDevice());
        if (!StringUtils.hasText(target.getReportImg())) target.setReportImg(existing.getReportImg());
        if (target.getReportTime() == null) target.setReportTime(existing.getReportTime());
        if (!StringUtils.hasText(target.getEventLevel())) target.setEventLevel(existing.getEventLevel());
        if (!StringUtils.hasText(target.getEventStatus())) target.setEventStatus(existing.getEventStatus());
        if (!StringUtils.hasText(target.getEventData())) target.setEventData(existing.getEventData());
        if (!StringUtils.hasText(target.getHandleResult())) target.setHandleResult(existing.getHandleResult());
        if (!StringUtils.hasText(target.getFeedbackInfo())) target.setFeedbackInfo(existing.getFeedbackInfo());
        if (!StringUtils.hasText(target.getFeedbackImg())) target.setFeedbackImg(existing.getFeedbackImg());
        if (target.getFeedbackStatus() == null) target.setFeedbackStatus(existing.getFeedbackStatus());
        if (target.getIsReport() == null) target.setIsReport(existing.getIsReport());
        if (target.getCreateUser() == null) target.setCreateUser(existing.getCreateUser());
        if (!StringUtils.hasText(target.getCreateDept())) target.setCreateDept(existing.getCreateDept());
        if (target.getCreateTime() == null) target.setCreateTime(existing.getCreateTime());
        if (target.getUpdateUser() == null) target.setUpdateUser(existing.getUpdateUser());
        if (target.getStatus() == null) target.setStatus(existing.getStatus());
        if (target.getIsDeleted() == null) target.setIsDeleted(existing.getIsDeleted());
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

    private long normalizePage(Long current) {
        return current == null || current < 1 ? 1L : current.longValue();
    }

    private long normalizeSize(Long size) {
        return size == null || size < 1 ? 20L : size.longValue();
    }
}
