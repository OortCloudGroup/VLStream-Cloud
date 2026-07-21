/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 算法模型表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_model")
@Schema(description = "VlsAlgorithmModelEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmModel extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 模型名称
	 */
	@Schema(description = "模型名称")
	private String modelName;
	/**
	 * 算法ID
	 */
	@Schema(description = "算法ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * 训练任务ID
	 */
	@Schema(description = "训练任务ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long trainingId;
	/**
	 * 模型版本
	 */
	@Schema(description = "模型版本")
	private Integer version;
	/**
	 * 模型格式：ONNX,PyTorch,TensorFlow
	 */
	@Schema(description = "模型格式：ONNX,PyTorch,TensorFlow")
	private String modelFormat;
	/**
	 * 模型大小
	 */
	@Schema(description = "模型大小")
	private String modelSize;
	/**
	 * 模型文件路径
	 */
	@Schema(description = "模型文件路径")
	private String modelPath;
	/**
	 * onnx模型文件路径
	 */
	@Schema(description = "onnx模型文件路径")
	private String onnxModelPath;
	/**
	 * rknn模型文件路径
	 */
	@Schema(description = "rknn模型文件路径")
	private String rknnModelPath;
	/**
	 * int8 rknn模型输出路径
	 */
	@Schema(description = "int8 rknn模型输出路径")
	private String int8RknnModelOutputPath;
	/**
	 * Hi3519DV500 SVP ACL OM模型输出路径
	 */
	@Schema(description = "Hi3519DV500 OM模型输出路径")
	private String omModelOutputPath;
	/**
	 * 模型准确率
	 */
	@Schema(description = "模型准确率")
	private BigDecimal accuracy;
	/**
	 * 模型描述
	 */
	@Schema(description = "模型描述")
	private String description;
	/**
	 * 下载次数
	 */
	@Schema(description = "下载次数")
	private Integer downloadCount;
	/**
	 * 部署次数
	 */
	@Schema(description = "部署次数")
	private Integer deployCount;
	/**
	 * 发布时间
	 */
	@Schema(description = "发布时间")
	private LocalDateTime publishTime;

}
