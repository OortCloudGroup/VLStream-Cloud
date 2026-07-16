package com.ruoyi.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.vlstream.domain.RecordEventStrategy;

/** Service contract for per-device camera event strategies. */
public interface IVlsRecordEventStrategyService extends IService<RecordEventStrategy> {

    /** Find the active strategy for a device code. */
    RecordEventStrategy getByDeviceId(String deviceId);

    /** Insert or update the unique strategy for a device code. */
    RecordEventStrategy saveOrUpdateStrategy(RecordEventStrategy strategy);

    /** Logically delete the strategy for a device code. */
    boolean deleteByDeviceId(String deviceId);
}
