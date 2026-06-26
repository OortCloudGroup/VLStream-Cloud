package org.springblade.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.vlstream.mapper.VlsAnnotationImageMapper;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;
import org.springblade.vlstream.pojo.vo.AnnotationInstanceVO;

import java.util.Objects;

/**
 * Annotation Instance Entity Class wrapper class, returning fields required by the view layer
 *
 * @author Oort
 * @since 2025-12-23
 */
public class VlsAnnotationInstanceWrapper extends BaseEntityWrapper<AnnotationInstance, AnnotationInstanceVO>  {

	public static VlsAnnotationInstanceWrapper build() {
		return new VlsAnnotationInstanceWrapper();
 	}

	@Override
	public AnnotationInstanceVO entityVO(AnnotationInstance vlsAnnotationInstance) {
		AnnotationInstanceVO vlsAnnotationInstanceVO = Objects.requireNonNull(BeanUtil.copyProperties(vlsAnnotationInstance, AnnotationInstanceVO.class));

		//User createUser = UserCache.getUser(vlsAnnotationInstance.getCreateUser());
		//User updateUser = UserCache.getUser(vlsAnnotationInstance.getUpdateUser());
		//vlsAnnotationInstanceVO.setCreateUserName(createUser.getName());
		//vlsAnnotationInstanceVO.setUpdateUserName(updateUser.getName());
		AnnotationImage annotationImage = SpringUtil.getBean(VlsAnnotationImageMapper.class).selectById(vlsAnnotationInstance.getImageId());
		vlsAnnotationInstanceVO.setImageName(annotationImage.getImageName());
		return vlsAnnotationInstanceVO;
	}

}
