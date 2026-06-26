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
 * Algorithm orchestration table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmOrchestrationExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Orchestration name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Orchestration name")
	private String orchestrationName;
	/**
	 * Orchestration description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Orchestration description")
	private String orchestrationDesc;
	/**
	 * Trigger type: realtime-real-time, scheduled-scheduled, manual-manual
	 */
	@ColumnWidth(20)
	@ExcelProperty("Trigger type: realtime-real-time, scheduled-scheduled, manual-manual")
	private String triggerType;
	/**
	 * Execution mode: serial-Serial, parallel-Parallel
	 */
	@ColumnWidth(20)
	@ExcelProperty("Execution mode: serial-Serial, parallel-Parallel")
	private String executeMode;
	/**
	 * Algorithm step configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm step configuration")
	private String algorithmSteps;
	/**
	 * Input Configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("Input Configuration")
	private String inputConfig;
	/**
	 * Output Configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("Output Configuration")
	private String outputConfig;
	/**
	 * Number of associated devices
	 */
	@ColumnWidth(20)
	@ExcelProperty("Number of associated devices")
	private Integer deviceCount;
	/**
	 * Run Count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Run Count")
	private Integer runCount;
	/**
	 * Status: active-active, inactive-inactive, draft-draft
	 */
	@ColumnWidth(20)
	@ExcelProperty("Status: active-active, inactive-inactive, draft-draft")
	private String orchestrationStatus;
	/**
	 * Last Run Time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Last Run Time")
	private LocalDateTime lastRunTime;
	/**
	 * Average running time (seconds)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Average running time (seconds)")
	private Integer avgRunTime;
	/**
	 * Success rate
	 */
	@ColumnWidth(20)
	@ExcelProperty("Success rate")
	private BigDecimal successRate;

}
