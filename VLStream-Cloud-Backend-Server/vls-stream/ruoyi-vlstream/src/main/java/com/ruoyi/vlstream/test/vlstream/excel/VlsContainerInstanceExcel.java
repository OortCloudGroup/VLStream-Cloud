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
 * 容器实例表 Excel实体类
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
	 * 实例名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("实例名称")
	private String instanceName;
	/**
	 * 容器ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("容器ID")
	private String containerId;
	/**
	 * 镜像名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("镜像名称")
	private String imageName;
	/**
	 * 镜像标签
	 */
	@ColumnWidth(20)
	@ExcelProperty("镜像标签")
	private String imageTag;
	/**
	 * 算法ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID")
	private Long algorithmId;
	/**
	 * 实例类型
	 */
	@ColumnWidth(20)
	@ExcelProperty("实例类型")
	private String instanceType;
	/**
	 * CPU限制
	 */
	@ColumnWidth(20)
	@ExcelProperty("CPU限制")
	private String cpuLimit;
	/**
	 * 内存限制
	 */
	@ColumnWidth(20)
	@ExcelProperty("内存限制")
	private String memoryLimit;
	/**
	 * GPU限制
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU限制")
	private String gpuLimit;
	/**
	 * 端口配置
	 */
	@ColumnWidth(20)
	@ExcelProperty("端口配置")
	private String portConfig;
	/**
	 * 环境变量配置
	 */
	@ColumnWidth(20)
	@ExcelProperty("环境变量配置")
	private String envConfig;
	/**
	 * 存储卷配置
	 */
	@ColumnWidth(20)
	@ExcelProperty("存储卷配置")
	private String volumeConfig;
	/**
	 * 实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中
	 */
	@ColumnWidth(20)
	@ExcelProperty("实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中")
	private String instanceStatus;
	/**
	 * 健康状态：healthy-健康,unhealthy-不健康,unknown-未知
	 */
	@ColumnWidth(20)
	@ExcelProperty("健康状态：healthy-健康,unhealthy-不健康,unknown-未知")
	private String healthStatus;
	/**
	 * 启动时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("启动时间")
	private LocalDateTime startTime;
	/**
	 * 停止时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("停止时间")
	private LocalDateTime stopTime;
	/**
	 * 重启次数
	 */
	@ColumnWidth(20)
	@ExcelProperty("重启次数")
	private Integer restartCount;
	/**
	 * CPU使用率
	 */
	@ColumnWidth(20)
	@ExcelProperty("CPU使用率")
	private BigDecimal cpuUsage;
	/**
	 * 内存使用率
	 */
	@ColumnWidth(20)
	@ExcelProperty("内存使用率")
	private BigDecimal memoryUsage;
	/**
	 * GPU使用率
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU使用率")
	private BigDecimal gpuUsage;
	/**
	 * 日志路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("日志路径")
	private String logsPath;

}
