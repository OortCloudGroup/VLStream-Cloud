/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioDefenseTimeSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AudioDefenseTimeSettingVO;

/**
 * 音频布防时间设置表 包装类
 */
public class VlsAudioDefenseTimeSettingWrapper extends BaseEntityWrapper<AudioDefenseTimeSetting, AudioDefenseTimeSettingVO> {

	public static VlsAudioDefenseTimeSettingWrapper build() {
		return new VlsAudioDefenseTimeSettingWrapper();
	}

	@Override
	public AudioDefenseTimeSettingVO entityVO(AudioDefenseTimeSetting audioDefenseTimeSetting) {
		if (audioDefenseTimeSetting == null) {
			return null;
		}
		return BeanUtil.copyProperties(audioDefenseTimeSetting, AudioDefenseTimeSettingVO.class);
	}
}
