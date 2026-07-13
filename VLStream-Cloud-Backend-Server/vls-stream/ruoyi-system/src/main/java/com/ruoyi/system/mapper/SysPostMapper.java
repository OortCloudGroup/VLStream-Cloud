package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SysPost;

import java.util.List;

/**
 * 职务等级表
 *
 * @author Lion Li
 */
public interface SysPostMapper extends BaseMapperPlus<SysPostMapper, SysPost, SysPost> {

    Long selectLatestUpdateTime();

    List<Long> selectPostListByUserId(String userId);
}
