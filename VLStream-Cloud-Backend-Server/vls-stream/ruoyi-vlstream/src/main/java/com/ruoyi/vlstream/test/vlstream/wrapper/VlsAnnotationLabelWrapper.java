package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.Objects;

/**
 * 标注标签实体类 包装类,返回视图层所需的字段
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAnnotationLabelWrapper extends BaseEntityWrapper<AnnotationLabel, AnnotationLabelVO>  {

	public static VlsAnnotationLabelWrapper build() {
		return new VlsAnnotationLabelWrapper();
 	}

	@Override
	public AnnotationLabelVO entityVO(AnnotationLabel vlsAnnotationLabel) {
		AnnotationLabelVO vlsAnnotationLabelVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAnnotationLabel, AnnotationLabelVO.class));

		//User createUser = UserCache.getUser(vlsAnnotationLabel.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAnnotationLabel.getUpdateUser());
		//vlsAnnotationLabelVO.setCreateUserName(createUser.getName());
		//vlsAnnotationLabelVO.setUpdateUserName(updateUser.getName());

		return vlsAnnotationLabelVO;
	}

}
