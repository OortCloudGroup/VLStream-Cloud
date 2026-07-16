/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.SpringUtil;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationInstanceVO;

import java.util.Objects;

/**
 * 标注实例实体类 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAnnotationInstanceWrapper extends BaseEntityWrapper<AnnotationInstance, AnnotationInstanceVO>  {

	public static VlsAnnotationInstanceWrapper build() {
		return new VlsAnnotationInstanceWrapper();
 	}

	@Override
	public AnnotationInstanceVO entityVO(AnnotationInstance vlsAnnotationInstance) {
		AnnotationInstanceVO vlsAnnotationInstanceVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAnnotationInstance, AnnotationInstanceVO.class));

		//User createUser = UserCache.getUser(vlsAnnotationInstance.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAnnotationInstance.getUpdateUser());
		//vlsAnnotationInstanceVO.setCreateUserName(createUser.getName());
		//vlsAnnotationInstanceVO.setUpdateUserName(updateUser.getName());
		AnnotationImage annotationImage = SpringUtil.getBean(VlsAnnotationImageMapper.class).selectById(vlsAnnotationInstance.getImageId());
		vlsAnnotationInstanceVO.setImageName(annotationImage.getImageName());
		return vlsAnnotationInstanceVO;
	}

}
