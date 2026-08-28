/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * instance Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsContainerInstanceExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * instance
	 */
	@ColumnWidth(20)
	@ExcelProperty("实例名称")
	private String instanceName;
	/**
	 * ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("容器ID")
	private String containerId;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("镜像名称")
	private String imageName;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("镜像标签")
	private String imageTag;
	/**
	 * algorithmID
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID")
	private Long algorithmId;
	/**
	 * instance
	 */
	@ColumnWidth(20)
	@ExcelProperty("实例类型")
	private String instanceType;
	/**
	 * CPU
	 */
	@ColumnWidth(20)
	@ExcelProperty("CPU限制")
	private String cpuLimit;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("内存限制")
	private String memoryLimit;
	/**
	 * GPU
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU限制")
	private String gpuLimit;
	/**
	 * configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("端口配置")
	private String portConfig;
	/**
	 * variableconfiguration
	 */
	@ColumnWidth(20)
	@ExcelProperty("环境变量配置")
	private String envConfig;
	/**
	 * configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("存储卷配置")
	private String volumeConfig;
	/**
	 * instance : running- in ,stopped- already ,error- ,starting- in ,stopping- in
	 */
	@ColumnWidth(20)
	@ExcelProperty("实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中")
	private String instanceStatus;
	/**
	 * : healthy- ,unhealthy- ,unknown- not
	 */
	@ColumnWidth(20)
	@ExcelProperty("健康状态：healthy-健康,unhealthy-不健康,unknown-未知")
	private String healthStatus;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("启动时间")
	private LocalDateTime startTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("停止时间")
	private LocalDateTime stopTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("重启次数")
	private Integer restartCount;
	/**
	 * CPU
	 */
	@ColumnWidth(20)
	@ExcelProperty("CPU使用率")
	private BigDecimal cpuUsage;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("内存使用率")
	private BigDecimal memoryUsage;
	/**
	 * GPU
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU使用率")
	private BigDecimal gpuUsage;
	/**
	 * log
	 */
	@ColumnWidth(20)
	@ExcelProperty("日志路径")
	private String logsPath;

}
