/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * record
 *
 * @author Oort
 * @since 2025-12-25
 */
@Data
@TableName("vls_video_record")
@Schema(description = "VlsVideoRecordEntity对象")
@EqualsAndHashCode(callSuper = true)
public class VideoRecord extends TenantEntity {
	private static final long serialVersionUID = 1L;
	/**
	 * stream
	 */
	@Schema(description = "stream")
	private String stream;
	/**
	 * deviceID
	 */
	@Schema(description = "设备ID")
	private Long deviceId;
	/**
	 * device
	 */
	@Schema(description = "设备名称")
	private String deviceName;
	/**
	 *
	 */
	@Schema(description = "视频文件名")
	private String fileName;
	/**
	 *
	 */
	@Schema(description = "视频文件路径")
	private String filePath;
	/**
	 * ( )
	 */
	@Schema(description = "文件大小(字节)")
	private Long fileSize;
	/**
	 * url
	 */
	@Schema(description = "点播url")
	private String url;
	/**
	 * ( )
	 */
	@Schema(description = "视频时长(秒)")
	private Integer duration;
	/**
	 *
	 */
	@Schema(description = "视频格式")
	private String format;
	/**
	 * start
	 */
	@Schema(description = "录制开始时间")
	private LocalDateTime recordStartTime;
	/**
	 * finish
	 */
	@Schema(description = "录制结束时间")
	private LocalDateTime recordEndTime;
	/**
	 * ( group)
	 */
	@Schema(description = "录制日期(用于按日期分组)")
	private LocalDate recordDate;
	/**
	 *
	 */
	@Schema(description = "录制状态")
	private String recordStatus;
	/**
	 *
	 */
	@Schema(description = "缩略图路径")
	private String thumbnailPath;

}
