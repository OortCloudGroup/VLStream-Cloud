package org.springblade.vlstream.service.impl;

import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.mapper.VlsResourceTypeMapper;
import org.springblade.vlstream.pojo.entity.ResourceType;
import org.springblade.vlstream.service.IVlsResourceTypeService;
import org.springframework.stereotype.Service;

/**
 * Resource Type Configuration Table Service Implementation Class
 */
@Service
public class VlsResourceTypeServiceImpl extends BaseServiceImpl<VlsResourceTypeMapper, ResourceType> implements IVlsResourceTypeService {
}
