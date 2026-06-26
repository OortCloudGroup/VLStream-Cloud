package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.vlstream.enums.AlgorithmTrainingStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Algorithm training task table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_training")
@Schema(description = "VlsAlgorithmTrainingEntity object")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmTraining extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Task name
	 */
	@Schema(description = "Task name")
	private String taskName;
	/**
	 * Algorithm ID
	 */
	@Schema(description = "Algorithm ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * Dataset ID
	 */
	@Schema(description = "Dataset ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long datasetId;
	/**
	 * Training status: pending-pending, training-training, completed-completed, failed-failed
	 */
	@Schema(description = "Training Status")
	private AlgorithmTrainingStatusEnum trainStatus;
	/**
	 * Training Progress Percentage
	 */
	@Schema(description = "Training Progress Percentage")
	private Integer progress;
	/**
	 * Current round
	 */
	@Schema(description = "Current round")
	private Integer epochCurrent;
	/**
	 * Total rounds
	 */
	@Schema(description = "Total rounds")
	private Integer epochTotal;
	/**
	 * Accuracy
	 */
	@Schema(description = "Accuracy")
	private BigDecimal accuracy;
	/**
	 * Precision
	 */
	@Schema(description = "Precision")
	private BigDecimal precisionValue;
	/**
	 * Recall rate
	 */
	@Schema(description = "Recall rate")
	private BigDecimal recallValue;
	/**
	 * mAP value
	 */
	@Schema(description = "mAP value")
	private BigDecimal mapValue;
	/**
	 * Loss value
	 */
	@Schema(description = "Loss value")
	private BigDecimal lossValue;
	/**
	 * GPU usage
	 */
	@Schema(description = "GPU usage")
	private String gpuUsage;
	/**
	 * Start time
	 */
	@Schema(description = "Start time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 * End time
	 */
	@Schema(description = "End time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date endTime;
	/**
	 * Estimated time
	 */
	@Schema(description = "Estimated time")
	private String estimatedTime;
	/**
	 * Model output path
	 */
	@Schema(description = "Model output path")
	private String modelOutputPath;
	/**
	 * onnx model output path
	 */
	@Schema(description = "onnx model output path")
	private String onnxModelOutputPath;
	/**
	 * rknn model output path
	 */
	@Schema(description = "rknn model output path")
	private String rknnModelOutputPath;
	/**
	 * int8 rknn model output path
	 */
	@Schema(description = "int8 rknn model output path")
	private String int8RknnModelOutputPath;
	/**
	 * Log path
	 */
	@Schema(description = "Log path")
	private String logPath;
	/**
	 * Training Parameters
	 */
	@Schema(description = "Training Parameters")
	private String configParams;
	/**
	 * Error message
	 */
	@Schema(description = "Error message")
	private String errorMessage;
	/**
	 * Model file path
	 */
	@Schema(description = "Model file path")
	private String modelPath;
	/**
	 * Completion time
	 */
	@Schema(description = "Completion time")
	private String completedAt;

}
