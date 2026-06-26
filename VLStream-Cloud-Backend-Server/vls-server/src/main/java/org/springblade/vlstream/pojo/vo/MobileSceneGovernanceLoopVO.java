package org.springblade.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.MobileSceneGovernance;
import org.springblade.vlstream.pojo.entity.MobileSceneGovernanceSubTask;

import java.io.Serial;
import java.util.List;

/**
 * Mobile loop governance view object
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernanceLoopVO extends MobileSceneGovernance {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Sub-loop task list")
	private List<MobileSceneGovernanceSubTask> subTaskList;
}
