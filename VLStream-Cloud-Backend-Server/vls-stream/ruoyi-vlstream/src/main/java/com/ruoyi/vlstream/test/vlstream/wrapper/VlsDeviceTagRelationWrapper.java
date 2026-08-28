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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceTagRelation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceTagRelationVO;

import java.util.Objects;

/**
 * device , layer field
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
