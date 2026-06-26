package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Algorithm repository table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmRepositoryExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Algorithm repository name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm repository name")
	private String name;
	/**
	 * Number of algorithms owned
	 */
	@ColumnWidth(20)
	@ExcelProperty("Number of algorithms owned")
	private Integer algorithmCount;
	/**
	 * Repository type: basic-Basic Preset, extended-Extended
	 */
	@ColumnWidth(20)
	@ExcelProperty("Repository type: basic-Basic Preset, extended-Extended")
	private String repositoryType;
	/**
	 * Remarks
	 */
	@ColumnWidth(20)
	@ExcelProperty("Remarks")
	private String remark;

}
