package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmAnnotationVO;

import java.util.Objects;

/**
 * 算法标注数据表 包装类,返回视图层所需的字段
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
