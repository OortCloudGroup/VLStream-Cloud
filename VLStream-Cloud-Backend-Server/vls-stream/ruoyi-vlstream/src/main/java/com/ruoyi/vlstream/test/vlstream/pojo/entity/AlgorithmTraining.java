/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 算法训练任务表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_training")
@Schema(description = "VlsAlgorithmTrainingEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmTraining extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 任务名称
	 */
	@Schema(description = "任务名称")
	private String taskName;
	/**
	 * 算法ID
	 */
	@Schema(description = "算法ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * 数据集ID
	 */
	@Schema(description = "数据集ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long datasetId;
	/**
	 * 训练状态：pending-等待,training-训练中,completed-完成,failed-失败
	 */
	@Schema(description = "训练状态")
	private AlgorithmTrainingStatusEnum trainStatus;
	/**
	 * 训练进度百分比
	 */
	@Schema(description = "训练进度百分比")
	private Integer progress;
	/**
	 * 当前轮次
	 */
	@Schema(description = "当前轮次")
	private Integer epochCurrent;
	/**
	 * 总轮次
	 */
	@Schema(description = "总轮次")
	private Integer epochTotal;
	/**
	 * 准确率
	 */
	@Schema(description = "准确率")
	private BigDecimal accuracy;
	/**
	 * 精确率
	 */
	@Schema(description = "精确率")
	private BigDecimal precisionValue;
	/**
	 * 召回率
	 */
	@Schema(description = "召回率")
	private BigDecimal recallValue;
	/**
	 * mAP值
	 */
	@Schema(description = "mAP值")
	private BigDecimal mapValue;
	/**
	 * 损失值
	 */
	@Schema(description = "损失值")
	private BigDecimal lossValue;
	/**
	 * GPU使用率
	 */
	@Schema(description = "GPU使用率")
	private String gpuUsage;
	/**
	 * 开始时间
	 */
	@Schema(description = "开始时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 * 结束时间
	 */
	@Schema(description = "结束时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date endTime;
	/**
	 * 预计时间
	 */
	@Schema(description = "预计时间")
	private String estimatedTime;
	/**
	 * 模型输出路径
	 */
	@Schema(description = "模型输出路径")
	private String modelOutputPath;
	/**
	 * onnx模型输出路径
	 */
	@Schema(description = "onnx模型输出路径")
	private String onnxModelOutputPath;
	/**
	 * rknn模型输出路径
	 */
	@Schema(description = "rknn模型输出路径")
	private String rknnModelOutputPath;
	/**
	 * int8 rknn模型输出路径
	 */
	@Schema(description = "int8 rknn模型输出路径")
	private String int8RknnModelOutputPath;
	/**
	 * 日志路径
	 */
	@Schema(description = "日志路径")
	private String logPath;
	/**
	 * 训练参数
	 */
	@Schema(description = "训练参数")
	private String configParams;
	/**
	 * 错误信息
	 */
	@Schema(description = "错误信息")
	private String errorMessage;
	/**
	 * 模型文件路径
	 */
	@Schema(description = "模型文件路径")
	private String modelPath;
	/**
	 * 完成时间
	 */
	@Schema(description = "完成时间")
	private String completedAt;

}
