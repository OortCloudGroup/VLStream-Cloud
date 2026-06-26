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
 * Container instance table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsContainerInstanceExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Instance name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Instance name")
	private String instanceName;
	/**
	 * Container ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Container ID")
	private String containerId;
	/**
	 * Image name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Image name")
	private String imageName;
	/**
	 * Image tag
	 */
	@ColumnWidth(20)
	@ExcelProperty("Image tag")
	private String imageTag;
	/**
	 * Algorithm ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm ID")
	private Long algorithmId;
	/**
	 * Instance type
	 */
	@ColumnWidth(20)
	@ExcelProperty("Instance type")
	private String instanceType;
	/**
	 * CPU limit
	 */
	@ColumnWidth(20)
	@ExcelProperty("CPU limit")
	private String cpuLimit;
	/**
	 * Memory limit
	 */
	@ColumnWidth(20)
	@ExcelProperty("Memory limit")
	private String memoryLimit;
	/**
	 * GPU limit
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU limit")
	private String gpuLimit;
	/**
	 * Port configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("Port configuration")
	private String portConfig;
	/**
	 * Environment variable configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("Environment variable configuration")
	private String envConfig;
	/**
	 * Storage volume configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("Storage volume configuration")
	private String volumeConfig;
	/**
	 * Instance status: running-Running, stopped-Stopped, error-Error, starting-Starting, stopping-Stopping
	 */
	@ColumnWidth(20)
	@ExcelProperty("Instance status: running-Running, stopped-Stopped, error-Error, starting-Starting, stopping-Stopping")
	private String instanceStatus;
	/**
	 * Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown
	 */
	@ColumnWidth(20)
	@ExcelProperty("Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown")
	private String healthStatus;
	/**
	 * Start time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Start time")
	private LocalDateTime startTime;
	/**
	 * Stop time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Stop time")
	private LocalDateTime stopTime;
	/**
	 * Restart count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Restart count")
	private Integer restartCount;
	/**
	 * CPU usage
	 */
	@ColumnWidth(20)
	@ExcelProperty("CPU usage")
	private BigDecimal cpuUsage;
	/**
	 * Memory usage rate
	 */
	@ColumnWidth(20)
	@ExcelProperty("Memory usage rate")
	private BigDecimal memoryUsage;
	/**
	 * GPU usage
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU usage")
	private BigDecimal gpuUsage;
	/**
	 * Log path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Log path")
	private String logsPath;

}
