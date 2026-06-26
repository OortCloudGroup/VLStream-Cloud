package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.RemoteServers;
import org.springblade.vlstream.pojo.vo.RemoteServersVO;

import java.util.Objects;

/**
 * Remote Server Configuration Table Wrapper Class, returns fields required by view layer
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
