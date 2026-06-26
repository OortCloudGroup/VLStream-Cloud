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
 * Audio exception detection settings table entity class
 */
@Data
@TableName("vls_audio_anomaly_detection_setting")
@Schema(description = "AudioAnomalyDetectionSetting object")
@EqualsAndHashCode(callSuper = true)
public class AudioAnomalyDetectionSetting extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Device Primary Key ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "Audio input exception: 0 no, 1 yes")
	private Integer audioInputAnomalyEnabled;

	@Schema(description = "Sudden sound intensity increase: 0 No, 1 Yes")
	private Integer soundRiseEnabled;

	@Schema(description = "Sudden sound intensity increase sensitivity")
	private Integer soundRiseSensitivity;

	@Schema(description = "Sound intensity threshold")
	private Integer soundIntensityThreshold;

	@Schema(description = "Sudden sound intensity decrease: 0 No, 1 Yes")
	private Integer soundDropEnabled;

	@Schema(description = "Sudden sound intensity decrease sensitivity")
	private Integer soundDropSensitivity;

	@Schema(description = "Remarks")
	private String remark;
}
