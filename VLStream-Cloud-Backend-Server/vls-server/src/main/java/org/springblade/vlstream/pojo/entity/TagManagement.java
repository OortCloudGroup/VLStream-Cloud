package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Label Management Table Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_tag_management")
@Schema(description = "VlsTagManagementEntity object")
@EqualsAndHashCode(callSuper = true)
public class TagManagement extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Label Name
	 */
	@Schema(description = "Label Name")
	private String tagName;
	/**
	 * Label Category: own-own labels, public-public labels
	 */
	@Schema(description = "Label Category: own-own labels, public-public labels")
	private String categoryType;
	/**
	 * Level: 1-Tag type, 2-Specific tag
	 */
	@Schema(description = "Level: 1-Tag type, 2-Specific tag")
	private Integer level;
	/**
	 * Parent ID, NULL when level=1, tag type ID when level=2
	 */
	@Schema(description = "Parent ID, NULL when level=1, tag type ID when level=2")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parentId;
	/**
	 * Sort order
	 */
	@Schema(description = "Sort order")
	private Integer sortOrder;
	/**
	 * Label Color
	 */
	@Schema(description = "Label Color")
	private String tagColor;
	/**
	 * Label Icon
	 */
	@Schema(description = "Label Icon")
	private String tagIcon;
	/**
	 * Label Description
	 */
	@Schema(description = "Label Description")
	private String description;
	/**
	 * Whether enabled: 1-Enabled, 0-Disabled
	 */
	@Schema(description = "Whether enabled: 1-Enabled, 0-Disabled")
	private Integer isActive;
	/**
	 * Number of usages
	 */
	@Schema(description = "Number of usages")
	private Integer usageCount;

}
