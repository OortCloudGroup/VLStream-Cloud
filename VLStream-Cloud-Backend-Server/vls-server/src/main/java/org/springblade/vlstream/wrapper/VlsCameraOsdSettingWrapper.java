package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.CameraOsdSetting;
import org.springblade.vlstream.pojo.vo.CameraOsdSettingVO;

/**
 * Camera OSD settings table wrapper class
 */
public class VlsCameraOsdSettingWrapper extends BaseEntityWrapper<CameraOsdSetting, CameraOsdSettingVO> {

	public static VlsCameraOsdSettingWrapper build() {
		return new VlsCameraOsdSettingWrapper();
	}

	@Override
	public CameraOsdSettingVO entityVO(CameraOsdSetting cameraOsdSetting) {
		if (cameraOsdSetting == null) {
			return null;
		}
		return BeanUtil.copyProperties(cameraOsdSetting, CameraOsdSettingVO.class);
	}
}
