package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;
import org.springblade.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.Objects;

/**
 * Algorithm repository table wrapper class, returning fields required by the view layer
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
