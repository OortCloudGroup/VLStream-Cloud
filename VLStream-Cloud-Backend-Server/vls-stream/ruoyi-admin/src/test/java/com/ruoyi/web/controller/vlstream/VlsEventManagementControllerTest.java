/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.EventManagement;
import com.ruoyi.vlstream.service.IVlsEventManagementService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsEventManagementControllerTest {

    @Test
    void exposesFrontendEventManagementRoutes() throws Exception {
        Method page = VlsEventManagementController.class.getDeclaredMethod("getEventPage", Long.class, Long.class, String.class, String.class, String.class, String.class, String.class, String.class);
        Method detail = VlsEventManagementController.class.getDeclaredMethod("getEventById", Long.class);
        Method create = VlsEventManagementController.class.getDeclaredMethod("createEvent", EventManagement.class);
        Method update = VlsEventManagementController.class.getDeclaredMethod("updateEvent", EventManagement.class);
        Method status = VlsEventManagementController.class.getDeclaredMethod("updateEventStatus", Long.class, String.class, String.class, String.class);
        Method delete = VlsEventManagementController.class.getDeclaredMethod("deleteEvent", Long.class);
        Method batch = VlsEventManagementController.class.getDeclaredMethod("batchDeleteEvents", java.util.List.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, detail.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {""}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {""}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/status"}, status.getAnnotation(PatchMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batch.getAnnotation(DeleteMapping.class).value());
    }

    @Test
    void pageReturnsBladePageAndForwardsFrontendFilters() {
        IVlsEventManagementService service = mock(IVlsEventManagementService.class);
        VlsEventManagementController controller = new VlsEventManagementController(service);
        EventManagement event = event(9L, "person intrusion");
        BladePage<EventManagement> page = BladePage.of(Collections.singletonList(event), 1L, 20L, 1L);

        when(service.getEventPage(1L, 20L, "intrusion", "pending", "high", "gate", "2026-01-01 00:00:00", "2026-01-02 00:00:00")).thenReturn(page);

        BladeResult<BladePage<EventManagement>> response = controller.getEventPage(1L, 20L, "intrusion", "pending", "high", "gate", "2026-01-01 00:00:00", "2026-01-02 00:00:00");

        assertEquals(200, response.getCode());
        assertSame(page, response.getData());
        verify(service).getEventPage(1L, 20L, "intrusion", "pending", "high", "gate", "2026-01-01 00:00:00", "2026-01-02 00:00:00");
    }

    @Test
    void createAndUpdateReturnStoredEvent() {
        IVlsEventManagementService service = mock(IVlsEventManagementService.class);
        VlsEventManagementController controller = new VlsEventManagementController(service);
        EventManagement event = event(null, "vehicle alarm");
        EventManagement stored = event(10L, "vehicle alarm");

        when(service.createEvent(event)).thenReturn(stored);
        when(service.updateEvent(stored)).thenReturn(stored);

        BladeResult<EventManagement> created = controller.createEvent(event);
        BladeResult<EventManagement> updated = controller.updateEvent(stored);

        assertSame(stored, created.getData());
        assertSame(stored, updated.getData());
        verify(service).createEvent(event);
        verify(service).updateEvent(stored);
    }

    @Test
    void updateStatusAndDeletesUseFrontendShapes() {
        IVlsEventManagementService service = mock(IVlsEventManagementService.class);
        VlsEventManagementController controller = new VlsEventManagementController(service);
        EventManagement event = event(11L, "smoke alarm");
        event.setEventStatus("completed");
        event.setHandleResult("handled");

        when(service.updateEventStatus(11L, "completed", "admin", "handled")).thenReturn(event);
        when(service.deleteEvent(11L)).thenReturn(true);
        when(service.batchDeleteEvents(Arrays.asList(11L, 12L))).thenReturn(true);

        BladeResult<EventManagement> status = controller.updateEventStatus(11L, "completed", "admin", "handled");
        BladeResult<Boolean> deleted = controller.deleteEvent(11L);
        BladeResult<Boolean> batchDeleted = controller.batchDeleteEvents(Arrays.asList(11L, 12L));

        assertSame(event, status.getData());
        assertEquals(Boolean.TRUE, deleted.getData());
        assertEquals(Boolean.TRUE, batchDeleted.getData());
        verify(service).updateEventStatus(11L, "completed", "admin", "handled");
        verify(service).deleteEvent(11L);
        verify(service).batchDeleteEvents(Arrays.asList(11L, 12L));
    }

    private EventManagement event(Long id, String eventDesc) {
        EventManagement event = new EventManagement();
        event.setId(id);
        event.setEventDesc(eventDesc);
        event.setEventType("intrusion");
        event.setEventLevel("high");
        event.setEventStatus("pending");
        event.setReportLocation("gate");
        event.setReportDevice("camera-1");
        return event;
    }
}
