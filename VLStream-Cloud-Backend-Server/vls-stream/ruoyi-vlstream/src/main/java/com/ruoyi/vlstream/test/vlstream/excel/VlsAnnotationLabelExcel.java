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


/**
 * annotation Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnnotationLabelExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * annotation item ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("关联的标注项目ID")
	private Long annotationId;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签名称")
	private String name;
	/**
	 * ( )
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签颜色(十六进制)")
	private String color;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签描述")
	private String description;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("排序顺序")
	private Integer sortOrder;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("使用次数统计")
	private Integer usageCount;

}
