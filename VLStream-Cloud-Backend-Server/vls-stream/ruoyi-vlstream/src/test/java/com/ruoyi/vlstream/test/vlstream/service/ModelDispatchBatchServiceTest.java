package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real durable batch ledger in a fresh H2 database; all device and publication effects are fixtures. */
@Tag("dev")
class ModelDispatchBatchServiceTest {
    JdbcTemplate jdbc;
    ModelDispatchAccess access;
    ModelDispatchService dispatch;
    ModelDispatchTaskService tasks;
    ModelDispatchLocks locks;
    ModelDispatchBatchService service;
    Map<String, ModelDispatchTask> deviceTasks;
    ModelDispatchService.DeploymentSnapshot snapshot;

    @BeforeEach void setup() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:batch-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE vls_model_dispatch_batch (operation_id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),owner_id VARCHAR(64),request_hash VARCHAR(64),snapshot_json TEXT,items_json TEXT,retries_json TEXT,create_time TIMESTAMP,update_time TIMESTAMP)");
        access = mock(ModelDispatchAccess.class); when(access.tenant()).thenReturn("tenant-a"); when(access.owner()).thenReturn("alice");
        dispatch = mock(ModelDispatchService.class); tasks = mock(ModelDispatchTaskService.class); locks = mock(ModelDispatchLocks.class);
        when(locks.run(any(), any())).thenAnswer(call -> ((java.util.function.Supplier<?>) call.getArgument(1)).get());
        deviceTasks = new HashMap<>();
        when(tasks.getByRequestId(anyString())).thenAnswer(call -> deviceTasks.get(call.getArgument(0)));
        snapshot = new ModelDispatchService.DeploymentSnapshot(); snapshot.setTenantId("tenant-a"); snapshot.setAlgorithmId(7L);
        snapshot.setTrainingId(70L); snapshot.setModelType("om"); snapshot.setSha256("locked-hash"); snapshot.setRemotePath("fixture-secret-path");
        when(dispatch.prepare(any(), any(), anyString())).thenReturn(snapshot);
        when(dispatch.dispatchSnapshot(any(), anyString(), anyString())).thenAnswer(call -> {
            ModelDispatchTask task = new ModelDispatchTask(); task.setRequestId(call.getArgument(2)); task.setDeviceId(call.getArgument(1));
            task.setTenantId("tenant-a"); task.setDispatchStatus("PUBLISHED"); task.setDownloadExpiresAt(System.currentTimeMillis()/1000 + 1800);
            deviceTasks.put(task.getRequestId(), task); return task.getRequestId();
        });
        service = new ModelDispatchBatchService(jdbc, new ObjectMapper(), dispatch, tasks, access, locks);
    }
    ModelDispatchBatchService.Submit request(String... ids) {
        ModelDispatchBatchService.Submit request = new ModelDispatchBatchService.Submit();
        request.setOperationId(UUID.randomUUID().toString()); request.setAlgorithmId(7L); request.setDeviceIds(Arrays.asList(ids)); return request;
    }
    ModelDispatchBatchService.Retry retry(String... ids) {
        ModelDispatchBatchService.Retry request = new ModelDispatchBatchService.Retry();
        request.setRetryId(UUID.randomUUID().toString()); request.setDeviceIds(Arrays.asList(ids)); return request;
    }
    @Test void duplicateSubmitAndReorderedTargetsNeverDispatchAgain() {
        ModelDispatchBatchService.Submit input = request("CAM-B", "CAM-A");
        ModelDispatchBatchService.View batch = service.submit(input);
        input.setDeviceIds(Arrays.asList("CAM-A", "CAM-B"));
        ModelDispatchBatchService.View same = service.submit(input);
        assertEquals(batch.getItems().get(0).getRequestId(), same.getItems().get(0).getRequestId());
        assertEquals(0, same.getSucceeded()); assertEquals(2, same.getPending());
        verify(dispatch, times(2)).dispatchSnapshot(eq(snapshot), anyString(), anyString());
        verify(dispatch, times(1)).prepare(7L, null, "om");
    }
    @Test void sameKeyDifferentModelOrTargetsRejected() {
        ModelDispatchBatchService.Submit input = request("CAM-A"); service.submit(input);
        input.setAlgorithmId(8L); assertThrows(ServiceException.class, () -> service.submit(input));
        input.setAlgorithmId(7L); input.setDeviceIds(Collections.singletonList("CAM-B")); assertThrows(ServiceException.class, () -> service.submit(input));
        verify(dispatch, times(1)).dispatchSnapshot(any(), anyString(), anyString());
    }
    @Test void partialFailureIsVisibleAndOnlyTheFailedTargetCanBeRetriedWithFrozenSelection() {
        when(dispatch.dispatchSnapshot(any(), eq("CAM-B"), anyString())).thenThrow(new ServiceException("设备离线"));
        ModelDispatchBatchService.View batch = service.submit(request("CAM-A", "CAM-B"));
        assertEquals(1, batch.getFailed()); assertEquals(1, batch.getPending()); assertEquals(0, batch.getSucceeded());
        assertEquals("设备离线", batch.getItems().get(1).getReason()); assertTrue(batch.getItems().get(1).isRetryable());
        assertThrows(ServiceException.class, () -> service.retry(batch.getOperationId(), retry("CAM-A")));
        ModelDispatchBatchService.Retry retry = retry("CAM-B");
        service.retry(batch.getOperationId(), retry); service.retry(batch.getOperationId(), retry);
        verify(dispatch, times(1)).dispatchSnapshot(any(), eq("CAM-A"), anyString());
        verify(dispatch, times(2)).dispatchSnapshot(argThat(value -> value.getTrainingId().equals(70L)), eq("CAM-B"), anyString());
        verify(dispatch, times(1)).prepare(any(), any(), anyString());
    }
    @Test void successIsOnlyHardwareSuccessAndTerminalSuccessCannotBeRetried() {
        ModelDispatchBatchService.View batch = service.submit(request("CAM-A"));
        deviceTasks.get(batch.getItems().get(0).getRequestId()).setDispatchStatus("SUCCESS");
        ModelDispatchBatchService.View current = service.get(batch.getOperationId());
        assertEquals(1, current.getSucceeded()); assertEquals(0, current.getPending());
        assertThrows(ServiceException.class, () -> service.retry(batch.getOperationId(), retry("CAM-A")));
    }
    @Test void timeoutOrPublishUncertaintyNeverPermitsRetry() {
        ModelDispatchBatchService.View batch = service.submit(request("CAM-A"));
        deviceTasks.get(batch.getItems().get(0).getRequestId()).setDownloadExpiresAt(1L);
        ModelDispatchBatchService.View current = service.get(batch.getOperationId());
        assertEquals(1, current.getUnknown()); assertFalse(current.getItems().get(0).isRetryable());
        assertThrows(ServiceException.class, () -> service.retry(batch.getOperationId(), retry("CAM-A")));
    }
    @Test void lostPublishResponseRetainsTaskAndRejectsRetry() {
        when(dispatch.dispatchSnapshot(any(), anyString(), anyString())).thenAnswer(call -> {
            ModelDispatchTask task = new ModelDispatchTask(); task.setDeviceId(call.getArgument(1)); task.setRequestId(call.getArgument(2));
            task.setTenantId("tenant-a"); task.setDispatchStatus("UNKNOWN"); deviceTasks.put(task.getRequestId(), task);
            throw new ServiceException("MQTT transport lost");
        });
        ModelDispatchBatchService.View batch = service.submit(request("CAM-A"));
        assertEquals(1, batch.getUnknown()); assertFalse(batch.getItems().get(0).isRetryable());
        assertThrows(ServiceException.class, () -> service.retry(batch.getOperationId(), retry("CAM-A")));
    }
    @Test void explicitHardwareFailureCanBeRetriedButUnknownTargetCannot() {
        ModelDispatchBatchService.View batch = service.submit(request("CAM-A"));
        deviceTasks.get(batch.getItems().get(0).getRequestId()).setDispatchStatus("FAILED");
        service.retry(batch.getOperationId(), retry("CAM-A"));
        assertThrows(ServiceException.class, () -> service.retry(batch.getOperationId(), retry("CAM-X")));
        verify(dispatch, times(2)).dispatchSnapshot(any(), anyString(), anyString());
    }
    @Test void freshServiceRestoresFromDatabaseAndProtectsTenantAndOwner() {
        ModelDispatchBatchService.View batch = service.submit(request("CAM-A"));
        ModelDispatchBatchService restarted = new ModelDispatchBatchService(jdbc, new ObjectMapper(), dispatch, tasks, access, locks);
        assertEquals(1, restarted.get(batch.getOperationId()).getPending()); assertEquals(1, restarted.recent().size());
        when(access.owner()).thenReturn("bob"); assertTrue(restarted.recent().isEmpty());
        assertThrows(ServiceException.class, () -> restarted.get(batch.getOperationId()));
        when(access.owner()).thenReturn("alice"); when(access.tenant()).thenReturn("tenant-b");
        assertThrows(ServiceException.class, () -> restarted.get(batch.getOperationId())); assertTrue(restarted.recent().isEmpty());
    }
    @Test void viewDoesNotExposeArtifactPathOrSnapshot() throws Exception {
        String result = new ObjectMapper().writeValueAsString(service.submit(request("CAM-A")));
        assertFalse(result.contains("fixture-secret-path")); assertFalse(result.contains("remotePath")); assertFalse(result.contains("snapshot"));
    }
    @Test void invalidOrOversizeBatchNeverReadsArtifactsOrDispatches() {
        assertThrows(ServiceException.class, () -> service.submit(request()));
        assertThrows(ServiceException.class, () -> service.submit(request("CAM-A", "CAM-A")));
        assertThrows(ServiceException.class, () -> service.submit(request("../CAM")));
        ModelDispatchBatchService.Submit input = request("CAM-A"); input.setDeviceIds(Collections.nCopies(21, "CAM-A"));
        assertThrows(ServiceException.class, () -> service.submit(input));
        ModelDispatchBatchService.Submit invalid = request("CAM-A"); invalid.setModelId(8L);
        assertThrows(ServiceException.class, () -> service.submit(invalid)); verifyNoInteractions(dispatch);
    }
    @Test void lockContentionFailsBeforePublication() {
        doThrow(new ServiceException("正在处理")).when(locks).run(any(), any());
        assertThrows(ServiceException.class, () -> service.submit(request("CAM-A"))); verifyNoInteractions(dispatch);
    }
}
