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
 * Audio linkage mode settings table entity class
 */
@Data
@TableName("vls_audio_linkage_mode_setting")
@Schema(description = "AudioLinkageModeSetting object")
@EqualsAndHashCode(callSuper = true)
public class AudioLinkageModeSetting extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Device Primary Key ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "Regular linkage: 0 No, 1 Yes")
	private Integer conventionalLinkageEnabled;

	@Schema(description = "Email linkage: 0 no, 1 yes")
	private Integer emailLinkageEnabled;

	@Schema(description = "Upload center: 0 No, 1 Yes")
	private Integer uploadCenterLinkageEnabled;

	@Schema(description = "Linkage alarm output: 0 No, 1 Yes")
	private Integer alarmOutputLinkageEnabled;

	@Schema(description = "Alarm output channel")
	private String alarmOutputChannel;

	@Schema(description = "Video linkage: 0 No, 1 Yes")
	private Integer recordLinkageEnabled;

	@Schema(description = "Recording channel")
	private String recordChannel;

	@Schema(description = "Remarks")
	private String remark;
}
