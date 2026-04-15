package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.LocalUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地用户Mapper接口
 */
@Mapper
public interface LocalUserMapper extends BaseMapper<LocalUser> {
} 