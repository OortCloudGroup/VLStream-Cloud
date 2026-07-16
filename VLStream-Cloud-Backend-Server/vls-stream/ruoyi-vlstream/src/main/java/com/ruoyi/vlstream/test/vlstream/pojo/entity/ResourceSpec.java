package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 * 资源规格配置表 实体类
 *
 * @author Oort
 */
@Data
@TableName("vls_resource_spec")
@Schema(description = "ResourceSpec对象")
@EqualsAndHashCode(callSuper = true)
public class ResourceSpec extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "资源类型ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceTypeId;

	@Schema(description = "规格名称")
	private String specName;

	@Schema(description = "CPU型号")
	private String cpuModel;

	@Schema(description = "vCPU核数")
	private Integer vcpu;

	@Schema(description = "内存(GB)")
	private Integer memoryGb;

	@Schema(description = "GPU描述")
	private String gpuDesc;

	@Schema(description = "系统盘(GB)")
	private Integer systemDiskGb;

	@Schema(description = "数据盘(GB)")
	private Integer dataDiskGb;

	@Schema(description = "是否启用：1-启用，0-禁用")
	private Integer isActive;

	@Schema(description = "排序顺序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remark;
}
