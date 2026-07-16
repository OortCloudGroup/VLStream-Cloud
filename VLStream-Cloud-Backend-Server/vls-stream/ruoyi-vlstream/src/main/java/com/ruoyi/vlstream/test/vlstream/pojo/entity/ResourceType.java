package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * 资源类型配置表 实体类
 *
 * @author Oort
 */
@Data
@TableName("vls_resource_type")
@Schema(description = "ResourceType对象")
@EqualsAndHashCode(callSuper = true)
public class ResourceType extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "资源类型编码")
	private String typeCode;

	@Schema(description = "资源类型名称")
	private String typeName;

	@Schema(description = "是否启用：1-启用，0-禁用")
	private Integer isActive;

	@Schema(description = "排序顺序")
	private Integer sortOrder;

	@Schema(description = "描述")
	private String description;
}
