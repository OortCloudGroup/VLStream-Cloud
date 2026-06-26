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
 * Event management table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsEventManagementExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Event description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Event description")
	private String eventDesc;
	/**
	 * Event type
	 */
	@ColumnWidth(20)
	@ExcelProperty("Event type")
	private String eventType;
	/**
	 * Report location
	 */
	@ColumnWidth(20)
	@ExcelProperty("Report location")
	private String reportLocation;
	/**
	 * Report device
	 */
	@ColumnWidth(20)
	@ExcelProperty("Report device")
	private String reportDevice;
	/**
	 * Report image
	 */
	@ColumnWidth(20)
	@ExcelProperty("Report image")
	private String reportImg;
	/**
	 * Report time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Report time")
	private LocalDateTime reportTime;
	/**
	 * Event level: low-Low, medium-Medium, high-High, urgent-Urgent
	 */
	@ColumnWidth(20)
	@ExcelProperty("Event level: low-Low, medium-Medium, high-High, urgent-Urgent")
	private String eventLevel;
	/**
	 * Event status: pending-Pending, processing-Processing, completed-Completed, closed-Closed
	 */
	@ColumnWidth(20)
	@ExcelProperty("Event status: pending-Pending, processing-Processing, completed-Completed, closed-Closed")
	private String eventStatus;
	/**
	 * Executor
	 */
	@ColumnWidth(20)
	@ExcelProperty("Executor")
	private String executor;
	/**
	 * Executor ID list
	 */
	@ColumnWidth(20)
	@ExcelProperty("Executor ID list")
	private String executorIds;
	/**
	 * Event data
	 */
	@ColumnWidth(20)
	@ExcelProperty("Event data")
	private String eventData;
	/**
	 * Processing result
	 */
	@ColumnWidth(20)
	@ExcelProperty("Processing result")
	private String handleResult;
	/**
	 * Feedback info
	 */
	@ColumnWidth(20)
	@ExcelProperty("Feedback info")
	private String feedbackInfo;
	/**
	 * Feedback image
	 */
	@ColumnWidth(20)
	@ExcelProperty("Feedback image")
	private String feedbackImg;
	/**
	 * Feedback status
	 */
	@ColumnWidth(20)
	@ExcelProperty("Feedback status")
	private Integer feedbackStatus;

}
