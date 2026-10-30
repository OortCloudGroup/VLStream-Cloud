package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class CloudAiEventsTest {
    private final ObjectMapper json = new ObjectMapper();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CloudAiService service = mock(CloudAiService.class);
    private final VlsEventReportApplicationService application = mock(VlsEventReportApplicationService.class);
    private final CloudAiLocalEventSink local = mock(CloudAiLocalEventSink.class);
    private final IVlsEventManagementService events = mock(IVlsEventManagementService.class);
    private final CloudAiEvents subject = new CloudAiEvents(jdbc, service, application, events, mock(DeviceMediaUploadService.class), local);
    private Map<String,Object> run;
    private ObjectNode report;
    private Map<String,String> evidence;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId("000000");
        run = new HashMap<>(); run.put("tenant_id","000000"); run.put("id","run"); run.put("scene_id","scene");
        run.put("planned_start",System.currentTimeMillis()-1000); run.put("snapshot_json","snapshot");
        when(service.read("snapshot")).thenReturn(json.readTree("{\"sceneName\":\"巡检\",\"cameras\":[{\"grantId\":\"cam\",\"deviceId\":\"ONVIF:27\",\"name\":\"11\"}],\"models\":[{\"modelId\":\"model\",\"labels\":{\"1\":\"未戴安全帽\"}}]}"));
        report = (ObjectNode) json.readTree("{\"id\":\"12345678-1234-1234-1234-123456789012\",\"cameraKey\":\"cam\",\"modelId\":\"model\",\"detections\":[{\"classId\":1,\"confidence\":0.91}],\"mediaState\":\"PENDING\"}");
        report.put("detectedAt",System.currentTimeMillis());
        evidence = new HashMap<>(); evidence.put("original","original"); evidence.put("annotated","annotated");
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @Test void singleTenantCreatesOnlyTheExistingActiveSafetyOwner() {
        subject.ingest(run, report, evidence);
        ArgumentCaptor<ActiveSafetyEventReport> captured = ArgumentCaptor.forClass(ActiveSafetyEventReport.class);
        verify(local).create(eq("000000"), anyLong(), captured.capture());
        assertEquals("未戴安全帽", captured.getValue().getEventType());
        assertEquals(Arrays.asList("annotated","original"), captured.getValue().getImageMediaIds());
        verify(application, never()).persistCloudAiEvent(any(),any());
        verify(local, never()).completeEvidence(anyString(),anyLong(),anyList(),anyList());
    }
    @Test void multiTenantDefersExternalDeliveryUntilVideoIsArchived() {
        when(application.isMultiTenant()).thenReturn(true);
        subject.ingest(run,report,evidence);
        verify(application).persistCloudAiEvent(any(),any());
        verify(application,never()).publishCloudAiEvidence(any(),any(),anyList(),anyList());
        verifyNoInteractions(local);
    }
    @Test void finalVideoUpdatesSameLocalEventAndDuplicateFinalReportDoesNothing() {
        Map<String,Object> existing = existing("PENDING");
        when(jdbc.queryForList(eq("SELECT * FROM vls_cloud_ai_event WHERE id=? FOR UPDATE"),anyString())).thenReturn(Collections.singletonList(existing));
        report.put("mediaState","READY"); evidence.put("video","video");
        subject.ingest(run,report,evidence);
        verify(local).completeEvidence("000000",123L,Arrays.asList("annotated","original"),Collections.singletonList("video"));
        verify(local,never()).create(anyString(),anyLong(),any());
        clearInvocations(local); existing.put("media_state","READY");
        subject.ingest(run,report,evidence);
        verifyNoInteractions(local);
    }
    @Test void crossTenantAndForgedCameraCannotWriteEvents() {
        run.put("tenant_id","other");
        assertThrows(RuntimeException.class,()->subject.ingest(run,report,evidence));
        run.put("tenant_id","000000"); report.put("cameraKey","other-camera");
        assertThrows(RuntimeException.class,()->subject.ingest(run,report,evidence));
        verifyNoInteractions(local,events,jdbc);
    }
    @Test void duplicateIdentityCannotMoveToAnotherRun() {
        Map<String,Object> existing = existing("PENDING"); existing.put("run_id","another-run");
        when(jdbc.queryForList(eq("SELECT * FROM vls_cloud_ai_event WHERE id=? FOR UPDATE"),anyString())).thenReturn(Collections.singletonList(existing));
        assertThrows(RuntimeException.class,()->subject.ingest(run,report,evidence));
        verifyNoInteractions(local,events);
    }
    @Test void longNativeChannelIdentityKeepsAStableDeviceKey() throws Exception {
        assertEquals("GB28181:27",CloudAiEvents.eventDeviceId(json.readTree("{\"protocol\":\"GB28181\",\"deviceRowId\":\"27\",\"deviceId\":\"GB28181:34020000002000000001/34020000001320000001\"}")));
        assertEquals("AETY-00-225T-S4AY-00000007",CloudAiEvents.eventDeviceId(json.readTree("{\"protocol\":\"VLSTREAM\",\"deviceRowId\":\"7\",\"deviceId\":\"AETY-00-225T-S4AY-00000007\"}")));
    }
    private Map<String,Object> existing(String state) {
        Map<String,Object> row = new HashMap<>(); row.put("tenant_id","000000"); row.put("run_id","run"); row.put("camera_key","cam");
        row.put("model_id","model"); row.put("media_state",state); row.put("event_id",123L); row.put("event_sink","LOCAL"); return row;
    }
}
