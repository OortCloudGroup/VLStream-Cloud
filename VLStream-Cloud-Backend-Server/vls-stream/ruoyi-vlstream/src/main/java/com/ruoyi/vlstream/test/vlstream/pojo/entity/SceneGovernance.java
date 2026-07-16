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


/**
 * 场景治理表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_scene_governance")
@Schema(description = "VlsSceneGovernanceEntity对象")
@EqualsAndHashCode(callSuper = true)
public class SceneGovernance extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "执行类型")
	private String cronExpression;

	@Schema(description = "区域")
	private String location;

	@Schema(description = "摄像头")
	private String cameras;

}
