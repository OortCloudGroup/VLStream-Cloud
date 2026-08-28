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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.RemoteServersVO;

import java.util.Objects;

/**
 * service configuration , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsRemoteServersWrapper extends BaseEntityWrapper<RemoteServers, RemoteServersVO>  {

	public static VlsRemoteServersWrapper build() {
		return new VlsRemoteServersWrapper();
 	}

	@Override
	public RemoteServersVO entityVO(RemoteServers vlsRemoteServers) {
		RemoteServersVO vlsRemoteServersVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsRemoteServers, RemoteServersVO.class));

		//User createUser = UserCache.getUser(vlsRemoteServers.getCreateUser());
		//User updateUser = UserCache.getUser(vlsRemoteServers.getUpdateUser());
		//vlsRemoteServersVO.setCreateUserName(createUser.getName());
		//vlsRemoteServersVO.setUpdateUserName(updateUser.getName());

		return vlsRemoteServersVO;
	}

}
