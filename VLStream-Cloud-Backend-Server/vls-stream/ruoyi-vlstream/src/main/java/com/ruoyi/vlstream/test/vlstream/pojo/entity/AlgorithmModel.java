/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * algorithmmodel
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
	 * model
	 */
	@Schema(description = "模型名称")
	private String modelName;
	/**
	 * algorithmID
	 */
	@Schema(description = "算法ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * trainingtaskID
	 */
	@Schema(description = "训练任务ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long trainingId;
	/**
	 * model
	 */
	@Schema(description = "模型版本")
	private Integer version;
	/**
	 * model : ONNX,PyTorch,TensorFlow
	 */
	@Schema(description = "模型格式：ONNX,PyTorch,TensorFlow")
	private String modelFormat;
	/**
	 * model
	 */
	@Schema(description = "模型大小")
	private String modelSize;
	/**
	 * model
	 */
	@Schema(description = "模型文件路径")
	private String modelPath;
	/**
	 * onnxmodel
	 */
	@Schema(description = "onnx模型文件路径")
	private String onnxModelPath;
	/**
	 * rknnmodel
	 */
	@Schema(description = "rknn模型文件路径")
	private String rknnModelPath;
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
	 * model
	 */
	@Schema(description = "模型准确率")
	private BigDecimal accuracy;
	/**
	 * model
	 */
	@Schema(description = "模型描述")
	private String description;
	/**
	 *
	 */
	@Schema(description = "下载次数")
	private Integer downloadCount;
	/**
	 *
	 */
	@Schema(description = "部署次数")
	private Integer deployCount;
	/**
	 *
	 */
	@Schema(description = "发布时间")
	private LocalDateTime publishTime;

}
