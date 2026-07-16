/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsVideoRecordExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsVideoRecordMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.VideoRecordVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsVideoRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频录制记录表 服务实现类
 *
 * @author Oort
 * @since 2025-12-25
 */
@Service
public class VlsVideoRecordServiceImpl extends BaseServiceImpl<VlsVideoRecordMapper, VideoRecord> implements IVlsVideoRecordService {

	@Override
	public IPage<VideoRecordVO> selectVlsVideoRecordPage(IPage<VideoRecordVO> page, VideoRecordVO vlsVideoRecord) {
		return page.setRecords(baseMapper.selectVlsVideoRecordPage(page, vlsVideoRecord));
	}

	@Override
	public List<VlsVideoRecordExcel> exportVlsVideoRecord(Wrapper<VideoRecord> queryWrapper) {
		List<VlsVideoRecordExcel> vlsVideoRecordList = baseMapper.exportVlsVideoRecord(queryWrapper);
		//vlsVideoRecordList.forEach(vlsVideoRecord -> {
		//	vlsVideoRecord.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsVideoRecordEntity.getType()));
		//});
		return vlsVideoRecordList;
	}

	@Override
	public List<VideoRecord> listPlaybackRecords(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
		if (deviceId == null) {
			return java.util.Arrays.asList();
		}
		LambdaQueryWrapper<VideoRecord> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(VideoRecord::getDeviceId, deviceId);
		if (startTime != null) {
			wrapper.ge(VideoRecord::getRecordEndTime, startTime);
		}
		if (endTime != null) {
			wrapper.le(VideoRecord::getRecordStartTime, endTime);
		}
		wrapper.orderByAsc(VideoRecord::getRecordStartTime);
		return list(wrapper);
	}

	@Override
	public List<VideoRecord> listDayRecords(Long deviceId, LocalDate recordDate) {
		if (deviceId == null || recordDate == null) {
			return java.util.Arrays.asList();
		}
		LambdaQueryWrapper<VideoRecord> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(VideoRecord::getDeviceId, deviceId)
			.eq(VideoRecord::getRecordDate, recordDate)
			.orderByAsc(VideoRecord::getRecordStartTime);
		return list(wrapper);
	}

	@Override
	public List<LocalDate> listRecordDates(Long deviceId, Integer year) {
		if (deviceId == null) {
			return java.util.Arrays.asList();
		}
		LambdaQueryWrapper<VideoRecord> wrapper = new LambdaQueryWrapper<>();
		wrapper.select(VideoRecord::getRecordDate)
			.eq(VideoRecord::getDeviceId, deviceId)
			.isNotNull(VideoRecord::getRecordDate)
			.orderByAsc(VideoRecord::getRecordDate);
		if (year != null && year > 0) {
			LocalDate yearStartDate = LocalDate.of(year, 1, 1);
			LocalDate yearEndDate = LocalDate.of(year, 12, 31);
			wrapper.between(VideoRecord::getRecordDate, yearStartDate, yearEndDate);
		}
		return list(wrapper).stream()
			.map(VideoRecord::getRecordDate)
			.filter(date -> date != null)
			.distinct()
			.filter(date -> year == null || date.getYear() == year)
			.collect(Collectors.toList());
	}

}
