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
 * Device Tag Association Table Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_device_tag_relation")
@Schema(description = "VlsDeviceTagRelationEntity object")
@EqualsAndHashCode(callSuper = true)
public class DeviceTagRelation extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Device ID, relates to device_info.id
	 */
	@Schema(description = "Device ID, relates to device_info.id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;
	/**
	 * Label ID, associated with tag_management.id
	 */
	@Schema(description = "Label ID, associated with tag_management.id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long tagId;

}
