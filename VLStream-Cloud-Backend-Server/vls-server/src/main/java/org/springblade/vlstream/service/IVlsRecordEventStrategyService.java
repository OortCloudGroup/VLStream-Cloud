package org.springblade.vlstream.service;

import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.pojo.entity.RecordEventStrategy;

/**
 * Camera event strategy service class
 */
public interface IVlsRecordEventStrategyService extends BaseService<RecordEventStrategy> {

	RecordEventStrategy getByDeviceId(String deviceId);

	boolean saveOrUpdateStrategy(RecordEventStrategy recordEventStrategy);

	boolean deleteByDeviceId(String deviceId);

}
