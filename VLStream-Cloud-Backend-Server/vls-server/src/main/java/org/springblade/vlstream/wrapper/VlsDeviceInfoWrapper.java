package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.vo.DeviceInfoVO;

import java.util.Objects;

/**
 * Device Information Table Wrapper Class, returns fields required by view layer
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
