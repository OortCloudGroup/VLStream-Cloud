package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;

import java.util.Objects;

/**
 * 事件管理表 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsEventManagementWrapper extends BaseEntityWrapper<EventManagement, EventManagementVO>  {

	public static VlsEventManagementWrapper build() {
		return new VlsEventManagementWrapper();
 	}

	@Override
	public EventManagementVO entityVO(EventManagement vlsEventManagement) {
		EventManagementVO vlsEventManagementVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsEventManagement, EventManagementVO.class));

		//User createUser = UserCache.getUser(vlsEventManagement.getCreateUser());
		//User updateUser = UserCache.getUser(vlsEventManagement.getUpdateUser());
		//vlsEventManagementVO.setCreateUserName(createUser.getName());
		//vlsEventManagementVO.setUpdateUserName(updateUser.getName());

		return vlsEventManagementVO;
	}

}
