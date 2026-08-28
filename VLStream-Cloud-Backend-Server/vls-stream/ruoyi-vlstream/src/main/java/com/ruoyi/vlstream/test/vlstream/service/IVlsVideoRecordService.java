/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsVideoRecordExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.VideoRecordVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * record service
 *
 * @author Oort
 * @since 2025-12-25
 */
public interface IVlsVideoRecordService extends BaseService<VideoRecord> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsVideoRecord Query parameter
	 * @return IPage<VlsVideoRecordVO>
	 */
	IPage<VideoRecordVO> selectVlsVideoRecordPage(IPage<VideoRecordVO> page, VideoRecordVO vlsVideoRecord);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsVideoRecordExcel>
	 */
	List<VlsVideoRecordExcel> exportVlsVideoRecord(Wrapper<VideoRecord> queryWrapper);

	/**
	 * Query record
	 *
	 * @param deviceId deviceID
	 * @param startTime
	 * @param endTime finish
	 * @return record
	 */
	List<VideoRecord> listPlaybackRecords(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

	/**
	 * Query device record
	 *
	 * @param deviceId deviceID
	 * @param recordDate
	 * @return record
	 */
	List<VideoRecord> listDayRecords(Long deviceId, LocalDate recordDate);

	/**
	 * Query device in record list
	 *
	 * @param deviceId deviceID
	 * @param year ( is empty )
	 * @return
	 */
	List<LocalDate> listRecordDates(Long deviceId, Integer year);

}
