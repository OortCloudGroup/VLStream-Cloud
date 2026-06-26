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
 * Intelligent analysis request table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnalysisRequestExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Analysis name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Analysis name")
	private String analysisName;
	/**
	 * Analysis type
	 */
	@ColumnWidth(20)
	@ExcelProperty("Analysis type")
	private String analysisType;
	/**
	 * Scene name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Scene name")
	private String sceneName;
	/**
	 * Device ID list, comma separated
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device ID list, comma separated")
	private String deviceIds;
	/**
	 * Device Information
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Information")
	private String deviceInfo;
	/**
	 * Algorithm ID list, comma separated
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm ID list, comma separated")
	private String algorithmIds;
	/**
	 * Area information
	 */
	@ColumnWidth(20)
	@ExcelProperty("Area information")
	private String regionInfo;
	/**
	 * Time range
	 */
	@ColumnWidth(20)
	@ExcelProperty("Time range")
	private String timeRange;
	/**
	 * Screenshot information
	 */
	@ColumnWidth(20)
	@ExcelProperty("Screenshot information")
	private String screenshots;
	/**
	 * Request status: pending-pending, processing-processing, completed-completed, failed-failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("Request status: pending-pending, processing-processing, completed-completed, failed-failed")
	private String requestStatus;
	/**
	 * Progress percentage
	 */
	@ColumnWidth(20)
	@ExcelProperty("Progress percentage")
	private Integer progress;
	/**
	 * Result file path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Result file path")
	private String resultPath;
	/**
	 * Processing start time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Processing start time")
	private LocalDateTime startTime;
	/**
	 * Completion time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Completion time")
	private LocalDateTime completeTime;
	/**
	 * Error message
	 */
	@ColumnWidth(20)
	@ExcelProperty("Error message")
	private String errorMessage;
	/**
	 * Description information
	 */
	@ColumnWidth(20)
	@ExcelProperty("Description information")
	private String description;

}
