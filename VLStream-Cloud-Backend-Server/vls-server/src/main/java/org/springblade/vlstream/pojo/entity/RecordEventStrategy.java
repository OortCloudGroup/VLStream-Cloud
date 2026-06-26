package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.util.Map;

/**
 * Camera event strategy (trigger recording/capture) entity class
 *
 * @author Oort
 * @since 2026-02-04
 */
@Data
@TableName(value = "vls_record_event_strategy", autoResultMap = true)
@Schema(description = "RecordEventStrategy object")
@EqualsAndHashCode(callSuper = true)
public class RecordEventStrategy extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Device Number (DeviceInfo.deviceId)")
	private String deviceId;

	@Schema(description = "Enable motion detection")
	private Boolean motionDetectionEnabled;

	@Schema(description = "Enable PTZ motion alarm reporting")
	private Boolean ptzAlarmReportEnabled;

	@Schema(description = "Enable dynamic analysis")
	private Boolean dynamicAnalysisEnabled;

	@Schema(description = "Enable occlusion alarm")
	private Boolean occlusionAlarmEnabled;

	@Schema(description = "Trigger Action (record/snapshot)")
	private String triggerAction;

	@Schema(description = "Pre-alarm recording seconds")
	private Integer preRecordSeconds;

	@Schema(description = "Post-alarm recording seconds")
	private Integer postRecordSeconds;

	@Schema(description = "Alarm frequency (minutes/time)")
	private Integer alarmFrequencyMinutes;

	@Schema(description = "Alarm level (tip/general/important/urgent)")
	private String alarmLevel;

	@Schema(description = "Alarm method (site/message/sms/email/phone, comma-separated)")
	private String alarmMethod;

	@Schema(description = "Receiver ID set (comma-separated)")
	private String receiverIds;

	@Schema(description = "Policy configuration")
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> protectionTime;

}
