/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernanceSubTask;

import java.util.List;

/**
 * 移动端循环治理视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernanceLoopVO extends MobileSceneGovernance {
	private static final long serialVersionUID = 1L;

	@Schema(description = "子循环任务列表")
	private List<MobileSceneGovernanceSubTask> subTaskList;
}
