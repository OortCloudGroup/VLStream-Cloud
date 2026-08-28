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
 * algorithmtrainingtask Excel
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
	 * task
	 */
	@ColumnWidth(20)
	@ExcelProperty("任务名称")
	private String taskName;
	/**
	 * algorithmID
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID")
	private Long algorithmId;
	/**
	 * datasetID
	 */
	@ColumnWidth(20)
	@ExcelProperty("数据集ID")
	private Long datasetId;
	/**
	 * training : pending- etc. ,training-training in ,completed- ,failed-failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练状态：pending-等待,training-训练中,completed-完成,failed-失败")
	private String trainStatus;
	/**
	 * training
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练进度百分比")
	private Integer progress;
	/**
	 * current
	 */
	@ColumnWidth(20)
	@ExcelProperty("当前轮次")
	private Integer epochCurrent;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("总轮次")
	private Integer epochTotal;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("准确率")
	private BigDecimal accuracy;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("精确率")
	private BigDecimal precisionValue;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("召回率")
	private BigDecimal recallValue;
	/**
	 * mAP value
	 */
	@ColumnWidth(20)
	@ExcelProperty("mAP值")
	private BigDecimal mapValue;
	/**
	 * value
	 */
	@ColumnWidth(20)
	@ExcelProperty("损失值")
	private BigDecimal lossValue;
	/**
	 * GPU
	 */
	@ColumnWidth(20)
	@ExcelProperty("GPU使用率")
	private String gpuUsage;
	/**
	 * start
	 */
	@ColumnWidth(20)
	@ExcelProperty("开始时间")
	private LocalDateTime startTime;
	/**
	 * finish
	 */
	@ColumnWidth(20)
	@ExcelProperty("结束时间")
	private LocalDateTime endTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("预计时间")
	private String estimatedTime;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型输出路径")
	private String modelOutputPath;
	/**
	 * log
	 */
	@ColumnWidth(20)
	@ExcelProperty("日志路径")
	private String logPath;
	/**
	 * trainingparameter
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练参数")
	private String configParams;
	/**
	 * info
	 */
	@ColumnWidth(20)
	@ExcelProperty("错误信息")
	private String errorMessage;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型文件路径")
	private String modelPath;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("完成时间")
	private String completedAt;

}
