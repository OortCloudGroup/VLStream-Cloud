package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.ResourceSpec;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for VLS resource specifications. */
@Mapper
public interface VlsResourceSpecMapper extends BaseMapperPlus<VlsResourceSpecMapper, ResourceSpec, ResourceSpec> {
}
