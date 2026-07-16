/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioDefenseTimeSetting;


/**
 * 音频布防时间设置表 视图实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioDefenseTimeSettingVO extends AudioDefenseTimeSetting {
	private static final long serialVersionUID = 1L;
}
