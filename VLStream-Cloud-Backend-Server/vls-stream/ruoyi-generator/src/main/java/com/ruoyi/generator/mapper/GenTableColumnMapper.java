/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.generator.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.generator.domain.GenTableColumn;

import java.util.List;

/**
 * field data layer
 *
 * @author Lion Li
 */
@InterceptorIgnore(dataPermission = "true")
public interface GenTableColumnMapper extends BaseMapperPlus<GenTableColumnMapper, GenTableColumn, GenTableColumn> {
    /**
     * Query info
     *
     * @param tableName
     * @return info
     */
    List<GenTableColumn> selectDbTableColumnsByName(String tableName);

}
