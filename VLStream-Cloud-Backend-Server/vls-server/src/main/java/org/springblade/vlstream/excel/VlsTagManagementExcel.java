package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Label Management Table Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsTagManagementExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Label Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Name")
	private String tagName;
	/**
	 * Label Category: own-own labels, public-public labels
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Category: own-own labels, public-public labels")
	private String categoryType;
	/**
	 * Level: 1-Tag type, 2-Specific tag
	 */
	@ColumnWidth(20)
	@ExcelProperty("Level: 1-Tag type, 2-Specific tag")
	private Byte level;
	/**
	 * Parent ID, NULL when level=1, tag type ID when level=2
	 */
	@ColumnWidth(20)
	@ExcelProperty("Parent ID, NULL when level=1, tag type ID when level=2")
	private Long parentId;
	/**
	 * Sort order
	 */
	@ColumnWidth(20)
	@ExcelProperty("Sort order")
	private Integer sortOrder;
	/**
	 * Label Color
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Color")
	private String tagColor;
	/**
	 * Label Icon
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Icon")
	private String tagIcon;
	/**
	 * Label Description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Description")
	private String description;
	/**
	 * Whether enabled: 1-Enabled, 0-Disabled
	 */
	@ColumnWidth(20)
	@ExcelProperty("Whether enabled: 1-Enabled, 0-Disabled")
	private Byte isActive;
	/**
	 * Number of usages
	 */
	@ColumnWidth(20)
	@ExcelProperty("Number of usages")
	private Integer usageCount;

}
