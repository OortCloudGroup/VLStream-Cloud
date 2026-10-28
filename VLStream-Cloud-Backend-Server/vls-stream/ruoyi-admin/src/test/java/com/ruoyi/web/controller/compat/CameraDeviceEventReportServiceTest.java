package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.service.VlsDeviceEventReportService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class CameraDeviceEventReportServiceTest {
    final LocationTaskCompatService single=mock(LocationTaskCompatService.class);
    final VlsDeviceEventReportService devices=mock(VlsDeviceEventReportService.class);
    final CameraDeviceEventReportService reports=new CameraDeviceEventReportService(single,devices);

    @Test void singleKeepsBothOriginalCameraContracts() {
        Map<String,Object> body=new LinkedHashMap<>();
        LocationTaskResult<?> expected=LocationTaskResult.success(Collections.singletonMap("id","original-single-id"));
        when(single.reportCameraEvent(body)).thenReturn((LocationTaskResult)expected);
        when(single.reportCameraEvents(body)).thenReturn((LocationTaskResult)expected);
        assertSame(expected,reports.reportSingle(body));assertSame(expected,reports.reportBatch(body));
        verify(devices,never()).reportCameras(any());
    }

    @Test void multiPublicControllerReturnsActualAcceptedIdsAndNeverCallsSingleSink() {
        when(devices.isMultiTenant()).thenReturn(true);
        EventManagement event=new EventManagement();event.setId(91L);
        when(devices.reportCameras(any())).thenReturn(Collections.singletonList(event));
        LocationTaskCompatController controller=new LocationTaskCompatController(single,mock(BladeTokenUserStore.class),reports);
        LocationTaskResult<?> result=controller.reportCameraEvent(Collections.singletonMap("device_id","CAM-1"));
        assertEquals(200,result.getCode());Map<?,?> data=(Map<?,?>)result.getData();
        assertEquals("PENDING",data.get("delivery_status"));assertEquals(Collections.singletonList("91"),data.get("source_event_ids"));
        verifyNoInteractions(single);
    }

    @Test void malformedBatchAndPersistenceFailureCannotReturnSuccess() {
        when(devices.isMultiTenant()).thenReturn(true);
        assertEquals(4101,reports.reportBatch(Collections.singletonMap("event_report",Collections.singletonList("invalid"))).getCode());
        verify(devices,never()).reportCameras(any());
        when(devices.reportCameras(any())).thenThrow(new DataAccessResourceFailureException("synthetic database failure"));
        LocationTaskCompatController controller=new LocationTaskCompatController(single,mock(BladeTokenUserStore.class),reports);
        assertEquals(5003,controller.reportCameraEvents(Collections.singletonMap("event_report",Collections.singletonList(Collections.emptyMap()))).getCode());
        verifyNoInteractions(single);
    }
}
