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
import com.ruoyi.vlstream.test.common.enums.YesNoEnum;
import org.springblade.core.mp.base.TenantEntity;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;


/**
 * algorithm
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm")
@Schema(description = "VlsAlgorithmEntity对象")
@EqualsAndHashCode(callSuper = true)
public class Algorithm extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * algorithm ID
	 */
	@Schema(description = "所属算法仓库ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long repositoryId;
	/**
	 * algorithm
	 */
	@Schema(description = "算法名称")
	private String name;
	/**
	 * algorithm
	 */
	@Schema(description = "算法分类")
	private AlgorithmCategoryEnum category;
	/**
	 * algorithm
	 */
	@Schema(description = "算法描述")
	private String description;
	/**
	 * algorithm URL
	 */
	@Schema(description = "算法图片URL")
	private String imageUrl;
	/**
	 * ptmodel
	 */
	@Schema(description = "pt模型文件路径")
	private String ptModelFilePath;
	/**
	 * model
	 */
	@Schema(description = "onnx模型文件路径")
	private String onnxModelFilePath;
	/**
	 * algorithmconfigurationparameter (JSON )
	 */
	@Schema(description = "算法配置参数（JSON格式）")
	private String configParams;
	/**
	 * (image、video etc.)
	 */
	@Schema(description = "输入格式（image、video等）")
	private String inputFormat;
	/**
	 * (bbox、mask、keypoint etc.)
	 */
	@Schema(description = "输出格式（bbox、mask、keypoint等）")
	private String outputFormat;
	/**
	 * whether need to GPU: 0- , 1- is
	 */
	@Schema(description = "是否需要GPU：0-否，1-是")
	private Integer gpuRequired;
	/**
	 * whether to algorithm
	 */
	@Schema(description = "是否为系统预置算法")
	private YesNoEnum isSystem;

}
