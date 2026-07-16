/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraDisplaySetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraDisplaySettingVO;

/**
 * 摄像机显示设置表 包装类
 */
public class VlsCameraDisplaySettingWrapper extends BaseEntityWrapper<CameraDisplaySetting, CameraDisplaySettingVO> {

	public static VlsCameraDisplaySettingWrapper build() {
		return new VlsCameraDisplaySettingWrapper();
	}

	@Override
	public CameraDisplaySettingVO entityVO(CameraDisplaySetting cameraDisplaySetting) {
		if (cameraDisplaySetting == null) {
			return null;
		}
		return BeanUtil.copyProperties(cameraDisplaySetting, CameraDisplaySettingVO.class);
	}
}
