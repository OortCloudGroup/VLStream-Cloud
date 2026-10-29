package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeQueueSnapshotServiceTest {
    private final ComputeNodeService nodes = mock(ComputeNodeService.class);
    private final ComputeSharedQueueService shared = mock(ComputeSharedQueueService.class);
    private final ComputeSsh ssh = mock(ComputeSsh.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ObjectMapper json = new ObjectMapper();
    private final ComputeQueueWaitEstimator estimator = mock(ComputeQueueWaitEstimator.class);
    private ComputeQueueSnapshotService service;
    private ComputeNode node;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId("000000");
        service = new ComputeQueueSnapshotService(nodes, shared, new ComputeSharedQueueProperties(), ssh, jdbc, json, estimator) {
            @Override protected String authenticatedUser() { return "alice"; }
        };
        node = new ComputeNode(); node.setId(-99L); node.setEnabled(true); node.setResourceScope("PLATFORM"); node.setBillingType("FREE");
        when(shared.authenticated()).thenReturn(true); when(shared.isShared(node)).thenReturn(true); when(shared.viewer()).thenReturn("owner");
        when(nodes.get(-99L)).thenReturn(node); when(nodes.queueIds(-99L)).thenReturn(Arrays.asList(-99L, 123L));
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    private String raw(String phase, String id, int count) {
        return "{\"state\":\"KNOWN\",\"gpuUuid\":\"GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac\",\"waitingCount\":" + count + ",\"activePhase\":" + phase + ",\"activeJobId\":" + id + ",\"checkedAt\":" + System.currentTimeMillis() + "}";
    }
    @Test void runningAndArchivingAreSeparateFromWaitingAndHaveNoFabricatedEta() throws Exception {
        for (String phase : new String[]{"RUNNING", "ARCHIVING"}) {
            ComputeQueueSnapshot result = service.project(json.readTree(raw("\"" + phase + "\"", "null", 15)), node);
            assertEquals("KNOWN", result.getState()); assertEquals(phase, result.getActivePhase()); assertEquals(15, result.getWaitingCount());
            assertNull(result.getActiveTaskName()); assertNull(result.getEstimatedWaitSeconds());
        }
        verifyNoInteractions(jdbc);
    }
    @Test void absentOrMalformedDataNeverBecomesAnEmptyQueue() throws Exception {
        for (String value : new String[]{"{}", raw("null", "null", -1), raw("\"DONE\"", "null", 0), raw("null", "null", 0).replace("\"checkedAt\":", "\"old\":"), raw("null", "null", 50001)})
            assertEquals("UNKNOWN", service.project(json.readTree(value), node).getState());
    }
    @Test void emptyRegisteredQueueDoesNotInventAGpuReadinessOrWaitPromise() throws Exception {
        ComputeQueueSnapshot result = service.project(json.readTree(raw("null", "null", 0)), node);
        assertEquals(0, result.getWaitingCount()); assertNull(result.getActivePhase()); assertNull(result.getEstimatedWaitSeconds());
    }
    @Test void ownedNameQueryBindsTenantUserSubjectAndCanonicalAliases() throws Exception {
        String id = "12345678-1234-1234-1234-123456789abc";
        when(jdbc.queryForList(anyString(), eq(String.class), eq(id), eq("000000"), eq("alice"), eq("owner"), eq(-99L), eq(123L))).thenReturn(Collections.singletonList("未带安全帽"));
        ComputeQueueSnapshot result = service.project(json.readTree(raw("\"RUNNING\"", "\"" + id + "\"", 2)), node);
        assertEquals("未带安全帽", result.getActiveTaskName());
        verify(jdbc).queryForList(contains("BINARY t.tenant_id=BINARY ? AND BINARY t.create_user=BINARY ?"), eq(String.class), eq(id), eq("000000"), eq("alice"), eq("owner"), eq(-99L), eq(123L));
    }
    @Test void foreignOrDeletedTaskNeverGetsANameEvenWhenHostSuppliesAnId() throws Exception {
        ComputeQueueSnapshot result = service.project(json.readTree(raw("\"RUNNING\"", "\"12345678-1234-1234-1234-123456789abc\"", 2)), node);
        assertNull(result.getActiveTaskName());
        assertFalse(json.writeValueAsString(result).contains("activeJobId"));
    }
    @Test void stoppedNodeReturnsUnknownWithoutSsh() {
        node.setPowerState("STOPPED");
        assertEquals("UNKNOWN", service.snapshot(-99L).getState()); verifyNoInteractions(ssh);
    }
    @Test void unauthenticatedAndPrivateRequestsAreRejectedWithoutSsh() {
        when(shared.authenticated()).thenReturn(false); assertThrows(RuntimeException.class, () -> service.snapshot(-99L));
        when(shared.authenticated()).thenReturn(true); node.setResourceScope("PRIVATE"); assertThrows(RuntimeException.class, () -> service.snapshot(-99L));
        verifyNoInteractions(ssh);
    }
    @Test void unapprovedTargetCannotUseSshOrTaskOperations() {
        doThrow(new ServiceException("unapproved")).when(shared).requireTarget(node);
        assertEquals("UNKNOWN", service.snapshot(-99L).getState()); verifyNoInteractions(ssh); verifyNoInteractions(jdbc);
    }
    @Test void snapshotRunsInMemoryAndNeverUploadsOrMutatesTaskHelper() throws Exception {
        node.setPowerState("RUNNING");
        when(shared.pythonCommand(node)).thenReturn("python");
        ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class);
        when(ssh.open(node)).thenReturn(connection);
        when(connection.execute(startsWith("nvidia-smi "), eq(10))).thenReturn("GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac");
        when(connection.execute(contains(" -B -c "), eq(10))).thenReturn("VLS_QUEUE_SNAPSHOT=" + raw("null", "null", 0));
        assertEquals("KNOWN", service.snapshot(-99L).getState());
        verify(connection).execute(contains(" -B -c "), eq(10));
        verify(connection, never()).putText(anyString(), anyString());
        verify(connection).close();
    }
    @Test void unknownStaleResourceCanReadQueueWithoutBecomingTrainable() throws Exception {
        node.setPowerState("UNKNOWN"); node.setProbeState("UNTESTED");
        when(shared.pythonCommand(node)).thenReturn("python");
        ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class); when(ssh.open(node)).thenReturn(connection);
        when(connection.execute(startsWith("nvidia-smi "), eq(10))).thenReturn("GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac");
        when(connection.execute(contains(" -B -c "), eq(10))).thenReturn("VLS_QUEUE_SNAPSHOT=" + raw("null", "null", 0));
        ComputeQueueSnapshot result = service.snapshot(-99L);
        assertEquals("KNOWN", result.getState()); assertEquals(0, result.getWaitingCount()); assertEquals("UNKNOWN", result.getExternalOccupancy());
        assertEquals("UNKNOWN", node.getPowerState()); assertEquals("UNTESTED", node.getProbeState()); assertNull(node.getProbeJson()); assertNull(node.getProbeTime());
        verify(shared, never()).proof(any()); verify(nodes, never()).probeSnapshot(any()); verify(nodes, never()).requestedProbe(any());
        verifyNoInteractions(jdbc); verify(connection, never()).putText(anyString(), anyString());
    }
    @Test void metadataOrRegistryUuidMismatchNeverBecomesKnown() throws Exception {
        when(shared.pythonCommand(node)).thenReturn("python");
        ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class); when(ssh.open(node)).thenReturn(connection);
        when(connection.execute(startsWith("nvidia-smi "), eq(10))).thenReturn("not-a-uuid");
        assertEquals("UNKNOWN", service.snapshot(-99L).getState()); verify(connection, never()).execute(contains(" -B -c "), anyInt());
        when(connection.execute(startsWith("nvidia-smi "), eq(10))).thenReturn("GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac");
        when(connection.execute(contains(" -B -c "), eq(10))).thenReturn("VLS_QUEUE_SNAPSHOT=" + raw("null", "null", 0).replace("24fb011a", "34fb011a"));
        assertEquals("UNKNOWN", service.snapshot(-99L).getState());
    }
    @Test void failedSshReturnsUnknownWithoutChangingHealth() throws Exception {
        node.setPowerState("UNKNOWN"); when(ssh.open(node)).thenThrow(new java.io.IOException("unavailable"));
        assertEquals("UNKNOWN", service.snapshot(-99L).getState()); assertEquals("UNKNOWN", node.getPowerState()); assertNull(node.getProbeTime());
        verify(shared, never()).proof(any()); verifyNoInteractions(jdbc);
    }
    @Test void sameActorInAnotherTenantCannotNameTheOriginalTenantsTask() throws Exception {
        String id = "12345678-1234-1234-1234-123456789abc";
        when(jdbc.queryForList(anyString(), eq(String.class), eq(id), eq("000000"), eq("alice"), eq("owner"), eq(-99L), eq(123L))).thenReturn(Collections.singletonList("私人任务"));
        assertEquals("私人任务", service.project(json.readTree(raw("\"RUNNING\"", "\"" + id + "\"", 0)), node).getActiveTaskName());
        TenantContextHolder.setTenantId("other-tenant");
        assertNull(service.project(json.readTree(raw("\"RUNNING\"", "\"" + id + "\"", 0)), node).getActiveTaskName());
        verify(jdbc).queryForList(anyString(), eq(String.class), eq(id), eq("other-tenant"), eq("alice"), eq("owner"), eq(-99L), eq(123L));
    }
    @Test void supportedEstimateProjectsOnlyPublicTimingEvidence() throws Exception {
        when(estimator.observe(anyString(), any())).thenReturn(new ComputeQueueWaitEstimator.Estimate(3600, 3));
        ComputeQueueSnapshot result = service.project(json.readTree(raw("\"RUNNING\"", "null", 15)), node);
        assertEquals(3600L, result.getEstimatedWaitSeconds()); assertEquals(3, result.getEstimationSampleCount());
        assertEquals("MATCHED_OBSERVED_LEASE_P75", result.getEstimationBasis());
        String serialized = json.writeValueAsString(result);
        for (String internal : new String[]{"activeTicketKey", "activeProfile", "waitingProfiles", "previousTicketKey", "activeJobId", "runDir", "owner"}) assertFalse(serialized.contains(internal));
    }
}
