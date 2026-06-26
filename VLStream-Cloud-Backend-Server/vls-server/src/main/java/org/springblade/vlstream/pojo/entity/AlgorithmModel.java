package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Algorithm model table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_model")
@Schema(description = "VlsAlgorithmModelEntity object")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmModel extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Model name
	 */
	@Schema(description = "Model name")
	private String modelName;
	/**
	 * Algorithm ID
	 */
	@Schema(description = "Algorithm ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * Training Task ID
	 */
	@Schema(description = "Training Task ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long trainingId;
	/**
	 * Model version
	 */
	@Schema(description = "Model version")
	private Integer version;
	/**
	 * Model format: ONNX, PyTorch, TensorFlow
	 */
	@Schema(description = "Model format: ONNX, PyTorch, TensorFlow")
	private String modelFormat;
	/**
	 * Model size
	 */
	@Schema(description = "Model size")
	private String modelSize;
	/**
	 * Model file path
	 */
	@Schema(description = "Model file path")
	private String modelPath;
	/**
	 * onnx model file path
	 */
	@Schema(description = "onnx model file path")
	private String onnxModelPath;
	/**
	 * rknn model file path
	 */
	@Schema(description = "rknn model file path")
	private String rknnModelPath;
	/**
	 * int8 rknn model output path
	 */
	@Schema(description = "int8 rknn model output path")
	private String int8RknnModelOutputPath;
	/**
	 * Model accuracy
	 */
	@Schema(description = "Model accuracy")
	private BigDecimal accuracy;
	/**
	 * Model description
	 */
	@Schema(description = "Model description")
	private String description;
	/**
	 * Download count
	 */
	@Schema(description = "Download count")
	private Integer downloadCount;
	/**
	 * Deployment count
	 */
	@Schema(description = "Deployment count")
	private Integer deployCount;
	/**
	 * Publish time
	 */
	@Schema(description = "Publish time")
	private LocalDateTime publishTime;

}
