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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceInfoVO;

import java.util.Objects;

/**
 * deviceinfo , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsDeviceInfoWrapper extends BaseEntityWrapper<DeviceInfo, DeviceInfoVO>  {

	public static VlsDeviceInfoWrapper build() {
		return new VlsDeviceInfoWrapper();
 	}

	@Override
	public DeviceInfoVO entityVO(DeviceInfo vlsDeviceInfo) {
		DeviceInfoVO vlsDeviceInfoVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsDeviceInfo, DeviceInfoVO.class));

		//User createUser = UserCache.getUser(vlsDeviceInfo.getCreateUser());
		//User updateUser = UserCache.getUser(vlsDeviceInfo.getUpdateUser());
		//vlsDeviceInfoVO.setCreateUserName(createUser.getName());
		//vlsDeviceInfoVO.setUpdateUserName(updateUser.getName());

		return vlsDeviceInfoVO;
	}

}
