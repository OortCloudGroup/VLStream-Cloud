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
import java.time.LocalDateTime;


/**
 * 事件管理表 Excel实体类
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
	 * 事件描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件描述")
	private String eventDesc;
	/**
	 * 事件类型
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件类型")
	private String eventType;
	/**
	 * 上报位置
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报位置")
	private String reportLocation;
	/**
	 * 上报设备
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报设备")
	private String reportDevice;
	/**
	 * 上报图片
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报图片")
	private String reportImg;
	/**
	 * 上报时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("上报时间")
	private LocalDateTime reportTime;
	/**
	 * 事件级别：low-低,medium-中,high-高,urgent-紧急
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件级别：low-低,medium-中,high-高,urgent-紧急")
	private String eventLevel;
	/**
	 * 事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件状态：pending-待处理,processing-处理中,completed-已完成,closed-已关闭")
	private String eventStatus;
	/**
	 * 执行人
	 */
	@ColumnWidth(20)
	@ExcelProperty("执行人")
	private String executor;
	/**
	 * 执行人ID列表
	 */
	@ColumnWidth(20)
	@ExcelProperty("执行人ID列表")
	private String executorIds;
	/**
	 * 事件数据
	 */
	@ColumnWidth(20)
	@ExcelProperty("事件数据")
	private String eventData;
	/**
	 * 处理结果
	 */
	@ColumnWidth(20)
	@ExcelProperty("处理结果")
	private String handleResult;
	/**
	 * 反馈信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("反馈信息")
	private String feedbackInfo;
	/**
	 * 反馈图片
	 */
	@ColumnWidth(20)
	@ExcelProperty("反馈图片")
	private String feedbackImg;
	/**
	 * 反馈状态
	 */
	@ColumnWidth(20)
	@ExcelProperty("反馈状态")
	private Integer feedbackStatus;

}
