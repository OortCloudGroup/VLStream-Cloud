package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Mobile scene governance sub loop task table Entity class
 */
@Data
@TableName("vls_mobile_scene_governance_sub_task")
@Schema(description = "MobileSceneGovernanceSubTask object")
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernanceSubTask extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Main task ID")
	private Long governanceId;

	@Schema(description = "Subtask name")
	private String name;

	@Schema(description = "Execution time")
	private LocalDateTime executeTime;

	@Schema(description = "Task status (pending/done/cancel)")
	private String taskStatus;

	@Schema(description = "Collection of analysis area IDs (comma-separated)")
	private String locationIds;

	@Schema(description = "Algorithm ID set (comma separated)")
	private String algorithmIds;

	@Schema(description = "Camera ID set (comma-separated)")
	private String cameraIds;
}
