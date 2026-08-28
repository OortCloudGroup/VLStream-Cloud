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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;

import java.util.Objects;

/**
 * annotation info , layer field
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
