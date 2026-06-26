package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;
import org.springblade.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.Objects;

/**
 * Algorithm orchestration table wrapper class, returning fields required by the view layer
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
