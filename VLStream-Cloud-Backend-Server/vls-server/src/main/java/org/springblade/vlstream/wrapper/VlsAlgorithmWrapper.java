package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.vo.AlgorithmVO;

import java.util.Objects;

/**
 * Algorithm table wrapper class, returning fields required by the view layer
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
