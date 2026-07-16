/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * 标注标签实体类 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_annotation_label")
@Schema(description = "VlsAnnotationLabelEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AnnotationLabel extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 关联的标注项目ID
	 */
	@Schema(description = "关联的标注项目ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 * 标签名称
	 */
	@Schema(description = "标签名称")
	private String name;
	/**
	 * 标签颜色(十六进制)
	 */
	@Schema(description = "标签颜色(十六进制)")
	private String color;
	/**
	 * 标签描述
	 */
	@Schema(description = "标签描述")
	private String description;
	/**
	 * 排序顺序
	 */
	@Schema(description = "排序顺序")
	private Integer sortOrder;
	/**
	 * 使用次数统计
	 */
	@Schema(description = "使用次数统计")
	private Integer usageCount;

}
