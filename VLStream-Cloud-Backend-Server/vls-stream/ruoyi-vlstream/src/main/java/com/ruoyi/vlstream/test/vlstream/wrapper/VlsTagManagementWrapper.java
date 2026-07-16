/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TagManagementVO;

import java.util.Objects;

/**
 * 标签管理表 包装类,返回视图层所需的字段
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
