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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;

import java.util.Objects;

/**
 * , layer field
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsTimeStrategyWrapper extends BaseEntityWrapper<TimeStrategy, TimeStrategyVO>  {

	public static VlsTimeStrategyWrapper build() {
		return new VlsTimeStrategyWrapper();
 	}

	@Override
	public TimeStrategyVO entityVO(TimeStrategy vlsTimeStrategy) {
		TimeStrategyVO vlsTimeStrategyVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsTimeStrategy, TimeStrategyVO.class));

		//User createUser = UserCache.getUser(vlsTimeStrategy.getCreateUser());
		//User updateUser = UserCache.getUser(vlsTimeStrategy.getUpdateUser());
		//vlsTimeStrategyVO.setCreateUserName(createUser.getName());
		//vlsTimeStrategyVO.setUpdateUserName(updateUser.getName());

		return vlsTimeStrategyVO;
	}

}
