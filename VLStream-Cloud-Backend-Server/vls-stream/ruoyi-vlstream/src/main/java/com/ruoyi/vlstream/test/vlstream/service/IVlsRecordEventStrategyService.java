/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RecordEventStrategy;

/**
 * 摄像头事件策略 服务类
 */
public interface IVlsRecordEventStrategyService extends BaseService<RecordEventStrategy> {

	RecordEventStrategy getByDeviceId(String deviceId);

	boolean saveOrUpdateStrategy(RecordEventStrategy recordEventStrategy);

	boolean deleteByDeviceId(String deviceId);

}
