/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsResourceSpecMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ResourceSpec;
import com.ruoyi.vlstream.test.vlstream.service.IVlsResourceSpecService;
import org.springframework.stereotype.Service;

/**
 * 资源规格配置表 服务实现类
 */
@Service
public class VlsResourceSpecServiceImpl extends BaseServiceImpl<VlsResourceSpecMapper, ResourceSpec> implements IVlsResourceSpecService {
}
