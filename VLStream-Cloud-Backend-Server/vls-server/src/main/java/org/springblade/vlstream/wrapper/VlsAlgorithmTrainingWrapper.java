package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.util.Objects;

/**
 * Algorithm training task table wrapper class, returning fields required by the view layer
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
