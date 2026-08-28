/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
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
 * annotation
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
	 * annotation item ID
	 */
	@Schema(description = "关联的标注项目ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 *
	 */
	@Schema(description = "标签名称")
	private String name;
	/**
	 * ( )
	 */
	@Schema(description = "标签颜色(十六进制)")
	private String color;
	/**
	 *
	 */
	@Schema(description = "标签描述")
	private String description;
	/**
	 *
	 */
	@Schema(description = "排序顺序")
	private Integer sortOrder;
	/**
	 *
	 */
	@Schema(description = "使用次数统计")
	private Integer usageCount;

}
