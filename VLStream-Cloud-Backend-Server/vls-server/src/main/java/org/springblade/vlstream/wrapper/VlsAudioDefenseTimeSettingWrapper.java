package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AudioDefenseTimeSetting;
import org.springblade.vlstream.pojo.vo.AudioDefenseTimeSettingVO;

/**
 * Audio arming schedule setting table wrapper class
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
