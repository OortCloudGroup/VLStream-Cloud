package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Scene governance table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_scene_governance")
@Schema(description = "VlsSceneGovernanceEntity object")
@EqualsAndHashCode(callSuper = true)
public class SceneGovernance extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Name")
	private String name;

	@Schema(description = "Description")
	private String description;

	@Schema(description = "Execution type")
	private String cronExpression;

	@Schema(description = "Area")
	private String location;

	@Schema(description = "Camera")
	private String cameras;

}
