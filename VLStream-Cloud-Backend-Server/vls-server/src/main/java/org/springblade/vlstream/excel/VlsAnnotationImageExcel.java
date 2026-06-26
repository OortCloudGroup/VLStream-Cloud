package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * Annotation Image Info Table Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnnotationImageExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Annotation Project ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation Project ID")
	private Long annotationId;
	/**
	 * Image name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Image name")
	private String imageName;
	/**
	 * Original filename
	 */
	@ColumnWidth(20)
	@ExcelProperty("Original filename")
	private String originalName;
	/**
	 * Local Storage Path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Local Storage Path")
	private String localPath;
	/**
	 * File size (bytes)
	 */
	@ColumnWidth(20)
	@ExcelProperty("File size (bytes)")
	private Long fileSize;
	/**
	 * Last Modified Time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Last Modified Time")
	private LocalDateTime lastModified;
	/**
	 * Whether it is an imported image: 0-No, 1-Yes
	 */
	@ColumnWidth(20)
	@ExcelProperty("Whether it is an imported image: 0-No, 1-Yes")
	private Byte isImported;
	/**
	 * Import time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Import time")
	private LocalDateTime importTime;

}
