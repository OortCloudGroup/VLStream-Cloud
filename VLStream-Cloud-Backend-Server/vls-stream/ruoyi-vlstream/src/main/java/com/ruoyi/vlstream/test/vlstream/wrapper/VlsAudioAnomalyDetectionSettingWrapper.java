/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioAnomalyDetectionSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AudioAnomalyDetectionSettingVO;

/**
 * Set
 */
public class VlsAudioAnomalyDetectionSettingWrapper extends BaseEntityWrapper<AudioAnomalyDetectionSetting, AudioAnomalyDetectionSettingVO> {

	public static VlsAudioAnomalyDetectionSettingWrapper build() {
		return new VlsAudioAnomalyDetectionSettingWrapper();
	}

	@Override
	public AudioAnomalyDetectionSettingVO entityVO(AudioAnomalyDetectionSetting audioAnomalyDetectionSetting) {
		if (audioAnomalyDetectionSetting == null) {
			return null;
		}
		return BeanUtil.copyProperties(audioAnomalyDetectionSetting, AudioAnomalyDetectionSettingVO.class);
	}
}
