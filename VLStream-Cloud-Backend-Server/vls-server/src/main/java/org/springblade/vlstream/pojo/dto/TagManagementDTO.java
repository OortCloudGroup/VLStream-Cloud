package org.springblade.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.TagManagement;

import java.io.Serial;
import java.util.List;

/**
 * Label Management Table Data Transfer Object Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TagManagementDTO extends TagManagement {
	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Sub-tag list", hidden = true)
	private List<TagManagementDTO> children;

}
