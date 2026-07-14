/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.EventManagement;

import java.util.List;

/**
 * Service for the VLS event management frontend compatibility surface.
 */
public interface IVlsEventManagementService {

    /**
     * Return frontend-compatible paged event records.
     */
    BladePage<EventManagement> getEventPage(Long current, Long size, String eventType, String eventStatus,
                                            String eventLevel, String keyword, String startTime, String endTime);

    /**
     * Return one event by id.
     */
    EventManagement getEventById(Long id);

    /**
     * Create a new event.
     */
    EventManagement createEvent(EventManagement eventManagement);

    /**
     * Update an existing event.
     */
    EventManagement updateEvent(EventManagement eventManagement);

    /**
     * Update frontend event processing status fields.
     */
    EventManagement updateEventStatus(Long id, String status, String executor, String handleResult);

    /**
     * Delete one event by id.
     */
    boolean deleteEvent(Long id);

    /**
     * Delete events by ids.
     */
    boolean batchDeleteEvents(List<Long> ids);
}
