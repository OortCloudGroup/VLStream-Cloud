/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 移动端场景治理子循环任务表 实体类
 */
@Data
@TableName("vls_mobile_scene_governance_sub_task")
@Schema(description = "MobileSceneGovernanceSubTask对象")
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernanceSubTask extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "主任务ID")
	private Long governanceId;

	@Schema(description = "子任务名称")
	private String name;

	@Schema(description = "执行时间")
	private LocalDateTime executeTime;

	@Schema(description = "任务状态(pending/done/cancel)")
	private String taskStatus;

	@Schema(description = "分析区域ID集合(逗号分隔)")
	private String locationIds;

	@Schema(description = "算法ID集合(逗号分隔)")
	private String algorithmIds;

	@Schema(description = "摄像头ID集合(逗号分隔)")
	private String cameraIds;
}
