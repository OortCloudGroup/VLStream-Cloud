/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.EventManagement;
import com.ruoyi.vlstream.service.IVlsEventManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Event management routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsEventManagement")
public class VlsEventManagementController {

    private final IVlsEventManagementService eventManagementService;

    /**
     * Return a page of event records.
     */
    @GetMapping("/page")
    public BladeResult<BladePage<EventManagement>> getEventPage(@RequestParam(required = false) Long current,
                                                                @RequestParam(required = false) Long size,
                                                                @RequestParam(required = false) String eventType,
                                                                @RequestParam(required = false) String eventStatus,
                                                                @RequestParam(required = false) String eventLevel,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String startTime,
                                                                @RequestParam(required = false) String endTime) {
        return BladeResult.success(eventManagementService.getEventPage(current, size, eventType, eventStatus, eventLevel, keyword, startTime, endTime));
    }

    /**
     * Return one event record by id.
     */
    @GetMapping("/{id}")
    public BladeResult<EventManagement> getEventById(@PathVariable Long id) {
        EventManagement event = eventManagementService.getEventById(id);
        return event == null ? BladeResult.<EventManagement>fail("Event not found") : BladeResult.success(event);
    }

    /**
     * Create an event.
     */
    @PostMapping("")
    public BladeResult<EventManagement> createEvent(@RequestBody EventManagement eventManagement) {
        try {
            return BladeResult.success(eventManagementService.createEvent(eventManagement));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Update an event.
     */
    @PutMapping("")
    public BladeResult<EventManagement> updateEvent(@RequestBody EventManagement eventManagement) {
        try {
            return BladeResult.success(eventManagementService.updateEvent(eventManagement));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Update event status and handling result.
     */
    @PatchMapping("/{id}/status")
    public BladeResult<EventManagement> updateEventStatus(@PathVariable Long id,
                                                          @RequestParam String status,
                                                          @RequestParam(required = false) String executor,
                                                          @RequestParam(required = false) String handleResult) {
        try {
            return BladeResult.success(eventManagementService.updateEventStatus(id, status, executor, handleResult));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Delete one event.
     */
    @DeleteMapping("/{id}")
    public BladeResult<Boolean> deleteEvent(@PathVariable Long id) {
        return BladeResult.success(eventManagementService.deleteEvent(id));
    }

    /**
     * Delete events in batch.
     */
    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteEvents(@RequestBody List<Long> ids) {
        return BladeResult.success(eventManagementService.batchDeleteEvents(ids));
    }
}
