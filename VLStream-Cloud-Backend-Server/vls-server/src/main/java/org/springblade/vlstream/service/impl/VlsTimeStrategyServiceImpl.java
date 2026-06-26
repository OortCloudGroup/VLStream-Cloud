package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsTimeStrategyExcel;
import org.springblade.vlstream.mapper.VlsTimeStrategyMapper;
import org.springblade.vlstream.pojo.entity.TimeStrategy;
import org.springblade.vlstream.pojo.vo.TimeStrategyVO;
import org.springblade.vlstream.service.IVlsTimeStrategyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Time strategy table service implementation class
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
		// Check if a time strategy already exists for this device
		TimeStrategy existing = getByDeviceId(timeStrategy.getDeviceId());
		boolean success;

		if (existing != null) {
			// If exists, update
			UpdateWrapper<TimeStrategy> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("device_id", timeStrategy.getDeviceId());
			timeStrategy.setId(existing.getId());
			success = this.update(timeStrategy, updateWrapper);
		} else {
			// If not exists, add
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

