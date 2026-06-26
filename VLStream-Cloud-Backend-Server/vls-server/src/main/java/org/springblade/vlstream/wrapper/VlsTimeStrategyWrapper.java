package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.TimeStrategy;
import org.springblade.vlstream.pojo.vo.TimeStrategyVO;

import java.util.Objects;

/**
 * Time strategy table wrapper class, returning fields required by view layer
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
