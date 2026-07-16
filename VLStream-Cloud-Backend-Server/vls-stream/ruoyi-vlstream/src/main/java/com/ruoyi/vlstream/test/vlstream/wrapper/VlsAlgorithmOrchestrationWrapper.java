/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.Objects;

/**
 * 算法编排表 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmOrchestrationWrapper extends BaseEntityWrapper<AlgorithmOrchestration, AlgorithmOrchestrationVO>  {

	public static VlsAlgorithmOrchestrationWrapper build() {
		return new VlsAlgorithmOrchestrationWrapper();
 	}

	@Override
	public AlgorithmOrchestrationVO entityVO(AlgorithmOrchestration vlsAlgorithmOrchestration) {
		AlgorithmOrchestrationVO vlsAlgorithmOrchestrationVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithmOrchestration, AlgorithmOrchestrationVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithmOrchestration.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithmOrchestration.getUpdateUser());
		//vlsAlgorithmOrchestrationVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmOrchestrationVO.setUpdateUserName(updateUser.getName());

		return vlsAlgorithmOrchestrationVO;
	}

}
