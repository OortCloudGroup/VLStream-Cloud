package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.DeviceTagRelation;
import org.springblade.vlstream.pojo.vo.DeviceTagRelationVO;

import java.util.Objects;

/**
 * Device Tag Association Table Wrapper Class, returns fields required by view layer
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
