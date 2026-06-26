package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Recording session persistent record
 */
@Data
@TableName("vls_recording_session")
@Schema(description = "RecordingSession object")
@EqualsAndHashCode(callSuper = true)
public class RecordingSession extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Time strategy ID")
	private Long timeStrategyId;

	@Schema(description = "Device ID")
	private Long deviceId;

	@Schema(description = "Device Name")
	private String deviceName;

	@Schema(description = "Stream address")
	private String streamUrl;

	@Schema(description = "Recording output directory")
	private String outputDirectory;

	@Schema(description = "Recording output pattern")
	private String outputPattern;

	@Schema(description = "Segment seconds")
	private Integer segmentSeconds;

	@Schema(description = "Session signature")
	private String sessionSignature;

	@Schema(description = "Session status running/stopped")
	private String sessionStatus;

	@Schema(description = "Last Sync Time")
	private LocalDateTime lastSyncTime;

	@Schema(description = "Session start time")
	private LocalDateTime sessionStartTime;

	@Schema(description = "Session end time")
	private LocalDateTime sessionStopTime;

	@Schema(description = "Stop reason")
	private String stopReason;
}
