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
 * 算法表 实体类
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
	 * 所属算法仓库ID
	 */
	@Schema(description = "所属算法仓库ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long repositoryId;
	/**
	 * 算法名称
	 */
	@Schema(description = "算法名称")
	private String name;
	/**
	 * 算法分类
	 */
	@Schema(description = "算法分类")
	private AlgorithmCategoryEnum category;
	/**
	 * 算法描述
	 */
	@Schema(description = "算法描述")
	private String description;
	/**
	 * 算法图片URL
	 */
	@Schema(description = "算法图片URL")
	private String imageUrl;
	/**
	 * pt模型文件路径
	 */
	@Schema(description = "pt模型文件路径")
	private String ptModelFilePath;
	/**
	 * 模型文件路径
	 */
	@Schema(description = "onnx模型文件路径")
	private String onnxModelFilePath;
	/**
	 * 算法配置参数（JSON格式）
	 */
	@Schema(description = "算法配置参数（JSON格式）")
	private String configParams;
	/**
	 * 输入格式（image、video等）
	 */
	@Schema(description = "输入格式（image、video等）")
	private String inputFormat;
	/**
	 * 输出格式（bbox、mask、keypoint等）
	 */
	@Schema(description = "输出格式（bbox、mask、keypoint等）")
	private String outputFormat;
	/**
	 * 是否需要GPU：0-否，1-是
	 */
	@Schema(description = "是否需要GPU：0-否，1-是")
	private Integer gpuRequired;
	/**
	 * 是否为系统预置算法
	 */
	@Schema(description = "是否为系统预置算法")
	private YesNoEnum isSystem;

}
