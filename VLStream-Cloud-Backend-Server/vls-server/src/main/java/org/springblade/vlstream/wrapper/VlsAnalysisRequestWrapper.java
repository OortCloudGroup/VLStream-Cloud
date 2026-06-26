package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AnalysisRequest;
import org.springblade.vlstream.pojo.vo.AnalysisRequestVO;

import java.util.Objects;

/**
 * Intelligent Analysis Request Table wrapper class, returning fields required by the view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAnalysisRequestWrapper extends BaseEntityWrapper<AnalysisRequest, AnalysisRequestVO>  {

	public static VlsAnalysisRequestWrapper build() {
		return new VlsAnalysisRequestWrapper();
 	}

	@Override
	public AnalysisRequestVO entityVO(AnalysisRequest vlsAnalysisRequest) {
		AnalysisRequestVO vlsAnalysisRequestVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAnalysisRequest, AnalysisRequestVO.class));

		//User createUser = UserCache.getUser(vlsAnalysisRequest.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAnalysisRequest.getUpdateUser());
		//vlsAnalysisRequestVO.setCreateUserName(createUser.getName());
		//vlsAnalysisRequestVO.setUpdateUserName(updateUser.getName());

		return vlsAnalysisRequestVO;
	}

}
