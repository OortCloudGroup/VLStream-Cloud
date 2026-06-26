package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;


/**
 * Recording plan table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsRecordingScheduleExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Device ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device ID")
	private Long deviceId;
	/**
	 * Device Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Name")
	private String deviceName;
	/**
	 * Plan Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Plan Name")
	private String scheduleName;
	/**
	 * Whether enabled: 0-Disabled, 1-Enabled
	 */
	@ColumnWidth(20)
	@ExcelProperty("Whether enabled: 0-Disabled, 1-Enabled")
	private Byte isEnabled;
	/**
	 * Single recording duration (seconds, default 10 minutes)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Single recording duration (seconds, default 10 minutes)")
	private Integer recordDuration;
	/**
	 * Recording quality
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recording quality")
	private String recordQuality;
	/**
	 * Recording format
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recording format")
	private String recordFormat;
	/**
	 * Storage path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Storage path")
	private String storagePath;
	/**
	 * Retention days (0 means permanent retention)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Retention days (0 means permanent retention)")
	private Integer retentionDays;
	/**
	 * Plan Type
	 */
	@ColumnWidth(20)
	@ExcelProperty("Plan Type")
	private String scheduleType;
	/**
	 * Time strategy ID (when schedule_type is time_strategy)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Time strategy ID (when schedule_type is time_strategy)")
	private Long timeStrategyId;
	/**
	 * Start time (when schedule_type is time_range)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Start time (when schedule_type is time_range)")
	private LocalTime startTime;
	/**
	 * End time (when schedule_type is time_range)
	 */
	@ColumnWidth(20)
	@ExcelProperty("End time (when schedule_type is time_range)")
	private LocalTime endTime;
	/**
	 * Days of week to record (1-7, comma-separated)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Days of week to record (1-7, comma-separated)")
	private String weekdays;
	/**
	 * Last Recording Time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Last Recording Time")
	private LocalDateTime lastRecordTime;
	/**
	 * Next recording time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Next recording time")
	private LocalDateTime nextRecordTime;
	/**
	 * Total recording count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Total recording count")
	private Integer totalRecords;
	/**
	 * Failed recording count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Failed recording count")
	private Integer failedRecords;

}
