package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 视频录制记录表 实体类
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
	 * 设备ID
	 */
	@Schema(description = "设备ID")
	private Long deviceId;
	/**
	 * 设备名称
	 */
	@Schema(description = "设备名称")
	private String deviceName;
	/**
	 * 视频文件名
	 */
	@Schema(description = "视频文件名")
	private String fileName;
	/**
	 * 视频文件路径
	 */
	@Schema(description = "视频文件路径")
	private String filePath;
	/**
	 * 文件大小(字节)
	 */
	@Schema(description = "文件大小(字节)")
	private Long fileSize;
	/**
	 * 点播url
	 */
	@Schema(description = "点播url")
	private String url;
	/**
	 * 视频时长(秒)
	 */
	@Schema(description = "视频时长(秒)")
	private Integer duration;
	/**
	 * 视频格式
	 */
	@Schema(description = "视频格式")
	private String format;
	/**
	 * 录制开始时间
	 */
	@Schema(description = "录制开始时间")
	private LocalDateTime recordStartTime;
	/**
	 * 录制结束时间
	 */
	@Schema(description = "录制结束时间")
	private LocalDateTime recordEndTime;
	/**
	 * 录制日期(用于按日期分组)
	 */
	@Schema(description = "录制日期(用于按日期分组)")
	private LocalDate recordDate;
	/**
	 * 录制状态
	 */
	@Schema(description = "录制状态")
	private String recordStatus;
	/**
	 * 缩略图路径
	 */
	@Schema(description = "缩略图路径")
	private String thumbnailPath;

}
