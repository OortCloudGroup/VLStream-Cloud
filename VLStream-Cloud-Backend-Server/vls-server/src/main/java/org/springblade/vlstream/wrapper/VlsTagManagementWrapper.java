package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.TagManagement;
import org.springblade.vlstream.pojo.vo.TagManagementVO;

import java.util.Objects;

/**
 * Label Management Table wrapper class, returning fields required by the view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsTagManagementWrapper extends BaseEntityWrapper<TagManagement, TagManagementVO>  {

	public static VlsTagManagementWrapper build() {
		return new VlsTagManagementWrapper();
 	}

	@Override
	public TagManagementVO entityVO(TagManagement vlsTagManagement) {
		TagManagementVO vlsTagManagementVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsTagManagement, TagManagementVO.class));

		//User createUser = UserCache.getUser(vlsTagManagement.getCreateUser());
		//User updateUser = UserCache.getUser(vlsTagManagement.getUpdateUser());
		//vlsTagManagementVO.setCreateUserName(createUser.getName());
		//vlsTagManagementVO.setUpdateUserName(updateUser.getName());

		return vlsTagManagementVO;
	}

}
