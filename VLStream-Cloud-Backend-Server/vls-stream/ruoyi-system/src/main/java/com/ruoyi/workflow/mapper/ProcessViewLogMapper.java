/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

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
 * workflow logMapperinterface
 *
 * @author lcq
 * @date 2025-08-15
 */
public interface ProcessViewLogMapper extends BaseMapperPlus<ProcessViewLogMapper, ProcessViewLog, ProcessViewLogVo> {

    // "each user new record" Query
    IPage<ProcessViewLogVo> selectLastVisitPerUserPage(
        Page<?> page,
        @Param(Constants.WRAPPER) Wrapper<ProcessViewLog> wrapper
    );}
