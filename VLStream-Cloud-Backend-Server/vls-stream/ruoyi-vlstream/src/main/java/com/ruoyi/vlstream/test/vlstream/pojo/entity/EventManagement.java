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
 * 事件管理表 实体类
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
	 * 事件描述
	 */
	@Schema(description = "事件描述")
	private String eventDesc;
	/**
	 * 事件类型
	 */
	@Schema(description = "事件类型")
	private String eventType;
	/**
	 * 上报位置
	 */
	@Schema(description = "上报位置")
	private String reportLocation;
	/**
	 * 上报设备
	 */
	@Schema(description = "上报设备")
	private String reportDevice;
	/**
	 * 上报图片
	 */
	@Schema(description = "上报图片")
	private String reportImg;
	/**
	 * 上报时间
	 */
	@Schema(description = "上报时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date reportTime;
	/**
	 * 事件级别
	 */
	@Schema(description = "事件级别")
	private EventLevelEnum eventLevel;
	/**
	 * 事件状态
	 */
	@Schema(description = "事件状态")
	private EventStatusEnum eventStatus;
	/**
	 * 事件数据
	 */
	@Schema(description = "事件数据")
	private String eventData;
	/**
	 * 处理结果
	 */
	@Schema(description = "处理结果")
	private String handleResult;
	/**
	 * 反馈信息
	 */
	@Schema(description = "反馈信息")
	private String feedbackInfo;
	/**
	 * 反馈图片
	 */
	@Schema(description = "反馈图片")
	private String feedbackImg;
	/**
	 * 反馈状态
	 */
	@Schema(description = "反馈状态")
	private Integer feedbackStatus;

	/**
	 * 是否已上报
	 */
	@Schema(description = "是否已上报")
	private Integer isReport;

}
