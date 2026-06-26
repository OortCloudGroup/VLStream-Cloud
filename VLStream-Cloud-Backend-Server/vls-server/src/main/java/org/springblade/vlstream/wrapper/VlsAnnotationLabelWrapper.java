package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;
import org.springblade.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.Objects;

/**
 * Annotation Label Entity Class wrapper class, returning fields required by the view layer
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
