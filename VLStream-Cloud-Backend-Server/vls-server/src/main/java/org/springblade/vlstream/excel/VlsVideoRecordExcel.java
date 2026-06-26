package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * Video recording record table Excel entity class
 *
 * @author Oort
 * @since 2025-12-25
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsVideoRecordExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * vhost
	 */
	@ColumnWidth(20)
	@ExcelProperty("vhost")
	private String vhost;
	/**
	 * app
	 */
	@ColumnWidth(20)
	@ExcelProperty("app")
	private String app;
	/**
	 * stream
	 */
	@ColumnWidth(20)
	@ExcelProperty("stream")
	private String stream;
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
	 * Video File Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Video File Name")
	private String fileName;
	/**
	 * Video File Path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Video File Path")
	private String filePath;
	/**
	 * File size (bytes)
	 */
	@ColumnWidth(20)
	@ExcelProperty("File size (bytes)")
	private Long fileSize;
	/**
	 * Playback URL
	 */
	@ColumnWidth(20)
	@ExcelProperty("Playback URL")
	private String url;
	/**
	 * Video Duration (seconds)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Video Duration (seconds)")
	private Integer duration;
	/**
	 * Video Format
	 */
	@ColumnWidth(20)
	@ExcelProperty("Video Format")
	private String format;
	/**
	 * Recording start time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recording start time")
	private LocalDateTime recordStartTime;
	/**
	 * Recording end time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recording end time")
	private LocalDateTime recordEndTime;
	/**
	 * Recording date (used for grouping by date)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recording date (used for grouping by date)")
	private LocalDate recordDate;
	/**
	 * Recording status
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recording status")
	private String recordStatus;
	/**
	 * Thumbnail path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Thumbnail path")
	private String thumbnailPath;

}
