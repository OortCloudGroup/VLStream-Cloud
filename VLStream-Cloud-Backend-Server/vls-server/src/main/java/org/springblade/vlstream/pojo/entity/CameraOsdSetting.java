package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Camera OSD settings table entity class
 */
@Data
@TableName("vls_camera_osd_setting")
@Schema(description = "CameraOsdSetting object")
@EqualsAndHashCode(callSuper = true)
public class CameraOsdSetting extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Device Primary Key ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "Display name:0 no 1 yes")
	private Integer showName;

	@Schema(description = "Display date:0 no 1 yes")
	private Integer showDate;

	@Schema(description = "Display week:0 no 1 yes")
	private Integer showWeek;

	@Schema(description = "Channel name")
	private String channelName;

	@Schema(description = "Time format")
	private String timeFormat;

	@Schema(description = "Date format")
	private String dateFormat;

	@Schema(description = "Character overlay 1 enabled: 0 No, 1 Yes")
	private Integer overlay1Enabled;

	@Schema(description = "Character overlay 1 content")
	private String overlay1Text;

	@Schema(description = "Character overlay 2 enabled: 0 No, 1 Yes")
	private Integer overlay2Enabled;

	@Schema(description = "Character overlay 2 content")
	private String overlay2Text;

	@Schema(description = "Character overlay 3 enabled: 0 No, 1 Yes")
	private Integer overlay3Enabled;

	@Schema(description = "Character overlay 3 content")
	private String overlay3Text;

	@Schema(description = "OSD properties")
	private String osdProperty;

	@Schema(description = "OSD font")
	private String osdFont;

	@Schema(description = "OSD color")
	private String osdColor;

	@Schema(description = "Alignment")
	private String alignMode;

	@Schema(description = "Remarks")
	private String remark;
}
