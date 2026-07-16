/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 录制会话持久化记录
 */
@Data
@TableName("vls_recording_session")
@Schema(description = "RecordingSession对象")
@EqualsAndHashCode(callSuper = true)
public class RecordingSession extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "时间策略ID")
	private Long timeStrategyId;

	@Schema(description = "设备ID")
	private Long deviceId;

	@Schema(description = "设备名称")
	private String deviceName;

	@Schema(description = "流地址")
	private String streamUrl;

	@Schema(description = "录制输出目录")
	private String outputDirectory;

	@Schema(description = "录制输出pattern")
	private String outputPattern;

	@Schema(description = "分片秒数")
	private Integer segmentSeconds;

	@Schema(description = "会话签名")
	private String sessionSignature;

	@Schema(description = "会话状态 running/stopped")
	private String sessionStatus;

	@Schema(description = "最后同步时间")
	private LocalDateTime lastSyncTime;

	@Schema(description = "会话开始时间")
	private LocalDateTime sessionStartTime;

	@Schema(description = "会话结束时间")
	private LocalDateTime sessionStopTime;

	@Schema(description = "停止原因")
	private String stopReason;
}
