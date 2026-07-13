/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

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
