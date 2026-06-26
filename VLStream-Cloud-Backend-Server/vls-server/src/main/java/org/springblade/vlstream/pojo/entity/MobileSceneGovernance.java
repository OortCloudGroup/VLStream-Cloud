package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Mobile scene governance main task table Entity class
 */
@Data
@TableName("vls_mobile_scene_governance")
@Schema(description = "MobileSceneGovernance object")
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernance extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Governance name")
	private String name;

	@Schema(description = "Governance mode (immediate/loop)")
	private String governanceMode;

	@Schema(description = "Loop cycle type (everyday/everyOtherDay/weekly/monthly)")
	private String cycleType;

	@Schema(description = "Interval days in alternate days mode")
	private Integer intervalDays;

	@Schema(description = "Weekly execution days (1-7, comma separated)")
	private String weeklyDays;

	@Schema(description = "Monthly execution days (1-31, comma separated)")
	private String monthlyDays;

	@Schema(description = "Start time")
	private LocalDateTime startTime;

	@Schema(description = "End time")
	private LocalDateTime endTime;

	@Schema(description = "Trigger time list (HH:mm:ss, comma separated)")
	private String triggerTimes;

	@Schema(description = "Collection of analysis area IDs (comma-separated)")
	private String locationIds;

	@Schema(description = "Algorithm ID set (comma separated)")
	private String algorithmIds;

	@Schema(description = "Camera ID set (comma-separated)")
	private String cameraIds;

	@Schema(description = "Analysis description")
	private String description;

	@TableField(exist = false)
	@Schema(description = "Algorithm name (comma separated)")
	private String algorithmNames;

	@TableField(exist = false)
	@Schema(description = "Camera name (comma-separated)")
	private String cameraNames;
}
