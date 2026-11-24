package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class CloudAiExecutionScopeTest {
    @Test void singleAndMultiWorkersUseDifferentDurableClaimBoundaries() {
        for (String scope : new String[]{"localhost:single", "localhost:multi"}) {
            JdbcTemplate jdbc = mock(JdbcTemplate.class);
            CloudAiService service = mock(CloudAiService.class);
            when(service.executionScope()).thenReturn(scope);
            when(jdbc.queryForList(anyString(), eq(String.class), eq(scope), anyLong()))
                .thenReturn(Collections.singletonList("run"));
            CloudAiWorker worker = new CloudAiWorker(jdbc, service, mock(CloudAiSources.class),
                mock(CloudAiEvents.class), mock(CloudAiEvidenceStore.class), mock(ModelArtifactObjectStore.class),
                mock(ComputeNodeService.class), mock(ConfiguredComputeRegistry.class),
                mock(ComputeSharedQueueService.class), mock(ComputeSsh.class));
            worker.scan();
            verify(jdbc).queryForList(contains("JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.executionScope'))=?"),
                eq(String.class), eq(scope), anyLong());
            verify(jdbc).update(contains("id=? AND JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.executionScope'))=?"),
                anyString(), anyLong(), eq("run"), eq(scope), anyLong());
            worker.shutdown();
        }
    }

    @Test void schedulerFiltersBeforeTakingTheRowLock() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CloudAiService service = mock(CloudAiService.class);
        when(service.executionScope()).thenReturn("localhost:single");
        CloudAiScheduler scheduler = new CloudAiScheduler(jdbc, mock(TransactionTemplate.class), service);
        scheduler.occurrence("other-mode-task", 10L);
        verify(jdbc).queryForList(contains("JSON_UNQUOTE(JSON_EXTRACT(schedule_json,'$.executionScope'))=?"),
            eq("other-mode-task"), eq("localhost:single"), eq(10L));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void executionIdentityIsServerOwnedAndRejectsMissingEnvironment() {
        com.ruoyi.vlstream.test.vlstream.compute.ComputeSharedQueueService queue = mock(com.ruoyi.vlstream.test.vlstream.compute.ComputeSharedQueueService.class);
        CloudAiService service = new CloudAiService(null,null,null,null,queue,null,null);
        when(queue.executionEnvironment()).thenReturn("localhost");
        ReflectionTestUtils.setField(service,"executionMode","single");
        assertEquals("localhost:single",service.executionScope());
        ReflectionTestUtils.setField(service,"executionMode","multi");
        assertEquals("localhost:multi",service.executionScope());
        when(queue.executionEnvironment()).thenReturn(null);
        assertThrows(RuntimeException.class,service::executionScope);
    }

    @Test void sharedDetectionWaitsWithoutSshWhileDeploymentOwnsTheNode() {
        for (boolean device : new boolean[]{false, true}) {
            CentralRun run = new CentralRun(device);
            run.reconcile();
            verify(run.coordinator).acquire(run.node, run.ref);
            verifyNoInteractions(run.ssh);
            verify(run.coordinator, never()).enqueueRelease(anyLong(), anyString());
        }
    }

    @Test void queuedLegacySharedDetectionUsesTheDeploymentLeaseAfterCatalogTransition() {
        for (boolean device : new boolean[]{false, true}) {
            CentralRun run = new CentralRun(device);
            ((ObjectNode) run.snapshot.path("resource")).remove("executionRevision");
            run.row.put("snapshot_json", run.snapshot.toString());
            run.reconcile();
            verify(run.coordinator).acquire(run.node, run.ref);
            verifyNoInteractions(run.ssh);

            run.row.put("cancel_requested", true);
            when(run.jdbc.update(contains("SET state=?,message=?,ended_at=?"), eq("CANCELLED"), anyString(), anyLong(), eq("run"), anyString())).thenReturn(1);
            run.reconcile();
            verify(run.coordinator).enqueueRelease(10L, run.ref);
        }
    }

    @Test void cancellingBeforeDispatchSkipsAcquisitionAndEnqueuesReleaseWithTerminalWrite() {
        for (boolean device : new boolean[]{false, true}) {
            CentralRun run = new CentralRun(device);
            run.row.put("cancel_requested", true);
            when(run.jdbc.update(contains("SET state=?,message=?,ended_at=?"), eq("CANCELLED"), anyString(), anyLong(), eq("run"), anyString())).thenReturn(1);
            run.reconcile();
            verify(run.coordinator, never()).acquire(any(ComputeNode.class), anyString());
            verifyNoInteractions(run.ssh);
            org.mockito.InOrder order = inOrder(run.jdbc, run.coordinator, run.transactions);
            order.verify(run.transactions).getTransaction(any());
            order.verify(run.jdbc).update(contains("SET state=?,message=?,ended_at=?"), eq("CANCELLED"), anyString(), anyLong(), eq("run"), anyString());
            order.verify(run.coordinator).enqueueRelease(10L, run.ref);
            order.verify(run.transactions).commit(any());
        }
    }

    @Test void disconnectedSharedDetectionWithUncertainExitKeepsDeploymentOwnership() throws Exception {
        for (boolean device : new boolean[]{false, true}) {
            CentralRun run = new CentralRun(device);
            run.row.put("dispatched", 2);
            when(run.coordinator.acquire(run.node, run.ref)).thenReturn(true);
            when(run.ssh.open(run.node)).thenThrow(new java.io.IOException("disconnected"));
            when(run.jdbc.queryForObject(contains("SELECT dispatched"), eq(Integer.class), eq("run"))).thenReturn(2);
            run.reconcile();
            verify(run.jdbc).update(contains("SET state='UNKNOWN'"), eq("run"), anyString());
            verify(run.coordinator, never()).enqueueRelease(anyLong(), anyString());
        }
    }

    private static class CentralRun {
        final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        final ComputeNode node = new ComputeNode();
        final ComputeSsh ssh = mock(ComputeSsh.class);
        final ComputeExecutionCoordinator coordinator = mock(ComputeExecutionCoordinator.class);
        final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        final Map<String, Object> row = new HashMap<>();
        final ObjectNode snapshot;
        final String ref;
        final Object worker;

        CentralRun(boolean device) {
            String scope = "fixture:single";
            ref = device ? "device:run" : "cloudai:run";
            ObjectMapper json = new ObjectMapper();
            snapshot = json.createObjectNode().put("executionScope", scope).put("shared", true);
            snapshot.putObject("resource").put("nodeId", "10").put("executionRevision", 1).put("targetFingerprint", "target");
            row.put("tenant_id", "tenant");
            row.put("node_id", 10L);
            row.put("dispatched", 0);
            row.put("snapshot_json", snapshot.toString());
            node.setId(10L);
            node.setPlatformCatalogManaged(true);
            node.setWorkDir("/fixture");
            ComputeNodeService nodes = mock(ComputeNodeService.class);
            ConfiguredComputeRegistry configured = mock(ConfiguredComputeRegistry.class);
            ComputeSharedQueueService queue = mock(ComputeSharedQueueService.class);
            when(nodes.get(10L)).thenReturn(node);
            when(configured.canonicalId(10L)).thenReturn(10L);
            when(configured.snapshot(node)).thenReturn(Collections.singletonMap("targetFingerprint", "target"));
            when(queue.isShared(node)).thenReturn(true);
            when(jdbc.queryForMap(contains("SELECT *"), eq("run"), anyString())).thenReturn(row);
            when(coordinator.managed()).thenReturn(true);
            when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            if (device) {
                CloudDeviceService service = mock(CloudDeviceService.class);
                when(service.executionScope()).thenReturn(scope);
                when(service.read(anyString())).thenAnswer(call -> json.readTree(call.<String>getArgument(0)));
                worker = new CloudDeviceWorker(jdbc, service, mock(CloudAiSources.class), mock(CloudDeviceEvents.class),
                    mock(CloudAiEvidenceStore.class), mock(ModelArtifactObjectStore.class), nodes, configured, queue, ssh);
            } else {
                CloudAiService service = mock(CloudAiService.class);
                when(service.executionScope()).thenReturn(scope);
                when(service.read(anyString())).thenAnswer(call -> json.readTree(call.<String>getArgument(0)));
                worker = new CloudAiWorker(jdbc, service, mock(CloudAiSources.class), mock(CloudAiEvents.class),
                    mock(CloudAiEvidenceStore.class), mock(ModelArtifactObjectStore.class), nodes, configured, queue, ssh);
            }
            ReflectionTestUtils.setField(worker, "executionCoordinator", coordinator);
            ReflectionTestUtils.setField(worker, "transactions", transactions);
        }

        void reconcile() { ReflectionTestUtils.invokeMethod(worker, "reconcile", "run"); }
    }
}
