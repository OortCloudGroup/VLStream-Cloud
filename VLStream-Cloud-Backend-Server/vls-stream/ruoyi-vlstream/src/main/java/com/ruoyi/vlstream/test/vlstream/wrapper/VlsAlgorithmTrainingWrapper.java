/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.util.Objects;

/**
 * algorithmtrainingtask , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmTrainingWrapper extends BaseEntityWrapper<AlgorithmTraining, AlgorithmTrainingVO>  {

	public static VlsAlgorithmTrainingWrapper build() {
		return new VlsAlgorithmTrainingWrapper();
 	}

	@Override
	public AlgorithmTrainingVO entityVO(AlgorithmTraining vlsAlgorithmTraining) {
		AlgorithmTrainingVO vlsAlgorithmTrainingVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithmTraining, AlgorithmTrainingVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithmTraining.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithmTraining.getUpdateUser());
		//vlsAlgorithmTrainingVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmTrainingVO.setUpdateUserName(updateUser.getName());

		return vlsAlgorithmTrainingVO;
	}

}
