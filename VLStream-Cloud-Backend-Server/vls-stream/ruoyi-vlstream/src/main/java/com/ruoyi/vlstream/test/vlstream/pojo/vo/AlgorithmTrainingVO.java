package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;


/**
 * 算法训练任务表 视图实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmTrainingVO extends AlgorithmTraining {
	private static final long serialVersionUID = 1L;

	@Schema(description = "算法名称")
	private String algorithmName;

	@Schema(description = "算法类型")
	private AlgorithmCategoryEnum trainType;

	@Schema(description = "对应模型")
	private String targetModel;

	@Schema(description = "数据集名称")
	private String datasetName;

	@Schema(description = "创建人姓名")
	private String createdByName;

	@Schema(description = "训练时长（分钟）")
	private Long durationMinutes;

	@Schema(description = "训练状态描述")
	private String trainStatusDesc;
}
