package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 移动端场景治理主任务表 实体类
 */
@Data
@TableName("vls_mobile_scene_governance")
@Schema(description = "MobileSceneGovernance对象")
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernance extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "治理名称")
	private String name;

	@Schema(description = "治理模式(immediate/loop)")
	private String governanceMode;

	@Schema(description = "循环周期类型(everyday/everyOtherDay/weekly/monthly)")
	private String cycleType;

	@Schema(description = "隔天模式间隔天数")
	private Integer intervalDays;

	@Schema(description = "每周执行日(1-7,逗号分隔)")
	private String weeklyDays;

	@Schema(description = "每月执行日(1-31,逗号分隔)")
	private String monthlyDays;

	@Schema(description = "开始时间")
	private LocalDateTime startTime;

	@Schema(description = "结束时间")
	private LocalDateTime endTime;

	@Schema(description = "触发时间列表(HH:mm:ss,逗号分隔)")
	private String triggerTimes;

	@Schema(description = "分析区域ID集合(逗号分隔)")
	private String locationIds;

	@Schema(description = "算法ID集合(逗号分隔)")
	private String algorithmIds;

	@Schema(description = "摄像头ID集合(逗号分隔)")
	private String cameraIds;

	@Schema(description = "分析描述")
	private String description;

	@TableField(exist = false)
	@Schema(description = "算法名称(逗号分隔)")
	private String algorithmNames;

	@TableField(exist = false)
	@Schema(description = "摄像头名称(逗号分隔)")
	private String cameraNames;
}
