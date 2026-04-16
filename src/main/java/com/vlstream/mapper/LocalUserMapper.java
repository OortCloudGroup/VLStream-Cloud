package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.LocalUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * Local User Mapper Interface
 */
@Mapper
public interface LocalUserMapper extends BaseMapper<LocalUser> {
} 