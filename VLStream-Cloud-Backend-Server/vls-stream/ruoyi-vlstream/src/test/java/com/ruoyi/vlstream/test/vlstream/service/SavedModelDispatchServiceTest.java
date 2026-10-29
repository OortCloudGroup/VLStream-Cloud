package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SavedModelDispatchServiceTest {
    private static final String OP = "742eb2a1-414c-4f7b-a25b-67e84d05ad79";
    private ModelDispatchService dispatcher;
    private ModelDispatchTaskService tasks;
    private ModelDispatchAccess access;
    private ModelDispatchLocks locks;
    private SavedModelDispatchService service;
    private final Map<String, ModelDispatchTask> rows = new HashMap<>();

    @BeforeEach void setup() {
        rows.clear();
        dispatcher = mock(ModelDispatchService.class);
        tasks = mock(ModelDispatchTaskService.class);
        access = mock(ModelDispatchAccess.class);
        locks = mock(ModelDispatchLocks.class);
        service = new SavedModelDispatchService(dispatcher, tasks, access, locks);
        when(access.tenant()).thenReturn("tenant-a");
        when(access.owner()).thenReturn("owner-a");
        when(locks.run(anyString(), any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(1)).get());
        when(tasks.getByRequestId(anyString())).thenAnswer(call -> rows.get(call.getArgument(0)));
        when(dispatcher.dispatchSavedModel(anyLong(), anyString(), anyString())).thenAnswer(call -> {
            String id = call.getArgument(2);
            ModelDispatchTask task = new ModelDispatchTask();
            task.setRequestId(id); task.setDeviceId(call.getArgument(1)); task.setDispatchStatus("PUBLISHED");
            rows.put(id, task);
            return id;
        });
    }

    @Test void twoModelsAndTwoDevicesHaveFourIndependentDurableTasks() {
        for (long model : new long[]{1L, 2L}) for (String device : new String[]{"CAM-A", "CAM-B"}) {
            assertEquals("PUBLISHED", service.submit(model, device, OP).getState());
        }
        assertEquals(4, rows.size());
        verify(access, times(2)).requireDevice("CAM-A", "om", false);
        verify(access, times(2)).requireDevice("CAM-B", "om", false);
    }

    @Test void duplicateOrLostHttpResponseRecoversOriginalWithoutPublishingAgain() {
        String first = service.submit(1L, "CAM-A", OP).getRequestId();
        assertEquals(first, service.submit(1L, "CAM-A", OP.toUpperCase()).getRequestId());
        verify(dispatcher, times(1)).dispatchSavedModel(eq(1L), eq("CAM-A"), anyString());
        verify(access, times(2)).requireDevice("CAM-A", "om", false);
    }

    @Test void tenantAndOwnerArePartOfIdempotencyScope() {
        String first = service.submit(1L, "CAM-A", OP).getRequestId();
        when(access.owner()).thenReturn("owner-b");
        String second = service.submit(1L, "CAM-A", OP).getRequestId();
        when(access.tenant()).thenReturn("tenant-b");
        String third = service.submit(1L, "CAM-A", OP).getRequestId();
        assertNotEquals(first, second); assertNotEquals(second, third);
    }

    @Test void deniedDeviceDoesNotPublishAndOtherCombinationsContinue() {
        when(access.requireDevice("CAM-B", "om", false)).thenThrow(new ServiceException("无权访问设备"));
        SavedModelDispatchService.Result failed = service.submit(1L, "CAM-B", OP);
        assertEquals("FAILED", failed.getState()); assertTrue(failed.isRetryable());
        assertTrue(failed.getMessage().contains("无权"));
        verify(dispatcher, never()).dispatchSavedModel(eq(1L), eq("CAM-B"), anyString());
        assertEquals("PUBLISHED", service.submit(1L, "CAM-A", OP).getState());
    }

    @Test void deniedModelOrTrainingDoesNotPublish() {
        when(dispatcher.requireSavedModelAccess(1L, "CAM-A")).thenThrow(new ServiceException("模型训练不属于当前用户"));
        assertEquals("FAILED", service.submit(1L, "CAM-A", OP).getState());
        verify(dispatcher, never()).dispatchSavedModel(anyLong(), anyString(), anyString());
    }

    @Test void publishExceptionWithExistingTaskIsUncertainAndCannotRetryAsFailure() {
        when(dispatcher.dispatchSavedModel(anyLong(), anyString(), anyString())).thenAnswer(call -> {
            ModelDispatchTask task = new ModelDispatchTask(); task.setRequestId(call.getArgument(2));
            task.setDispatchStatus("UNKNOWN"); rows.put(task.getRequestId(), task);
            throw new ServiceException("发布超时");
        });
        SavedModelDispatchService.Result result = service.submit(1L, "CAM-A", OP);
        assertEquals("UNKNOWN", result.getState()); assertFalse(result.isRetryable());
        assertEquals(result.getRequestId(), service.submit(1L, "CAM-A", OP).getRequestId());
        verify(dispatcher, times(1)).dispatchSavedModel(anyLong(), anyString(), anyString());
    }

    @Test void databaseReadFailureNeverLooksLikeSafeFailure() {
        when(tasks.getByRequestId(anyString())).thenThrow(new ServiceException("database unavailable"));
        assertThrows(ServiceException.class, () -> service.submit(1L, "CAM-A", OP));
        verify(dispatcher, never()).dispatchSavedModel(anyLong(), anyString(), anyString());
    }

    @Test void expiredTaskIsUnknownAndOnlyVerifiedTerminalFailureMayRetry() {
        String id = service.submit(1L, "CAM-A", OP).getRequestId();
        rows.get(id).setDownloadExpiresAt(1L);
        assertEquals("UNKNOWN", service.get(1L, "CAM-A", OP).getState());
        assertFalse(service.get(1L, "CAM-A", OP).isRetryable());
        rows.get(id).setDispatchStatus("FAILED");
        assertTrue(service.get(1L, "CAM-A", OP).isRetryable());
        rows.get(id).setDispatchStatus("SUCCESS");
        assertEquals("SUCCESS", service.get(1L, "CAM-A", OP).getState());
        assertFalse(service.get(1L, "CAM-A", OP).isRetryable());
        verify(dispatcher, times(1)).dispatchSavedModel(anyLong(), anyString(), anyString());
    }

    @Test void revokedPermissionOnRecoveryNeverPublishesAgain() {
        service.submit(1L, "CAM-A", OP);
        when(access.requireDevice("CAM-A", "om", false)).thenThrow(new ServiceException("权限已撤销"));
        assertFalse(service.submit(1L, "CAM-A", OP).isRetryable());
        assertThrows(ServiceException.class, () -> service.get(1L, "CAM-A", OP));
        verify(dispatcher, times(1)).dispatchSavedModel(anyLong(), anyString(), anyString());
    }

    @Test void concurrentAttemptLockFailureNeverBecomesRetryable() {
        doThrow(new ServiceException("处理中")).when(locks).run(anyString(), any());
        assertThrows(ServiceException.class, () -> service.submit(1L, "CAM-A", OP));
        verifyNoInteractions(dispatcher, tasks);
    }

    @Test void invalidRequestCannotPublishAndReadDoesNotCreate() {
        assertThrows(ServiceException.class, () -> service.submit(1L, "CAM-A", "bad"));
        assertThrows(ServiceException.class, () -> service.submit(1L, "", OP));
        assertThrows(ServiceException.class, () -> service.get(1L, "CAM-A", OP));
        verify(dispatcher, never()).dispatchSavedModel(anyLong(), anyString(), anyString());
    }
}
