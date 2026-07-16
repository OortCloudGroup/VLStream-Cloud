/*
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
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * 视频录制记录表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-25
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsVideoRecordExcel implements Serializable {
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
	 * 设备ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备ID")
	private Long deviceId;
	/**
	 * 设备名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备名称")
	private String deviceName;
	/**
	 * 视频文件名
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频文件名")
	private String fileName;
	/**
	 * 视频文件路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频文件路径")
	private String filePath;
	/**
	 * 文件大小(字节)
	 */
	@ColumnWidth(20)
	@ExcelProperty("文件大小(字节)")
	private Long fileSize;
	/**
	 * 点播url
	 */
	@ColumnWidth(20)
	@ExcelProperty("点播url")
	private String url;
	/**
	 * 视频时长(秒)
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频时长(秒)")
	private Integer duration;
	/**
	 * 视频格式
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频格式")
	private String format;
	/**
	 * 录制开始时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("录制开始时间")
	private LocalDateTime recordStartTime;
	/**
	 * 录制结束时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("录制结束时间")
	private LocalDateTime recordEndTime;
	/**
	 * 录制日期(用于按日期分组)
	 */
	@ColumnWidth(20)
	@ExcelProperty("录制日期(用于按日期分组)")
	private LocalDate recordDate;
	/**
	 * 录制状态
	 */
	@ColumnWidth(20)
	@ExcelProperty("录制状态")
	private String recordStatus;
	/**
	 * 缩略图路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("缩略图路径")
	private String thumbnailPath;

}
