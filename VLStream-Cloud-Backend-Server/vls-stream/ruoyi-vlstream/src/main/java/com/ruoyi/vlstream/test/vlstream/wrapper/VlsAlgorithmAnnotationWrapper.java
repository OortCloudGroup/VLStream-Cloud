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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmAnnotationVO;

import java.util.Objects;

/**
 * algorithmannotationdata , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmAnnotationWrapper extends BaseEntityWrapper<AlgorithmAnnotation, AlgorithmAnnotationVO>  {

	public static VlsAlgorithmAnnotationWrapper build() {
		return new VlsAlgorithmAnnotationWrapper();
 	}

	@Override
	public AlgorithmAnnotationVO entityVO(AlgorithmAnnotation vlsAlgorithmAnnotation) {
		AlgorithmAnnotationVO vlsAlgorithmAnnotationVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithmAnnotation, AlgorithmAnnotationVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithmAnnotation.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithmAnnotation.getUpdateUser());
		//vlsAlgorithmAnnotationVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmAnnotationVO.setUpdateUserName(updateUser.getName());

		return vlsAlgorithmAnnotationVO;
	}

}
