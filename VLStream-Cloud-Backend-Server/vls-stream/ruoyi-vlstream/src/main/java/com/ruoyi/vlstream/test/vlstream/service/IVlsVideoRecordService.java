/*
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
 * 视频录制记录表 服务类
 *
 * @author Oort
 * @since 2025-12-25
 */
public interface IVlsVideoRecordService extends BaseService<VideoRecord> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsVideoRecord 查询参数
	 * @return IPage<VlsVideoRecordVO>
	 */
	IPage<VideoRecordVO> selectVlsVideoRecordPage(IPage<VideoRecordVO> page, VideoRecordVO vlsVideoRecord);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsVideoRecordExcel>
	 */
	List<VlsVideoRecordExcel> exportVlsVideoRecord(Wrapper<VideoRecord> queryWrapper);

	/**
	 * 按时间范围查询回放记录
	 *
	 * @param deviceId 设备ID
	 * @param startTime 起始时间
	 * @param endTime 结束时间
	 * @return 回放记录列表
	 */
	List<VideoRecord> listPlaybackRecords(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

	/**
	 * 查询设备某一天的录制记录
	 *
	 * @param deviceId 设备ID
	 * @param recordDate 日期
	 * @return 录制记录列表
	 */
	List<VideoRecord> listDayRecords(Long deviceId, LocalDate recordDate);

	/**
	 * 查询设备在指定年份有录制记录的日期列表
	 *
	 * @param deviceId 设备ID
	 * @param year 年份（为空时不限制）
	 * @return 日期列表
	 */
	List<LocalDate> listRecordDates(Long deviceId, Integer year);

}
