/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import com.ruoyi.vlstream.test.vlstream.enums.AnalysisRequestStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * can
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_analysis_request")
@Schema(description = "VlsAnalysisRequestEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AnalysisRequest extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@Schema(description = "分析名称")
	private String analysisName;
	/**
	 *
	 */
	@Schema(description = "分析类型")
	private String analysisType;
	/**
	 * deviceID ,
	 */
	@Schema(description = "设备ID列表，逗号分隔")
	private String deviceIds;
	/**
	 *
	 */
	@Schema(description = "分析区域，逗号分隔")
	private String regionInfo;
	/**
	 *
	 */
	@Schema(description = "时间范围")
	private String timeRange;
	/**
	 *
	 */
	@Schema(description = "分析图片")
	private String images;
	/**
	 *
	 */
	@Schema(description = "请求状态")
	private AnalysisRequestStatusEnum requestStatus;
	/**
	 * Process
	 */
	@Schema(description = "处理进度百分比")
	private Integer progress;
	/**
	 *
	 */
	@Schema(description = "结果文件路径")
	private String resultPath;
	/**
	 * startProcess
	 */
	@Schema(description = "开始处理时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 *
	 */
	@Schema(description = "完成时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date completeTime;
	/**
	 * info
	 */
	@Schema(description = "错误信息")
	private String errorMessage;
	/**
	 * info
	 */
	@Schema(description = "描述信息")
	private String description;

}
