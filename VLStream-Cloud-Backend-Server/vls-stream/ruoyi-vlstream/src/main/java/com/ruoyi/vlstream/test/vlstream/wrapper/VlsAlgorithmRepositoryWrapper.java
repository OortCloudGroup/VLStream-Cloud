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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.Objects;

/**
 * algorithm , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmRepositoryWrapper extends BaseEntityWrapper<AlgorithmRepository, AlgorithmRepositoryVO>  {

	public static VlsAlgorithmRepositoryWrapper build() {
		return new VlsAlgorithmRepositoryWrapper();
 	}

	@Override
	public AlgorithmRepositoryVO entityVO(AlgorithmRepository vlsAlgorithmRepository) {
		AlgorithmRepositoryVO vlsAlgorithmRepositoryVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithmRepository, AlgorithmRepositoryVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithmRepository.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithmRepository.getUpdateUser());
		//vlsAlgorithmRepositoryVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmRepositoryVO.setUpdateUserName(updateUser.getName());

		return vlsAlgorithmRepositoryVO;
	}

}
