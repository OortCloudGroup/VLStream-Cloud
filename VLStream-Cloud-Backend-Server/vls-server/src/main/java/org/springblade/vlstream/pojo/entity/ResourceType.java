package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Resource Type Configuration Table Entity Class
 *
 * @author Oort
 */
@Data
@TableName("vls_resource_type")
@Schema(description = "ResourceType object")
@EqualsAndHashCode(callSuper = true)
public class ResourceType extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Resource Type Code")
	private String typeCode;

	@Schema(description = "Resource Type Name")
	private String typeName;

	@Schema(description = "Whether enabled: 1-Enabled, 0-Disabled")
	private Integer isActive;

	@Schema(description = "Sort order")
	private Integer sortOrder;

	@Schema(description = "Description")
	private String description;
}
