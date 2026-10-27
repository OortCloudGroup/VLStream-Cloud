package com.ruoyi.vlstream.test.vlstream.service;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Tag("dev")
class ModelDispatchLocksTest {
    @Test void contentionDoesNotRunAndWatchdogLockIsReleasedOnFailure() {
        RedissonClient client = mock(RedissonClient.class); RLock lock = mock(RLock.class);
        when(client.getLock(anyString())).thenReturn(lock);
        ModelDispatchLocks locks = new ModelDispatchLocks(client);
        assertThrows(ServiceException.class, () -> locks.run("device:tenant-a:CAM-A", () -> { fail("must not execute"); return null; }));
        verify(lock, never()).unlock();
        when(lock.tryLock()).thenReturn(true); when(lock.isHeldByCurrentThread()).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> locks.run("device:tenant-a:CAM-A", () -> { throw new IllegalStateException(); }));
        verify(lock).unlock();
    }
}
