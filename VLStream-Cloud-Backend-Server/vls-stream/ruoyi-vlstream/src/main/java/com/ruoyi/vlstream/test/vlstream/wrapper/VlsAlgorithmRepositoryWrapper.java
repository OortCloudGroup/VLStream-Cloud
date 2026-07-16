package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmRepository;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmRepositoryVO;

import java.util.Objects;

/**
 * 算法仓库表 包装类,返回视图层所需的字段
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
