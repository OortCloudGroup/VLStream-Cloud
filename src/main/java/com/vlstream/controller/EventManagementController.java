package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.EventManagement;
import com.vlstream.service.EventManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST endpoints for event management.
 */
@Slf4j
@Api(tags = "Event Management")
@RestController
@RequestMapping("/api/event-management")
@RequiredArgsConstructor
public class EventManagementController {

    private final EventManagementService eventManagementService;

    @GetMapping("/page")
    @ApiOperation("Page query")
    public Result<IPage<EventManagement>> pageEvents(
            @ApiParam("Current page") @RequestParam(defaultValue = "1") Long current,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") Long size,
            @ApiParam("Event type") @RequestParam(required = false) String eventType,
            @ApiParam("Event status") @RequestParam(required = false) String eventStatus,
            @ApiParam("Event level") @RequestParam(required = false) String eventLevel,
            @ApiParam("Search keyword") @RequestParam(required = false) String keyword,
            @ApiParam("Report time from") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("Report time to") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            Page<EventManagement> page = new Page<>(current, size);
            IPage<EventManagement> result = eventManagementService.pageEvents(page, eventType, eventStatus, eventLevel, keyword, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to page query events", e);
            return Result.error("Page query failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("Query by ID")
    public Result<EventManagement> getEvent(@ApiParam("Primary id") @PathVariable Long id) {
        EventManagement event = eventManagementService.getEventById(id);
        if (event == null) {
            return Result.error("Event not found");
        }
        return Result.success(event);
    }

    @PostMapping
    @ApiOperation("Create event")
    public Result<Boolean> createEvent(@RequestBody EventManagement eventManagement) {
        boolean created = eventManagementService.createEvent(eventManagement);
        if (created) {
            return Result.success(true);
        }
        return Result.error("Create event failed");
    }

    @PutMapping
    @ApiOperation("Update event")
    public Result<Boolean> updateEvent(@RequestBody EventManagement eventManagement) {
        boolean updated = eventManagementService.updateEvent(eventManagement);
        if (updated) {
            return Result.success(true);
        }
        return Result.error("Update event failed");
    }

    @PatchMapping("/{id}/status")
    @ApiOperation("Update event status")
    public Result<Boolean> updateStatus(@ApiParam("Primary id") @PathVariable Long id,
                                        @ApiParam("Status") @RequestParam String status,
                                        @ApiParam("Executor") @RequestParam(required = false) String executor,
                                        @ApiParam("Handle result") @RequestParam(required = false) String handleResult) {
        boolean updated = eventManagementService.updateStatus(id, status, executor, handleResult);
        if (updated) {
            return Result.success(true);
        }
        return Result.error("Update status failed");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete event")
    public Result<Boolean> deleteEvent(@ApiParam("Primary id") @PathVariable Long id) {
        boolean removed = eventManagementService.removeEvent(id);
        if (removed) {
            return Result.success(true);
        }
        return Result.error("Delete event failed");
    }

    @DeleteMapping("/batch")
    @ApiOperation("Batch delete events")
    public Result<Boolean> deleteEvents(@RequestBody List<Long> ids) {
        boolean removed = eventManagementService.removeEvents(ids);
        if (removed) {
            return Result.success(true);
        }
        return Result.error("Batch delete failed");
    }
}
