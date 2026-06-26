package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.vo.AlgorithmModelVO;

import java.util.Objects;

/**
 * Algorithm model table wrapper class, returning fields required by the view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAlgorithmModelWrapper extends BaseEntityWrapper<AlgorithmModel, AlgorithmModelVO>  {

	public static VlsAlgorithmModelWrapper build() {
		return new VlsAlgorithmModelWrapper();
 	}

	@Override
	public AlgorithmModelVO entityVO(AlgorithmModel vlsAlgorithmModel) {
		AlgorithmModelVO vlsAlgorithmModelVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAlgorithmModel, AlgorithmModelVO.class));

		//User createUser = UserCache.getUser(vlsAlgorithmModel.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAlgorithmModel.getUpdateUser());
		//vlsAlgorithmModelVO.setCreateUserName(createUser.getName());
		//vlsAlgorithmModelVO.setUpdateUserName(updateUser.getName());

		String downloadPath = String.format("http://oort.oortcloudsmart.com:21410/bus/vls-server/vlsAlgorithmTraining/download-model?id=%s&type=int8-rknn", vlsAlgorithmModel.getId());
		vlsAlgorithmModelVO.setModelDownloadPath(downloadPath);
		return vlsAlgorithmModelVO;
	}

}
