/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceTagRelation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceTagRelationVO;

import java.util.Objects;

/**
 * 设备标签关联表 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsDeviceTagRelationWrapper extends BaseEntityWrapper<DeviceTagRelation, DeviceTagRelationVO>  {

	public static VlsDeviceTagRelationWrapper build() {
		return new VlsDeviceTagRelationWrapper();
 	}

	@Override
	public DeviceTagRelationVO entityVO(DeviceTagRelation vlsDeviceTagRelation) {
		DeviceTagRelationVO vlsDeviceTagRelationVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsDeviceTagRelation, DeviceTagRelationVO.class));

		//User createUser = UserCache.getUser(vlsDeviceTagRelation.getCreateUser());
		//User updateUser = UserCache.getUser(vlsDeviceTagRelation.getUpdateUser());
		//vlsDeviceTagRelationVO.setCreateUserName(createUser.getName());
		//vlsDeviceTagRelationVO.setUpdateUserName(updateUser.getName());

		return vlsDeviceTagRelationVO;
	}

}
