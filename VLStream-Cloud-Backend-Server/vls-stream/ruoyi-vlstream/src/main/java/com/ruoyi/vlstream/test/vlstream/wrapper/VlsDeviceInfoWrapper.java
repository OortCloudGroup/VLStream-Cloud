package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceInfoVO;

import java.util.Objects;

/**
 * 设备信息表 包装类,返回视图层所需的字段
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
