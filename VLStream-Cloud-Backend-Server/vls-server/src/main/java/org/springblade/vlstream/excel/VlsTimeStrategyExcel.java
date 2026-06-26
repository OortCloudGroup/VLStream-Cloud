package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Time strategy table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsTimeStrategyExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Device ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device ID")
	private String deviceId;
	/**
	 * Policy type: everyday-daily, weekly-weekly
	 */
	@ColumnWidth(20)
	@ExcelProperty("Policy type: everyday-daily, weekly-weekly")
	private String strategyType;
	/**
	 * Time periods of daily mode, JSON array format: [0,1,2,3]
	 */
	@ColumnWidth(20)
	@ExcelProperty("Time periods of daily mode, JSON array format: [0,1,2,3]")
	private String dailyTimes;
	/**
	 * Time periods of weekly mode, JSON object format: {\"monday\":[0,1,2],\"tuesday\":[3,4,5]}
	 */
	@ColumnWidth(20)
	@ExcelProperty("Time periods of weekly mode, JSON object format: {\"monday\":[0,1,2],\"tuesday\":[3,4,5]}")
	private String weeklyTimes;

}
