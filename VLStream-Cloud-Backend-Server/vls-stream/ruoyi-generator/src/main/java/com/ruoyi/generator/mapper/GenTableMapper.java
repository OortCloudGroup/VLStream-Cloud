/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.generator.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.generator.domain.GenTable;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * data layer
 *
 * @author Lion Li
 */
@InterceptorIgnore(dataPermission = "true")
public interface GenTableMapper extends BaseMapperPlus<GenTableMapper, GenTable, GenTable> {

    /**
     * Query list
     *
     * @param genTable Query
     * @return data collection
     */
    Page<GenTable> selectPageDbTableList(@Param("page") Page<GenTable> page, @Param("genTable") GenTable genTable);

    /**
     * Query list
     *
     * @param tableNames
     * @return data collection
     */
    List<GenTable> selectDbTableListByNames(String[] tableNames);

    /**
     * Query all info
     *
     * @return infocollection
     */
    List<GenTable> selectGenTableAll();

    /**
     * Query ID info
     *
     * @param id ID
     * @return info
     */
    GenTable selectGenTableById(Long id);

    /**
     * Query info
     *
     * @param tableName
     * @return info
     */
    GenTable selectGenTableByName(String tableName);

}
