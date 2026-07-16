package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnalysisRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnalysisRequestVO;

import java.util.Objects;

/**
 * 智能分析请求表 包装类,返回视图层所需的字段
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
