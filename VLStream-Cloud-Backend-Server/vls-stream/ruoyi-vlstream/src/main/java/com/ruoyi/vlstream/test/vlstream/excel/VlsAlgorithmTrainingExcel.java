/*
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
 * 算法训练任务表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmTrainingExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 任务名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("任务名称")
	private String taskName;
	/**
	 * 算法ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID")
	private Long algorithmId;
	/**
	 * 数据集ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("数据集ID")
	private Long datasetId;
	/**
	 * 训练状态：pending-等待,training-训练中,completed-完成,failed-失败
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练状态：pending-等待,training-训练中,completed-完成,failed-失败")
	private String trainStatus;
	/**
	 * 训练进度百分比
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练进度百分比")
	private Integer progress;
	/**
	 * 当前轮次
	 */
	@ColumnWidth(20)
	@ExcelProperty("当前轮次")
	private Integer epochCurrent;
	/**
	 * 总轮次
	 */
	@ColumnWidth(20)
	@ExcelProperty("总轮次")
	private Integer epochTotal;
	/**
	 * 准确率
	 */
	@ColumnWidth(20)
	@ExcelProperty("准确率")
	private BigDecimal accuracy;
	/**
	 * 精确率
	 */
	@ColumnWidth(20)
	@ExcelProperty("精确率")
	private BigDecimal precisionValue;
	/**
	 * 召回率
	 */
	@ColumnWidth(20)
	@ExcelProperty("召回率")
	private BigDecimal recallValue;
	/**
	 * mAP值
	 */
	@ColumnWidth(20)
	@ExcelProperty("mAP值")
	private BigDecimal mapValue;
	/**
	 * 损失值
	 */
	@ColumnWidth(20)
	@ExcelProperty("损失值")
	private BigDecimal lossValue;
	/**
	 * GPU使用率
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU使用率")
	private String gpuUsage;
	/**
	 * 开始时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("开始时间")
	private LocalDateTime startTime;
	/**
	 * 结束时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("结束时间")
	private LocalDateTime endTime;
	/**
	 * 预计时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("预计时间")
	private String estimatedTime;
	/**
	 * 模型输出路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型输出路径")
	private String modelOutputPath;
	/**
	 * 日志路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("日志路径")
	private String logPath;
	/**
	 * 训练参数
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练参数")
	private String configParams;
	/**
	 * 错误信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("错误信息")
	private String errorMessage;
	/**
	 * 模型文件路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型文件路径")
	private String modelPath;
	/**
	 * 完成时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("完成时间")
	private String completedAt;

}
