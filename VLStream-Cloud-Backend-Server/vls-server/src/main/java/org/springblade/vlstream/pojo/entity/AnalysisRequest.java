package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.vlstream.enums.AnalysisRequestStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.util.Date;

/**
 * Intelligent Analysis Request Table Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_analysis_request")
@Schema(description = "VlsAnalysisRequestEntity object")
@EqualsAndHashCode(callSuper = true)
public class AnalysisRequest extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Analysis name
	 */
	@Schema(description = "Analysis name")
	private String analysisName;
	/**
	 * Analysis type
	 */
	@Schema(description = "Analysis type")
	private String analysisType;
	/**
	 * Device ID list, comma separated
	 */
	@Schema(description = "Device ID list, comma separated")
	private String deviceIds;
	/**
	 * Analysis area
	 */
	@Schema(description = "Analysis area, comma-separated")
	private String regionInfo;
	/**
	 * Time range
	 */
	@Schema(description = "Time range")
	private String timeRange;
	/**
	 * Analyze image
	 */
	@Schema(description = "Analyze image")
	private String images;
	/**
	 * Request Status
	 */
	@Schema(description = "Request Status")
	private AnalysisRequestStatusEnum requestStatus;
	/**
	 * Progress percentage
	 */
	@Schema(description = "Progress percentage")
	private Integer progress;
	/**
	 * Result file path
	 */
	@Schema(description = "Result file path")
	private String resultPath;
	/**
	 * Processing start time
	 */
	@Schema(description = "Processing start time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 * Completion time
	 */
	@Schema(description = "Completion time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date completeTime;
	/**
	 * Error message
	 */
	@Schema(description = "Error message")
	private String errorMessage;
	/**
	 * Description information
	 */
	@Schema(description = "Description information")
	private String description;

}
