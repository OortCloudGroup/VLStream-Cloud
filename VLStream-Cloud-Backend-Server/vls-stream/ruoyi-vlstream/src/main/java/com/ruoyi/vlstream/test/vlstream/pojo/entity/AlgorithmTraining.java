/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * algorithmtrainingtask
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
	 * task
	 */
	@Schema(description = "任务名称")
	private String taskName;
	/**
	 * algorithmID
	 */
	@Schema(description = "算法ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * datasetID
	 */
	@Schema(description = "数据集ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long datasetId;
	/**
	 * training : pending- etc. ,training-training in ,completed- ,failed-failed
	 */
	@Schema(description = "训练状态")
	private AlgorithmTrainingStatusEnum trainStatus;
	/**
	 * training
	 */
	@Schema(description = "训练进度百分比")
	private Integer progress;
	/**
	 * current
	 */
	@Schema(description = "当前轮次")
	private Integer epochCurrent;
	/**
	 *
	 */
	@Schema(description = "总轮次")
	private Integer epochTotal;
	/**
	 *
	 */
	@Schema(description = "准确率")
	private BigDecimal accuracy;
	/**
	 *
	 */
	@Schema(description = "精确率")
	private BigDecimal precisionValue;
	/**
	 *
	 */
	@Schema(description = "召回率")
	private BigDecimal recallValue;
	/**
	 * mAP value
	 */
	@Schema(description = "mAP值")
	private BigDecimal mapValue;
	/**
	 * value
	 */
	@Schema(description = "损失值")
	private BigDecimal lossValue;
	/**
	 * GPU
	 */
	@Schema(description = "GPU使用率")
	private String gpuUsage;
	/**
	 * start
	 */
	@Schema(description = "开始时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 * finish
	 */
	@Schema(description = "结束时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date endTime;
	/**
	 *
	 */
	@Schema(description = "预计时间")
	private String estimatedTime;
	/**
	 * model
	 */
	@Schema(description = "模型输出路径")
	private String modelOutputPath;
	/**
	 * onnxmodel
	 */
	@Schema(description = "onnx模型输出路径")
	private String onnxModelOutputPath;
	/**
	 * ONNXConvert : converting-Convert in ,completed- ,failed-failed
	 */
	@Schema(description = "ONNX转换状态")
	private String onnxConversionStatus;
	/**
	 * ONNXConvert failed
	 */
	@Schema(description = "ONNX转换失败原因")
	private String onnxConversionError;
	/**
	 * rknnmodel
	 */
	@Schema(description = "rknn模型输出路径")
	private String rknnModelOutputPath;
	/**
	 * int8 rknnmodel
	 */
	@Schema(description = "int8 rknn模型输出路径")
	private String int8RknnModelOutputPath;
	/**
	 * Hi3519DV500 SVP ACL OMmodel
	 */
	@Schema(description = "Hi3519DV500 OM模型输出路径")
	private String omModelOutputPath;
	/**
	 * OMConvert : converting-Convert in ,completed- ,failed-failed
	 */
	@Schema(description = "OM转换状态")
	private String omConversionStatus;
	/**
	 * OMConvert failed
	 */
	@Schema(description = "OM转换失败原因")
	private String omConversionError;
	/**
	 * log
	 */
	@Schema(description = "日志路径")
	private String logPath;
	/**
	 * trainingparameter
	 */
	@Schema(description = "训练参数")
	private String configParams;
	/**
	 * info
	 */
	@Schema(description = "错误信息")
	private String errorMessage;
	/**
	 * model
	 */
	@Schema(description = "模型文件路径")
	private String modelPath;
	/**
	 *
	 */
	@Schema(description = "完成时间")
	private String completedAt;

}
