/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTimeStrategyExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsTimeStrategyMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsTimeStrategyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 时间策略表 服务实现类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Service
public class VlsTimeStrategyServiceImpl extends BaseServiceImpl<VlsTimeStrategyMapper, TimeStrategy> implements IVlsTimeStrategyService {

	private final ObjectProvider<VlsRtspRecordingManager> rtspRecordingManagerProvider;

	public VlsTimeStrategyServiceImpl(ObjectProvider<VlsRtspRecordingManager> rtspRecordingManagerProvider) {
		this.rtspRecordingManagerProvider = rtspRecordingManagerProvider;
	}

	@Override
	public IPage<TimeStrategyVO> selectVlsTimeStrategyPage(IPage<TimeStrategyVO> page, TimeStrategyVO vlsTimeStrategy) {
		return page.setRecords(baseMapper.selectVlsTimeStrategyPage(page, vlsTimeStrategy));
	}

	@Override
	public List<VlsTimeStrategyExcel> exportVlsTimeStrategy(Wrapper<TimeStrategy> queryWrapper) {
		List<VlsTimeStrategyExcel> vlsTimeStrategyList = baseMapper.exportVlsTimeStrategy(queryWrapper);
		return vlsTimeStrategyList;
	}

	@Override
	public TimeStrategy getByDeviceId(String deviceId) {
		QueryWrapper<TimeStrategy> wrapper = new QueryWrapper<>();
		wrapper.eq("device_id", deviceId);
		return this.getOne(wrapper);
	}

	@Override
	public boolean saveOrUpdateStrategy(TimeStrategy timeStrategy) {
		// 检查是否已存在该设备的时间策略
		TimeStrategy existing = getByDeviceId(timeStrategy.getDeviceId());
		boolean success;

		if (existing != null) {
			// 如果存在，更新
			UpdateWrapper<TimeStrategy> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("device_id", timeStrategy.getDeviceId());
			timeStrategy.setId(existing.getId());
			success = this.update(timeStrategy, updateWrapper);
		} else {
			// 如果不存在，新增
			success = this.save(timeStrategy);
		}
		if (success) {
			notifyRecordingRefresh();
		}
		return success;
	}

	@Override
	public boolean deleteByDeviceId(String deviceId) {
		QueryWrapper<TimeStrategy> wrapper = new QueryWrapper<>();
		wrapper.eq("device_id", deviceId);
		boolean success = this.remove(wrapper);
		if (success) {
			notifyRecordingRefresh();
		}
		return success;
	}


	private void notifyRecordingRefresh() {
		VlsRtspRecordingManager rtspRecordingManager = rtspRecordingManagerProvider.getIfAvailable();
		if (rtspRecordingManager != null) {
			rtspRecordingManager.refreshNowAsync();
		}
	}

}

