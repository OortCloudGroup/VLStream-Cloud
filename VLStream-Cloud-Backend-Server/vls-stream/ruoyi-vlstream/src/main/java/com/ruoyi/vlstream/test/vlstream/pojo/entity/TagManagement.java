package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * 标签管理表 实体类
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
	 * 标签名称
	 */
	@Schema(description = "标签名称")
	private String tagName;
	/**
	 * 标签大类：own-自有标签，public-公共标签
	 */
	@Schema(description = "标签大类：own-自有标签，public-公共标签")
	private String categoryType;
	/**
	 * 层级：1-标签类型，2-具体标签
	 */
	@Schema(description = "层级：1-标签类型，2-具体标签")
	private Integer level;
	/**
	 * 父级ID，level=1时为NULL，level=2时为标签类型ID
	 */
	@Schema(description = "父级ID，level=1时为NULL，level=2时为标签类型ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parentId;
	/**
	 * 排序顺序
	 */
	@Schema(description = "排序顺序")
	private Integer sortOrder;
	/**
	 * 标签颜色
	 */
	@Schema(description = "标签颜色")
	private String tagColor;
	/**
	 * 标签图标
	 */
	@Schema(description = "标签图标")
	private String tagIcon;
	/**
	 * 标签描述
	 */
	@Schema(description = "标签描述")
	private String description;
	/**
	 * 是否启用：1-启用，0-禁用
	 */
	@Schema(description = "是否启用：1-启用，0-禁用")
	private Integer isActive;
	/**
	 * 使用次数
	 */
	@Schema(description = "使用次数")
	private Integer usageCount;

}
