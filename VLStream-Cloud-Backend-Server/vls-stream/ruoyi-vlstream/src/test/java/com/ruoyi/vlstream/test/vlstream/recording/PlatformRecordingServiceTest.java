package com.ruoyi.vlstream.test.vlstream.recording;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiSources;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class PlatformRecordingServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RecordingDeviceAccess access = mock(RecordingDeviceAccess.class);
    private final PlatformRecordingService service = new PlatformRecordingService(jdbc, new ObjectMapper(), mock(CloudAiSources.class), access);
    @BeforeEach void setup() { TenantContextHolder.setTenantId("tenant-a"); }
    @AfterEach void cleanup() { service.close(); TenantContextHolder.clear(); }

    @Test void revokedDevicePermissionPreventsListingEvenIfTenantMatches() {
        doThrow(new ServiceException("denied", 403)).when(access).require("VLSTREAM", "1");
        assertThrows(ServiceException.class, () -> service.list("VLSTREAM", "1"));
        verifyNoInteractions(jdbc);
    }
    @Test void signallingAddressNeverOverridesOfflineOrUnknownVlstreamState() throws Exception {
        ObjectMapper json = new ObjectMapper();
        assertThrows(ServiceException.class, () -> PlatformRecordingService.requireOnline(json.readTree("{\"protocol\":\"VLSTREAM\",\"online\":false,\"socketUrl\":\"ws://camera.example\"}")));
        assertThrows(ServiceException.class, () -> PlatformRecordingService.requireOnline(json.readTree("{\"protocol\":\"VLSTREAM\",\"socketUrl\":\"ws://camera.example\"}")));
        assertDoesNotThrow(() -> PlatformRecordingService.requireOnline(json.readTree("{\"protocol\":\"VLSTREAM\",\"online\":true}")));
    }
    @Test void tenantBoundaryIsAppliedBeforeStopping() {
        when(jdbc.queryForList(anyString(), eq("source-b"), eq("tenant-a"))).thenReturn(Collections.emptyList());
        assertThrows(ServiceException.class, () -> service.manual("source-b", false));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verifyNoInteractions(access);
    }
    @Test void currentDeviceAccessIsRecheckedForSharedSourceMutation() {
        Map<String,Object> row = new HashMap<>(); row.put("protocol", "ONVIF"); row.put("device_row_id", "29");
        when(jdbc.queryForList(anyString(), eq("shared-source"), eq("tenant-a"))).thenReturn(Collections.singletonList(row));
        doThrow(new ServiceException("revoked", 403)).when(access).require("ONVIF", "29");
        assertThrows(ServiceException.class, () -> service.manual("shared-source", false));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void stoppingManualDoesNotOverwriteScheduledDemand() {
        Map<String,Object> row = new HashMap<>(); row.put("protocol", "ONVIF"); row.put("device_row_id", "29");
        when(jdbc.queryForList(anyString(), eq("shared-source"), eq("tenant-a"))).thenReturn(Collections.singletonList(row));
        service.manual("shared-source", false);
        verify(access).require("ONVIF", "29");
        verify(jdbc).update("UPDATE vls_platform_record_source SET manual_enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", false, "shared-source", "tenant-a");
    }
}
