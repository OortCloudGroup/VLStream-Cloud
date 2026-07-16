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
 * 场景治理表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsSceneGovernanceExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String name;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String description;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String executeType;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String selectedDays;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private Integer intervalNum;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String algorithm;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String location;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String cameras;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String devices;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private String rules;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private LocalDateTime startTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("")
	private LocalDateTime endTime;

}
