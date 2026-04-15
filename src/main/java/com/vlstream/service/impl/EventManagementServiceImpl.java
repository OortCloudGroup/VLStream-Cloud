package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.EventManagement;
import com.vlstream.mapper.EventManagementMapper;
import com.vlstream.service.EventManagementService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event management service implementation.
 */
@Slf4j
@Service
public class EventManagementServiceImpl extends ServiceImpl<EventManagementMapper, EventManagement> implements EventManagementService {

    @Override
    public IPage<EventManagement> pageEvents(Page<EventManagement> page,
                                             String eventType,
                                             String eventStatus,
                                             String eventLevel,
                                             String keyword,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime) {
        LambdaQueryWrapper<EventManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(eventType), EventManagement::getEventType, eventType);
        wrapper.eq(StringUtils.isNotBlank(eventStatus), EventManagement::getEventStatus, eventStatus);
        wrapper.eq(StringUtils.isNotBlank(eventLevel), EventManagement::getEventLevel, eventLevel);
        wrapper.ge(startTime != null, EventManagement::getReportTime, startTime);
        wrapper.le(endTime != null, EventManagement::getReportTime, endTime);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(EventManagement::getEventDesc, keyword)
                    .or().like(EventManagement::getReportLocation, keyword)
                    .or().like(EventManagement::getReportDevice, keyword));
        }
        wrapper.orderByDesc(EventManagement::getReportTime);
        return page(page, wrapper);
    }

    @Override
    public EventManagement getEventById(Long id) {
        if (id == null) {
            return null;
        }
        return getById(id);
    }

    @Override
    public boolean createEvent(EventManagement eventManagement) {
        if (eventManagement == null) {
            return false;
        }
        if (StringUtils.isBlank(eventManagement.getEventLevel())) {
            eventManagement.setEventLevel("medium");
        }
        if (StringUtils.isBlank(eventManagement.getEventStatus())) {
            eventManagement.setEventStatus("pending");
        }
        if (eventManagement.getReportTime() == null) {
            eventManagement.setReportTime(LocalDateTime.now());
        }
        return save(eventManagement);
    }

    @Override
    public boolean updateEvent(EventManagement eventManagement) {
        return eventManagement != null && updateById(eventManagement);
    }

    @Override
    public boolean updateStatus(Long id, String status, String executor, String handleResult) {
        if (id == null || StringUtils.isBlank(status)) {
            return false;
        }
        EventManagement update = new EventManagement();
        update.setId(id);
        update.setEventStatus(status);
        if (StringUtils.isNotBlank(executor)) {
            update.setExecutor(executor);
        }
        if (StringUtils.isNotBlank(handleResult)) {
            update.setHandleResult(handleResult);
        }
        return updateById(update);
    }

    @Override
    public boolean removeEvent(Long id) {
        if (id == null) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public boolean removeEvents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }
}
