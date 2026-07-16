/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.StringUtil;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRecordEventStrategyMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RecordEventStrategy;
import com.ruoyi.vlstream.test.vlstream.service.IVlsRecordEventStrategyService;
import org.springframework.stereotype.Service;

/**
 * 摄像头事件策略 服务实现类
 */
@Service
public class VlsRecordEventStrategyServiceImpl extends BaseServiceImpl<VlsRecordEventStrategyMapper, RecordEventStrategy>
	implements IVlsRecordEventStrategyService {

	@Override
	public RecordEventStrategy getByDeviceId(String deviceId) {
		if (StringUtil.isBlank(deviceId)) {
			return null;
		}
		QueryWrapper<RecordEventStrategy> wrapper = new QueryWrapper<>();
		wrapper.eq("device_id", deviceId);
		return getOne(wrapper);
	}

	@Override
	public boolean saveOrUpdateStrategy(RecordEventStrategy recordEventStrategy) {
		if (recordEventStrategy == null || StringUtil.isBlank(recordEventStrategy.getDeviceId())) {
			return false;
		}
		RecordEventStrategy existing = getByDeviceId(recordEventStrategy.getDeviceId());
		if (existing != null) {
			UpdateWrapper<RecordEventStrategy> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("device_id", recordEventStrategy.getDeviceId());
			recordEventStrategy.setId(existing.getId());
			return update(recordEventStrategy, updateWrapper);
		}
		return save(recordEventStrategy);
	}

	@Override
	public boolean deleteByDeviceId(String deviceId) {
		if (StringUtil.isBlank(deviceId)) {
			return false;
		}
		QueryWrapper<RecordEventStrategy> wrapper = new QueryWrapper<>();
		wrapper.eq("device_id", deviceId);
		return remove(wrapper);
	}
}
