/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;


/**
 * Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsRecordingScheduleExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * deviceID
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备ID")
	private Long deviceId;
	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备名称")
	private String deviceName;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("计划名称")
	private String scheduleName;
	/**
	 * whether : 0- , 1-
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否启用：0-禁用，1-启用")
	private Byte isEnabled;
	/**
	 * ( , 10 )
	 */
	@ColumnWidth(20)
	@ExcelProperty("单次录制时长(秒，默认10分钟)")
	private Integer recordDuration;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("录制质量")
	private String recordQuality;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("录制格式")
	private String recordFormat;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("存储路径")
	private String storagePath;
	/**
	 * (0 )
	 */
	@ColumnWidth(20)
	@ExcelProperty("保留天数(0表示永久保留)")
	private Integer retentionDays;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("计划类型")
	private String scheduleType;
	/**
	 * ID( schedule_type to time_strategy )
	 */
	@ColumnWidth(20)
	@ExcelProperty("时间策略ID(当schedule_type为time_strategy时)")
	private Long timeStrategyId;
	/**
	 * start ( schedule_type to time_range )
	 */
	@ColumnWidth(20)
	@ExcelProperty("开始时间(当schedule_type为time_range时)")
	private LocalTime startTime;
	/**
	 * finish ( schedule_type to time_range )
	 */
	@ColumnWidth(20)
	@ExcelProperty("结束时间(当schedule_type为time_range时)")
	private LocalTime endTime;
	/**
	 * (1-7, )
	 */
	@ColumnWidth(20)
	@ExcelProperty("星期几录制(1-7,逗号分隔)")
	private String weekdays;
	/**
	 * after
	 */
	@ColumnWidth(20)
	@ExcelProperty("最后录制时间")
	private LocalDateTime lastRecordTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("下次录制时间")
	private LocalDateTime nextRecordTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("总录制次数")
	private Integer totalRecords;
	/**
	 * failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("失败录制次数")
	private Integer failedRecords;

}
