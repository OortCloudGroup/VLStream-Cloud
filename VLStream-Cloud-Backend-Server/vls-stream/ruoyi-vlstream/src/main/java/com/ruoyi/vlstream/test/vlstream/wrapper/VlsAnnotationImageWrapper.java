/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;

import java.util.Objects;

/**
 * 标注图片信息表 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAnnotationImageWrapper extends BaseEntityWrapper<AnnotationImage, AnnotationImageVO>  {

	public static VlsAnnotationImageWrapper build() {
		return new VlsAnnotationImageWrapper();
 	}

	@Override
	public AnnotationImageVO entityVO(AnnotationImage vlsAnnotationImage) {
		AnnotationImageVO vlsAnnotationImageVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAnnotationImage, AnnotationImageVO.class));

		//User createUser = UserCache.getUser(vlsAnnotationImage.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAnnotationImage.getUpdateUser());
		//vlsAnnotationImageVO.setCreateUserName(createUser.getName());
		//vlsAnnotationImageVO.setUpdateUserName(updateUser.getName());

		return vlsAnnotationImageVO;
	}

}
