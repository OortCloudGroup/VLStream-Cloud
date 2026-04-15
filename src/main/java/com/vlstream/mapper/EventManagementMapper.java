package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.EventManagement;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for event_management table.
 */
@Mapper
public interface EventManagementMapper extends BaseMapper<EventManagement> {
}
