package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.entity.DeviceTagRelation;
import org.springblade.vlstream.pojo.entity.TagManagement;

import java.io.Serial;

/**
 * Device Tag Association Table Data Transfer Object Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceTagRelationDTO extends DeviceTagRelation {
	@Serial
	private static final long serialVersionUID = 1L;

	// The following are associated objects and do not correspond to database fields
	@Schema(description = "Device Information", hidden = true)
	private DeviceInfo deviceInfo;

	@Schema(description = "Label Information", hidden = true)
	private TagManagement tagInfo;

	@Schema(description = "Label Name", hidden = true)
	private String tagName;

	@Schema(description = "Label Type", hidden = true)
	private String categoryType;

	@Schema(description = "Label Color", hidden = true)
	private String tagColor;

}
