/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 标注图片信息表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_annotation_image")
@Schema(description = "VlsAnnotationImageEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AnnotationImage extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 标注项目ID
	 */
	@Schema(description = "标注项目ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 * 图片名称
	 */
	@Schema(description = "图片名称")
	private String imageName;
	/**
	 * 原始文件名
	 */
	@Schema(description = "原始文件名")
	private String originalName;
	/**
	 * 本地存储路径
	 */
	@Schema(description = "本地存储路径")
	private String localPath;
	/**
	 * 文件大小（字节）
	 */
	@Schema(description = "文件大小（字节）")
	private Long fileSize;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date lastModified;
	/**
	 * 是否为导入的图片：0-否，1-是
	 */
	@Schema(description = "是否为导入的图片：0-否，1-是")
	private Integer isImported;
	/**
	 * 导入时间
	 */
	@Schema(description = "导入时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date importTime;

}
