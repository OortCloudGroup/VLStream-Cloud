package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ResourceType;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ResourceTypeVO;

/**
 * 资源类型配置表 包装类
 */
public class VlsResourceTypeWrapper extends BaseEntityWrapper<ResourceType, ResourceTypeVO> {

	public static VlsResourceTypeWrapper build() {
		return new VlsResourceTypeWrapper();
	}

	@Override
	public ResourceTypeVO entityVO(ResourceType resourceType) {
		if (resourceType == null) {
			return null;
		}
		return BeanUtil.copyProperties(resourceType, ResourceTypeVO.class);
	}
}
