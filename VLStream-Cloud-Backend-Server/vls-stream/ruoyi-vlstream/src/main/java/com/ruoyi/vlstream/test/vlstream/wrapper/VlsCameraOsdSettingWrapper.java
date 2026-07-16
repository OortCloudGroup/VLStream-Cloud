/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraOsdSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraOsdSettingVO;

/**
 * 摄像机OSD设置表 包装类
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
