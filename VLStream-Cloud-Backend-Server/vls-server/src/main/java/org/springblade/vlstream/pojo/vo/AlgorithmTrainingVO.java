package org.springblade.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.enums.AlgorithmCategoryEnum;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;

import java.io.Serial;

/**
 * Algorithm training task table view entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmTrainingVO extends AlgorithmTraining {
	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Algorithm name")
	private String algorithmName;

	@Schema(description = "Algorithm type")
	private AlgorithmCategoryEnum trainType;

	@Schema(description = "Corresponding model")
	private String targetModel;

	@Schema(description = "Dataset name")
	private String datasetName;

	@Schema(description = "Creator name")
	private String createdByName;

	@Schema(description = "Training Duration (minutes)")
	private Long durationMinutes;

	@Schema(description = "Training Status Description")
	private String trainStatusDesc;
}
