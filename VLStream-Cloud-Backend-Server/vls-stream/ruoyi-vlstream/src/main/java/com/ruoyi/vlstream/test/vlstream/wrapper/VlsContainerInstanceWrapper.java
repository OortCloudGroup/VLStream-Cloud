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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;

import java.util.Objects;

/**
 * instance , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsContainerInstanceWrapper extends BaseEntityWrapper<ContainerInstance, ContainerInstanceVO>  {

	public static VlsContainerInstanceWrapper build() {
		return new VlsContainerInstanceWrapper();
 	}

	@Override
	public ContainerInstanceVO entityVO(ContainerInstance vlsContainerInstance) {
		ContainerInstanceVO vlsContainerInstanceVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsContainerInstance, ContainerInstanceVO.class));

		//User createUser = UserCache.getUser(vlsContainerInstance.getCreateUser());
		//User updateUser = UserCache.getUser(vlsContainerInstance.getUpdateUser());
		//vlsContainerInstanceVO.setCreateUserName(createUser.getName());
		//vlsContainerInstanceVO.setUpdateUserName(updateUser.getName());

		return vlsContainerInstanceVO;
	}

}
