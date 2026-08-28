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
public class VlsTimeStrategyExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * deviceID
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备ID")
	private String deviceId;
	/**
	 * : everyday- , weekly-
	 */
	@ColumnWidth(20)
	@ExcelProperty("策略类型：everyday-每天, weekly-每周")
	private String strategyType;
	/**
	 * , JSONarray : [0,1,2,3]
	 */
	@ColumnWidth(20)
	@ExcelProperty("每天模式的时间段，JSON数组格式：[0,1,2,3]")
	private String dailyTimes;
	/**
	 * , JSONobject : {\"monday\":[0,1,2],\"tuesday\":[3,4,5]}
	 */
	@ColumnWidth(20)
	@ExcelProperty("每周模式的时间段，JSON对象格式：{\"monday\":[0,1,2],\"tuesday\":[3,4,5]}")
	private String weeklyTimes;

}
