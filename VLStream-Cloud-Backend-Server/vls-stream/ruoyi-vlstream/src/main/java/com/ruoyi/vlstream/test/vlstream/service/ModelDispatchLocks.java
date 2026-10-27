package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import java.util.function.Supplier;

/** Shared watchdog locks; task rows protect an in-flight device after a process restart. */
@Service
@RequiredArgsConstructor
public class ModelDispatchLocks {
    private final RedissonClient redisson;

    public <T> T run(String key, Supplier<T> work) {
        RLock lock = redisson.getLock("vls:model-dispatch:" + key);
        if (!lock.tryLock()) throw new ServiceException("该批次或设备正在处理，请刷新结果后重试");
        try { return work.get(); }
        finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }
}
