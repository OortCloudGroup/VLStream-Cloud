package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.common.enums.YesNoEnum;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.vlstream.enums.AlgorithmCategoryEnum;

import java.io.Serial;

/**
 * Algorithm table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm")
@Schema(description = "VlsAlgorithmEntity object")
@EqualsAndHashCode(callSuper = true)
public class Algorithm extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Algorithm repository ID to which it belongs
	 */
	@Schema(description = "Algorithm repository ID to which it belongs")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long repositoryId;
	/**
	 * Algorithm name
	 */
	@Schema(description = "Algorithm name")
	private String name;
	/**
	 * Algorithm category
	 */
	@Schema(description = "Algorithm category")
	private AlgorithmCategoryEnum category;
	/**
	 * Algorithm description
	 */
	@Schema(description = "Algorithm description")
	private String description;
	/**
	 * Algorithm image URL
	 */
	@Schema(description = "Algorithm image URL")
	private String imageUrl;
	/**
	 * pt model file path
	 */
	@Schema(description = "pt model file path")
	private String ptModelFilePath;
	/**
	 * Model file path
	 */
	@Schema(description = "onnx model file path")
	private String onnxModelFilePath;
	/**
	 * Algorithm configuration parameters (JSON format)
	 */
	@Schema(description = "Algorithm configuration parameters (JSON format)")
	private String configParams;
	/**
	 * Input format (image, video, etc.)
	 */
	@Schema(description = "Input format (image, video, etc.)")
	private String inputFormat;
	/**
	 * Output format (bbox, mask, keypoint, etc.)
	 */
	@Schema(description = "Output format (bbox, mask, keypoint, etc.)")
	private String outputFormat;
	/**
	 * Whether GPU is required: 0-No, 1-Yes
	 */
	@Schema(description = "Whether GPU is required: 0-No, 1-Yes")
	private Integer gpuRequired;
	/**
	 * Whether it is a system preset algorithm
	 */
	@Schema(description = "Whether it is a system preset algorithm")
	private YesNoEnum isSystem;

}
