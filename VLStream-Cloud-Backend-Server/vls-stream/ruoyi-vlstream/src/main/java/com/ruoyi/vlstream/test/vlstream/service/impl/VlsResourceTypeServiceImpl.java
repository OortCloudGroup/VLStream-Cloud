/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsResourceTypeMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ResourceType;
import com.ruoyi.vlstream.test.vlstream.service.IVlsResourceTypeService;
import org.springframework.stereotype.Service;

/**
 * configuration service
 */
@Service
public class VlsResourceTypeServiceImpl extends BaseServiceImpl<VlsResourceTypeMapper, ResourceType> implements IVlsResourceTypeService {
}
