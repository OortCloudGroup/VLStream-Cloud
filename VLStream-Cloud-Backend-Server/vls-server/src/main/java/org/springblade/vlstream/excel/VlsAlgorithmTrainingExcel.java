package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * Algorithm training task table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmTrainingExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Task name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Task name")
	private String taskName;
	/**
	 * Algorithm ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm ID")
	private Long algorithmId;
	/**
	 * Dataset ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Dataset ID")
	private Long datasetId;
	/**
	 * Training status: pending-pending, training-training, completed-completed, failed-failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("Training status: pending-pending, training-training, completed-completed, failed-failed")
	private String trainStatus;
	/**
	 * Training Progress Percentage
	 */
	@ColumnWidth(20)
	@ExcelProperty("Training Progress Percentage")
	private Integer progress;
	/**
	 * Current round
	 */
	@ColumnWidth(20)
	@ExcelProperty("Current round")
	private Integer epochCurrent;
	/**
	 * Total rounds
	 */
	@ColumnWidth(20)
	@ExcelProperty("Total rounds")
	private Integer epochTotal;
	/**
	 * Accuracy
	 */
	@ColumnWidth(20)
	@ExcelProperty("Accuracy")
	private BigDecimal accuracy;
	/**
	 * Precision
	 */
	@ColumnWidth(20)
	@ExcelProperty("Precision")
	private BigDecimal precisionValue;
	/**
	 * Recall rate
	 */
	@ColumnWidth(20)
	@ExcelProperty("Recall rate")
	private BigDecimal recallValue;
	/**
	 * mAP value
	 */
	@ColumnWidth(20)
	@ExcelProperty("mAP value")
	private BigDecimal mapValue;
	/**
	 * Loss value
	 */
	@ColumnWidth(20)
	@ExcelProperty("Loss value")
	private BigDecimal lossValue;
	/**
	 * GPU usage
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU usage")
	private String gpuUsage;
	/**
	 * Start time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Start time")
	private LocalDateTime startTime;
	/**
	 * End time
	 */
	@ColumnWidth(20)
	@ExcelProperty("End time")
	private LocalDateTime endTime;
	/**
	 * Estimated time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Estimated time")
	private String estimatedTime;
	/**
	 * Model output path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model output path")
	private String modelOutputPath;
	/**
	 * Log path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Log path")
	private String logPath;
	/**
	 * Training Parameters
	 */
	@ColumnWidth(20)
	@ExcelProperty("Training Parameters")
	private String configParams;
	/**
	 * Error message
	 */
	@ColumnWidth(20)
	@ExcelProperty("Error message")
	private String errorMessage;
	/**
	 * Model file path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model file path")
	private String modelPath;
	/**
	 * Completion time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Completion time")
	private String completedAt;

}
