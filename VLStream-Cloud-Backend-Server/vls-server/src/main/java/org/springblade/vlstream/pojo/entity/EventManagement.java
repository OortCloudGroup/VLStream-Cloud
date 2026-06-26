package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.vlstream.enums.EventLevelEnum;
import org.springblade.vlstream.enums.EventStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.util.Date;

/**
 * Event management table Entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_event_management")
@Schema(description = "VlsEventManagementEntity object")
@EqualsAndHashCode(callSuper = true)
public class EventManagement extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Event description
	 */
	@Schema(description = "Event description")
	private String eventDesc;
	/**
	 * Event type
	 */
	@Schema(description = "Event type")
	private String eventType;
	/**
	 * Report location
	 */
	@Schema(description = "Report location")
	private String reportLocation;
	/**
	 * Report device
	 */
	@Schema(description = "Report device")
	private String reportDevice;
	/**
	 * Report image
	 */
	@Schema(description = "Report image")
	private String reportImg;
	/**
	 * Report time
	 */
	@Schema(description = "Report time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date reportTime;
	/**
	 * Event level
	 */
	@Schema(description = "Event level")
	private EventLevelEnum eventLevel;
	/**
	 * Event status
	 */
	@Schema(description = "Event status")
	private EventStatusEnum eventStatus;
	/**
	 * Event data
	 */
	@Schema(description = "Event data")
	private String eventData;
	/**
	 * Processing result
	 */
	@Schema(description = "Processing result")
	private String handleResult;
	/**
	 * Feedback info
	 */
	@Schema(description = "Feedback info")
	private String feedbackInfo;
	/**
	 * Feedback image
	 */
	@Schema(description = "Feedback image")
	private String feedbackImg;
	/**
	 * Feedback status
	 */
	@Schema(description = "Feedback status")
	private Integer feedbackStatus;

	/**
	 * Whether reported
	 */
	@Schema(description = "Whether reported")
	private Integer isReport;

}
