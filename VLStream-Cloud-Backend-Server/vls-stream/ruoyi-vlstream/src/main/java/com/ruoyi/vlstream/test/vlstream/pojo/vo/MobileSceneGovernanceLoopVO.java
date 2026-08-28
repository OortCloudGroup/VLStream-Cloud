/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernanceSubTask;

import java.util.List;

/**
 * loop object
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MobileSceneGovernanceLoopVO extends MobileSceneGovernance {
	private static final long serialVersionUID = 1L;

	@Schema(description = "子循环任务列表")
	private List<MobileSceneGovernanceSubTask> subTaskList;
}
