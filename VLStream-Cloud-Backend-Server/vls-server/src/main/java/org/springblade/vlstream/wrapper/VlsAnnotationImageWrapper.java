package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.vo.AnnotationImageVO;

import java.util.Objects;

/**
 * Annotation Image Info Table wrapper class, returning fields required by the view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAnnotationImageWrapper extends BaseEntityWrapper<AnnotationImage, AnnotationImageVO>  {

	public static VlsAnnotationImageWrapper build() {
		return new VlsAnnotationImageWrapper();
 	}

	@Override
	public AnnotationImageVO entityVO(AnnotationImage vlsAnnotationImage) {
		AnnotationImageVO vlsAnnotationImageVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAnnotationImage, AnnotationImageVO.class));

		//User createUser = UserCache.getUser(vlsAnnotationImage.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAnnotationImage.getUpdateUser());
		//vlsAnnotationImageVO.setCreateUserName(createUser.getName());
		//vlsAnnotationImageVO.setUpdateUserName(updateUser.getName());

		return vlsAnnotationImageVO;
	}

}
