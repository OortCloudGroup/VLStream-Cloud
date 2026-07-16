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


/**
 * 设备标签关联表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsDeviceTagRelationExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 设备ID，关联device_info.id
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备ID，关联device_info.id")
	private Long deviceId;
	/**
	 * 标签ID，关联tag_management.id
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签ID，关联tag_management.id")
	private Long tagId;

}
