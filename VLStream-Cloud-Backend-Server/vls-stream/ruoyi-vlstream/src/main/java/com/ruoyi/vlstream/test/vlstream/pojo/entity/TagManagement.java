/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 *
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_tag_management")
@Schema(description = "VlsTagManagementEntity对象")
@EqualsAndHashCode(callSuper = true)
public class TagManagement extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@Schema(description = "标签名称")
	private String tagName;
	/**
	 * : own- , public-
	 */
	@Schema(description = "标签大类：own-自有标签，public-公共标签")
	private String categoryType;
	/**
	 * layer : 1- , 2-
	 */
	@Schema(description = "层级：1-标签类型，2-具体标签")
	private Integer level;
	/**
	 * ID, level=1 to NULL, level=2 to ID
	 */
	@Schema(description = "父级ID，level=1时为NULL，level=2时为标签类型ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parentId;
	/**
	 *
	 */
	@Schema(description = "排序顺序")
	private Integer sortOrder;
	/**
	 *
	 */
	@Schema(description = "标签颜色")
	private String tagColor;
	/**
	 *
	 */
	@Schema(description = "标签图标")
	private String tagIcon;
	/**
	 *
	 */
	@Schema(description = "标签描述")
	private String description;
	/**
	 * whether : 1- , 0-
	 */
	@Schema(description = "是否启用：1-启用，0-禁用")
	private Integer isActive;
	/**
	 *
	 */
	@Schema(description = "使用次数")
	private Integer usageCount;

}
