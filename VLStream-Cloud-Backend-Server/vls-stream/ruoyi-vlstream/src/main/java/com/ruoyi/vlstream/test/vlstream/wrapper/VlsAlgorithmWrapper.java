/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmVO;

import java.util.Objects;

/**
 * 算法表 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmWrapper extends BaseEntityWrapper<Algorithm, AlgorithmVO>  {

	public static VlsAlgorithmWrapper build() {
		return new VlsAlgorithmWrapper();
 	}

	@Override
	public AlgorithmVO entityVO(Algorithm vlsAlgorithm) {
		AlgorithmVO vlsAlgorithmVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithm, AlgorithmVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithm.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithm.getUpdateUser());
		//vlsAlgorithmVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmVO.setUpdateUserName(updateUser.getName());

		return vlsAlgorithmVO;
	}

}
