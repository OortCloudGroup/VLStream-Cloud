package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Annotation Label Entity Class Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnnotationLabelExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Associated annotation project ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Associated annotation project ID")
	private Long annotationId;
	/**
	 * Label Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Name")
	private String name;
	/**
	 * Label Color (Hexadecimal)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Color (Hexadecimal)")
	private String color;
	/**
	 * Label Description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label Description")
	private String description;
	/**
	 * Sort order
	 */
	@ColumnWidth(20)
	@ExcelProperty("Sort order")
	private Integer sortOrder;
	/**
	 * Usage frequency statistics
	 */
	@ColumnWidth(20)
	@ExcelProperty("Usage frequency statistics")
	private Integer usageCount;

}
