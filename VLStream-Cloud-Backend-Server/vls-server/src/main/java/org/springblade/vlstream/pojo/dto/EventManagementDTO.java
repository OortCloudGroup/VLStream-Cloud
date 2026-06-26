package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.EventManagement;

import java.io.Serial;

/**
 * Event management table Data Transfer Object (DTO) entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EventManagementDTO extends EventManagement {
	@Serial
	private static final long serialVersionUID = 1L;

}
