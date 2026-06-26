package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * Annotation Instance Entity Class Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnnotationInstanceExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Associated annotation project ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Associated annotation project ID")
	private Long annotationId;
	/**
	 * Label ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label ID")
	private Long labelId;
	/**
	 * Image id
	 */
	@ColumnWidth(20)
	@ExcelProperty("Image id")
	private Long imageId;
	/**
	 * Annotation type: rect-rectangle, circle-circle, polygon-polygon
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation type: rect-rectangle, circle-circle, polygon-polygon")
	private String annotationType;
	/**
	 * Annotation Coordinate Data (JSON format)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation Coordinate Data (JSON format)")
	private String annotationData;
	/**
	 * Confidence
	 */
	@ColumnWidth(20)
	@ExcelProperty("Confidence")
	private BigDecimal confidence;
	/**
	 * Whether verified
	 */
	@ColumnWidth(20)
	@ExcelProperty("Whether verified")
	private Byte verified;

}
