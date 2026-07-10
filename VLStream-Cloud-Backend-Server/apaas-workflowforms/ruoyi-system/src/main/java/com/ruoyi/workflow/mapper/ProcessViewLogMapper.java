package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.ProcessViewLog;
import com.ruoyi.workflow.domain.vo.ProcessViewLogVo;
import org.apache.ibatis.annotations.Param;

/**
 * 流程访问日志Mapper接口
 *
 * @author lcq
 * @date 2025-08-15
 */
public interface ProcessViewLogMapper extends BaseMapperPlus<ProcessViewLogMapper, ProcessViewLog, ProcessViewLogVo> {

    // 基于窗口函数的“每个用户最新一条访问记录”分页查询
    IPage<ProcessViewLogVo> selectLastVisitPerUserPage(
        Page<?> page,
        @Param(Constants.WRAPPER) Wrapper<ProcessViewLog> wrapper
    );}
