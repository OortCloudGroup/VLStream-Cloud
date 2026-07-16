/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.SceneGovernance;


/**
 * 场景治理表 视图实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SceneGovernanceVO extends SceneGovernance {
	private static final long serialVersionUID = 1L;

	@Schema(description = "算法名称")
	private String algorithmName;

	@Schema(description = "摄像头名称")
	private String camerasName;

}
