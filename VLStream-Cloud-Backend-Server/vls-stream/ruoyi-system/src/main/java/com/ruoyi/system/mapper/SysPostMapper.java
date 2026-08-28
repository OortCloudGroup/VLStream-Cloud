/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SysPost;

import java.util.List;

/**
 * etc.
 *
 * @author Lion Li
 */
public interface SysPostMapper extends BaseMapperPlus<SysPostMapper, SysPost, SysPost> {

    Long selectLatestUpdateTime();

    List<Long> selectPostListByUserId(String userId);
}
