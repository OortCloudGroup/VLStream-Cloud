package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Map;

/**
 * 音频布防时间设置表 实体类
 */
@Data
@TableName(value = "vls_audio_defense_time_setting", autoResultMap = true)
@Schema(description = "AudioDefenseTimeSetting对象")
@EqualsAndHashCode(callSuper = true)
public class AudioDefenseTimeSetting extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备主键ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "时间策略配置")
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> protectionTime;

	@Schema(description = "备注")
	private String remark;
}
