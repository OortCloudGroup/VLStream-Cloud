/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * event Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsEventManagementExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * event
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件描述")
	private String eventDesc;
	/**
	 * event
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件类型")
	private String eventType;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报位置")
	private String reportLocation;
	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报设备")
	private String reportDevice;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报图片")
	private String reportImg;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报时间")
	private LocalDateTime reportTime;
	/**
	 * event : low- ,medium- in ,high- ,urgent-
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件级别：low-低,medium-中,high-高,urgent-紧急")
	private String eventLevel;
	/**
	 * event : pending- Process ,processing-Process in ,completed- already ,closed- already
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭")
	private String eventStatus;
	/**
	 * Execute
	 */
	@ColumnWidth(20)
	@ExcelProperty("执行人")
	private String executor;
	/**
	 * Execute ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("执行人ID列表")
	private String executorIds;
	/**
	 * eventdata
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件数据")
	private String eventData;
	/**
	 * Process
	 */
	@ColumnWidth(20)
	@ExcelProperty("处理结果")
	private String handleResult;
	/**
	 * info
	 */
	@ColumnWidth(20)
	@ExcelProperty("反馈信息")
	private String feedbackInfo;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("反馈图片")
	private String feedbackImg;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("反馈状态")
	private Integer feedbackStatus;

}
