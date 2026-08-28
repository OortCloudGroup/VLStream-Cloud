/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;


/**
 *
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
