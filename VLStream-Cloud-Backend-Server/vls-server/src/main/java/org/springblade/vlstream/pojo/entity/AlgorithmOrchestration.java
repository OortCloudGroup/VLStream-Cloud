package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Algorithm orchestration table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_orchestration")
@Schema(description = "VlsAlgorithmOrchestrationEntity object")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmOrchestration extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Orchestration name
	 */
	@Schema(description = "Orchestration name")
	private String orchestrationName;
	/**
	 * Orchestration description
	 */
	@Schema(description = "Orchestration description")
	private String orchestrationDesc;
	/**
	 * Trigger type: realtime-real-time, scheduled-scheduled, manual-manual
	 */
	@Schema(description = "Trigger type: realtime-real-time, scheduled-scheduled, manual-manual")
	private String triggerType;
	/**
	 * Execution mode: serial-Serial, parallel-Parallel
	 */
	@Schema(description = "Execution mode: serial-Serial, parallel-Parallel")
	private String executeMode;
	/**
	 * Algorithm step configuration
	 */
	@Schema(description = "Algorithm step configuration")
	private String algorithmSteps;
	/**
	 * Input Configuration
	 */
	@Schema(description = "Input Configuration")
	private String inputConfig;
	/**
	 * Output Configuration
	 */
	@Schema(description = "Output Configuration")
	private String outputConfig;
	/**
	 * Number of associated devices
	 */
	@Schema(description = "Number of associated devices")
	private Integer deviceCount;
	/**
	 * Run Count
	 */
	@Schema(description = "Run Count")
	private Integer runCount;
	/**
	 * Status: active-active, inactive-inactive, draft-draft
	 */
	@Schema(description = "Status: active-active, inactive-inactive, draft-draft")
	private String orchestrationStatus;
	/**
	 * Last Run Time
	 */
	@Schema(description = "Last Run Time")
	private LocalDateTime lastRunTime;
	/**
	 * Average running time (seconds)
	 */
	@Schema(description = "Average running time (seconds)")
	private Integer avgRunTime;
	/**
	 * Success rate
	 */
	@Schema(description = "Success rate")
	private BigDecimal successRate;

}
