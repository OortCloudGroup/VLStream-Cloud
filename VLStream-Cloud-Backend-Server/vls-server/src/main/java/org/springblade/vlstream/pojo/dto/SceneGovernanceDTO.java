package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.SceneGovernance;

import java.io.Serial;

/**
 * Scene governance table DTO entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SceneGovernanceDTO extends SceneGovernance {
	@Serial
	private static final long serialVersionUID = 1L;

}
