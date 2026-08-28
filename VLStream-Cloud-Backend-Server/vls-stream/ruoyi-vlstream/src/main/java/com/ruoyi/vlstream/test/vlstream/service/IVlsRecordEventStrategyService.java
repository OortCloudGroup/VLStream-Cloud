/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RecordEventStrategy;

/**
 * event service
 */
public interface IVlsRecordEventStrategyService extends BaseService<RecordEventStrategy> {

	RecordEventStrategy getByDeviceId(String deviceId);

	boolean saveOrUpdateStrategy(RecordEventStrategy recordEventStrategy);

	boolean deleteByDeviceId(String deviceId);

}
