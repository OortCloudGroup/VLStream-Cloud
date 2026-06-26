package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.vo.AlgorithmAnnotationVO;

import java.util.Objects;

/**
 * Algorithm annotation data table wrapper class, returning fields required by the view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmAnnotationWrapper extends BaseEntityWrapper<AlgorithmAnnotation, AlgorithmAnnotationVO>  {

	public static VlsAlgorithmAnnotationWrapper build() {
		return new VlsAlgorithmAnnotationWrapper();
 	}

	@Override
	public AlgorithmAnnotationVO entityVO(AlgorithmAnnotation vlsAlgorithmAnnotation) {
		AlgorithmAnnotationVO vlsAlgorithmAnnotationVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithmAnnotation, AlgorithmAnnotationVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithmAnnotation.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithmAnnotation.getUpdateUser());
		//vlsAlgorithmAnnotationVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmAnnotationVO.setUpdateUserName(updateUser.getName());

		return vlsAlgorithmAnnotationVO;
	}

}
