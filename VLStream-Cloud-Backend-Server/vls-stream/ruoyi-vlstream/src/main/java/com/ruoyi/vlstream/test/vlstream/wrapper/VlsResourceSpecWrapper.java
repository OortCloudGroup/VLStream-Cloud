/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ResourceSpec;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ResourceSpecVO;

/**
 * 资源规格配置表 包装类
 */
public class VlsResourceSpecWrapper extends BaseEntityWrapper<ResourceSpec, ResourceSpecVO> {

	public static VlsResourceSpecWrapper build() {
		return new VlsResourceSpecWrapper();
	}

	@Override
	public ResourceSpecVO entityVO(ResourceSpec resourceSpec) {
		if (resourceSpec == null) {
			return null;
		}
		return BeanUtil.copyProperties(resourceSpec, ResourceSpecVO.class);
	}
}
