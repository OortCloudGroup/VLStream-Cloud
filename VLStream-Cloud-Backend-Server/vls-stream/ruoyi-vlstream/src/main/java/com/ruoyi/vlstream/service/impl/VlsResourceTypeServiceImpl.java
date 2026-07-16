/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.ResourceType;
import com.ruoyi.vlstream.mapper.VlsResourceTypeMapper;
import com.ruoyi.vlstream.service.IVlsResourceTypeService;
import org.springframework.stereotype.Service;

/** Real database service for resource types. */
@Service
public class VlsResourceTypeServiceImpl
    extends AbstractVlsTenantCrudService<VlsResourceTypeMapper, ResourceType>
    implements IVlsResourceTypeService {
}
