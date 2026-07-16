package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Map;

/**
 * 摄像头事件策略(触发录像/抓图) 实体类
 *
 * @author Oort
 * @since 2026-02-04
 */
@Data
@TableName(value = "vls_record_event_strategy", autoResultMap = true)
@Schema(description = "RecordEventStrategy对象")
@EqualsAndHashCode(callSuper = true)
public class RecordEventStrategy extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备编号(DeviceInfo.deviceId)")
	private String deviceId;

	@Schema(description = "启用移动侦测")
	private Boolean motionDetectionEnabled;

	@Schema(description = "启用PTZ运动报警上报")
	private Boolean ptzAlarmReportEnabled;

	@Schema(description = "启用动态分析")
	private Boolean dynamicAnalysisEnabled;

	@Schema(description = "启用遮挡报警")
	private Boolean occlusionAlarmEnabled;

	@Schema(description = "触发动作(record/snapshot)")
	private String triggerAction;

	@Schema(description = "触发报警前录制秒数")
	private Integer preRecordSeconds;

	@Schema(description = "触发报警后录制秒数")
	private Integer postRecordSeconds;

	@Schema(description = "告警频率(分钟/次)")
	private Integer alarmFrequencyMinutes;

	@Schema(description = "告警级别(tip/general/important/urgent)")
	private String alarmLevel;

	@Schema(description = "告警方式(site/message/sms/email/phone,逗号分隔)")
	private String alarmMethod;

	@Schema(description = "接收人ID集合(逗号分隔)")
	private String receiverIds;

	@Schema(description = "策略配置")
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> protectionTime;

}
