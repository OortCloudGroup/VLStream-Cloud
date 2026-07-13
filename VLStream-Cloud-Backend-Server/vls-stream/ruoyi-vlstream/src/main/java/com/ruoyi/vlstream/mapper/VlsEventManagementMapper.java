package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.EventManagement;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for VLS event management records.
 */
@Mapper
public interface VlsEventManagementMapper extends BaseMapperPlus<VlsEventManagementMapper, EventManagement, EventManagement> {
}
