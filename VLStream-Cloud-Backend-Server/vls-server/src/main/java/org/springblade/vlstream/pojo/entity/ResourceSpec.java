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
 * Resource Specification Configuration Table Entity Class
 *
 * @author Oort
 */
@Data
@TableName("vls_resource_spec")
@Schema(description = "ResourceSpec object")
@EqualsAndHashCode(callSuper = true)
public class ResourceSpec extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Resource Type ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceTypeId;

	@Schema(description = "Specification name")
	private String specName;

	@Schema(description = "CPU model")
	private String cpuModel;

	@Schema(description = "vCPU cores")
	private Integer vcpu;

	@Schema(description = "Memory (GB)")
	private Integer memoryGb;

	@Schema(description = "GPU description")
	private String gpuDesc;

	@Schema(description = "System disk (GB)")
	private Integer systemDiskGb;

	@Schema(description = "Data disk (GB)")
	private Integer dataDiskGb;

	@Schema(description = "Whether enabled: 1-Enabled, 0-Disabled")
	private Integer isActive;

	@Schema(description = "Sort order")
	private Integer sortOrder;

	@Schema(description = "Remarks")
	private String remark;
}
