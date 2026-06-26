package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Video Recording Record Table Entity Class
 *
 * @author Oort
 * @since 2025-12-25
 */
@Data
@TableName("vls_video_record")
@Schema(description = "VlsVideoRecordEntity object")
@EqualsAndHashCode(callSuper = true)
public class VideoRecord extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;
	/**
	 * stream
	 */
	@Schema(description = "stream")
	private String stream;
	/**
	 * Device ID
	 */
	@Schema(description = "Device ID")
	private Long deviceId;
	/**
	 * Device Name
	 */
	@Schema(description = "Device Name")
	private String deviceName;
	/**
	 * Video File Name
	 */
	@Schema(description = "Video File Name")
	private String fileName;
	/**
	 * Video File Path
	 */
	@Schema(description = "Video File Path")
	private String filePath;
	/**
	 * File size (bytes)
	 */
	@Schema(description = "File size (bytes)")
	private Long fileSize;
	/**
	 * Playback URL
	 */
	@Schema(description = "Playback URL")
	private String url;
	/**
	 * Video Duration (seconds)
	 */
	@Schema(description = "Video Duration (seconds)")
	private Integer duration;
	/**
	 * Video Format
	 */
	@Schema(description = "Video Format")
	private String format;
	/**
	 * Recording start time
	 */
	@Schema(description = "Recording start time")
	private LocalDateTime recordStartTime;
	/**
	 * Recording end time
	 */
	@Schema(description = "Recording end time")
	private LocalDateTime recordEndTime;
	/**
	 * Recording date (used for grouping by date)
	 */
	@Schema(description = "Recording date (used for grouping by date)")
	private LocalDate recordDate;
	/**
	 * Recording status
	 */
	@Schema(description = "Recording status")
	private String recordStatus;
	/**
	 * Thumbnail path
	 */
	@Schema(description = "Thumbnail path")
	private String thumbnailPath;

}
