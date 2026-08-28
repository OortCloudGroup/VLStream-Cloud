/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioLinkageModeSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AudioLinkageModeSettingVO;

/**
 * Set
 */
public class VlsAudioLinkageModeSettingWrapper extends BaseEntityWrapper<AudioLinkageModeSetting, AudioLinkageModeSettingVO> {

	public static VlsAudioLinkageModeSettingWrapper build() {
		return new VlsAudioLinkageModeSettingWrapper();
	}

	@Override
	public AudioLinkageModeSettingVO entityVO(AudioLinkageModeSetting audioLinkageModeSetting) {
		if (audioLinkageModeSetting == null) {
			return null;
		}
		return BeanUtil.copyProperties(audioLinkageModeSetting, AudioLinkageModeSettingVO.class);
	}
}
