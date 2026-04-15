package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.EventManagement;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service definition for event management.
 */
public interface EventManagementService extends IService<EventManagement> {

    /**
     * Page query with optional filters.
     */
    IPage<EventManagement> pageEvents(Page<EventManagement> page,
                                      String eventType,
                                      String eventStatus,
                                      String eventLevel,
                                      String keyword,
                                      LocalDateTime startTime,
                                      LocalDateTime endTime);

    /**
     * Get one event by id.
     */
    EventManagement getEventById(Long id);

    /**
     * Create a new event.
     */
    boolean createEvent(EventManagement eventManagement);

    /**
     * Update an existing event.
     */
    boolean updateEvent(EventManagement eventManagement);

    /**
     * Update only status and optional executor/handle result.
     */
    boolean updateStatus(Long id, String status, String executor, String handleResult);

    /**
     * Logical delete by id.
     */
    boolean removeEvent(Long id);

    /**
     * Batch logical delete.
     */
    boolean removeEvents(List<Long> ids);
}
