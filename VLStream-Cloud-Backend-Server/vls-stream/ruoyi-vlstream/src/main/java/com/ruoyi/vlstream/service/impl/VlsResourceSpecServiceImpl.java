package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.ResourceSpec;
import com.ruoyi.vlstream.mapper.VlsResourceSpecMapper;
import com.ruoyi.vlstream.service.IVlsResourceSpecService;
import org.springframework.stereotype.Service;

/** Real database service for resource specifications. */
@Service
public class VlsResourceSpecServiceImpl
    extends AbstractVlsTenantCrudService<VlsResourceSpecMapper, ResourceSpec>
    implements IVlsResourceSpecService {
}
