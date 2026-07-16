/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.common.utils.poi.ExcelUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Event management routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsEventManagement")
public class VlsEventManagementController extends VlsControllerSupport {

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
        return operationResult(eventManagementService.deleteEvent(id), "Event was not deleted");
    }

    /**
     * Delete events in batch.
     */
    @DeleteMapping("/batch")
    public BladeResult<Boolean> batchDeleteEvents(@RequestBody List<Long> ids) {
        return BladeResult.success(eventManagementService.batchDeleteEvents(ids));
    }

    /** Return one event through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<EventManagement> detail(@RequestParam Long id) {
        return getEventById(id);
    }

    /** Return the event page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<EventManagement>> list(@RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) String eventType,
                                                        @RequestParam(required = false) String eventStatus,
                                                        @RequestParam(required = false) String eventLevel,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String startTime,
                                                        @RequestParam(required = false) String endTime) {
        return getEventPage(current, size, eventType, eventStatus, eventLevel, keyword, startTime, endTime);
    }

    /** Create an event through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<EventManagement> save(@RequestBody EventManagement event) {
        return createEvent(event);
    }

    /** Report an event through the same real persistence path as event creation. */
    @PostMapping("/report")
    public BladeResult<EventManagement> report(@RequestBody EventManagement event) {
        return createEvent(event);
    }

    /** Update an event through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<EventManagement> update(@RequestBody EventManagement event) {
        return updateEvent(event);
    }

    /** Insert or update an event through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<EventManagement> submit(@RequestBody EventManagement event) {
        return event != null && event.getId() != null ? updateEvent(event) : createEvent(event);
    }

    /** Delete events by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(eventManagementService.batchDeleteEvents(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export actual filtered event rows. */
    @GetMapping("/export-vlsEventManagement")
    public void exportVlsEventManagement(@RequestParam(required = false) String eventType,
                                         @RequestParam(required = false) String eventStatus,
                                         @RequestParam(required = false) String eventLevel,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String startTime,
                                         @RequestParam(required = false) String endTime,
                                         HttpServletResponse response) {
        BladePage<EventManagement> page = eventManagementService.getEventPage(Long.valueOf(1L),
            Long.valueOf(Integer.MAX_VALUE), eventType, eventStatus, eventLevel, keyword, startTime, endTime);
        ExcelUtil.exportExcel(page.getRecords(), "VLS Events", EventManagement.class, response);
    }
}
