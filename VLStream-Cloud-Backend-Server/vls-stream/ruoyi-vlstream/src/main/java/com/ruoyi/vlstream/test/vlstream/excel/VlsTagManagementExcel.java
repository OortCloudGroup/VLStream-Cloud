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
 * Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsTagManagementExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签名称")
	private String tagName;
	/**
	 * : own- , public-
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签大类：own-自有标签，public-公共标签")
	private String categoryType;
	/**
	 * layer : 1- , 2-
	 */
	@ColumnWidth(20)
	@ExcelProperty("层级：1-标签类型，2-具体标签")
	private Byte level;
	/**
	 * ID, level=1 to NULL, level=2 to ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("父级ID，level=1时为NULL，level=2时为标签类型ID")
	private Long parentId;
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
	@ExcelProperty("标签颜色")
	private String tagColor;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签图标")
	private String tagIcon;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签描述")
	private String description;
	/**
	 * whether : 1- , 0-
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否启用：1-启用，0-禁用")
	private Byte isActive;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("使用次数")
	private Integer usageCount;

}
