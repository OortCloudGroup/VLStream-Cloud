/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import com.ruoyi.vlstream.test.vlstream.enums.EventLevelEnum;
import com.ruoyi.vlstream.test.vlstream.enums.EventStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * event
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_event_management")
@Schema(description = "VlsEventManagementEntity对象")
@EqualsAndHashCode(callSuper = true)
public class EventManagement extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * MQTT ID, QoS 1 .
	 */
	@Schema(description = "MQTT上报消息唯一ID")
	private String mqttMessageId;
	/**
	 * device event ID.
	 */
	@Schema(description = "设备侧事件唯一ID")
	private String deviceEventId;
	/**
	 * event mediaId.
	 */
	@Schema(description = "事件图片mediaId")
	private String mediaId;

	/**
	 * event
	 */
	@Schema(description = "事件描述")
	private String eventDesc;
	/**
	 * event
	 */
	@Schema(description = "事件类型")
	private String eventType;
	/**
	 *
	 */
	@Schema(description = "上报位置")
	private String reportLocation;
	/**
	 * device
	 */
	@Schema(description = "上报设备")
	private String reportDevice;
	/**
	 *
	 */
	@Schema(description = "上报图片")
	private String reportImg;
	/**
	 *
	 */
	@Schema(description = "上报时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date reportTime;
	/**
	 * event
	 */
	@Schema(description = "事件级别")
	private EventLevelEnum eventLevel;
	/**
	 * event
	 */
	@Schema(description = "事件状态")
	private EventStatusEnum eventStatus;
	/**
	 * eventdata
	 */
	@Schema(description = "事件数据")
	private String eventData;
	/**
	 * Process
	 */
	@Schema(description = "处理结果")
	private String handleResult;
	/**
	 * info
	 */
	@Schema(description = "反馈信息")
	private String feedbackInfo;
	/**
	 *
	 */
	@Schema(description = "反馈图片")
	private String feedbackImg;
	/**
	 *
	 */
	@Schema(description = "反馈状态")
	private Integer feedbackStatus;

	/**
	 * whether already
	 */
	@Schema(description = "是否已上报")
	private Integer isReport;

}
