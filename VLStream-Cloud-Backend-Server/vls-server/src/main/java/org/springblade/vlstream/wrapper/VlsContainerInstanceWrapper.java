package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.ContainerInstance;
import org.springblade.vlstream.pojo.vo.ContainerInstanceVO;

import java.util.Objects;

/**
 * Container instance table wrapper class, returning fields required for view layer
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
