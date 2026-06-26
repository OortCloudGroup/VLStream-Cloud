package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Device Tag Association Table Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsDeviceTagRelationExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Device ID, relates to device_info.id
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device ID, relates to device_info.id")
	private Long deviceId;
	/**
	 * Label ID, associated with tag_management.id
	 */
	@ColumnWidth(20)
	@ExcelProperty("Label ID, associated with tag_management.id")
	private Long tagId;

}
