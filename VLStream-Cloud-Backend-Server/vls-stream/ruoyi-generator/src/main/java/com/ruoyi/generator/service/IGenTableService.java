/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.generator.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;

import java.util.List;
import java.util.Map;

/**
 * service layer
 *
 * @author Lion Li
 */
public interface IGenTableService {

    /**
     * Query field list
     *
     * @param tableId field
     * @return fieldcollection
     */
    List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId);

    /**
     * Query list
     *
     * @param genTable info
     * @return collection
     */
    TableDataInfo<GenTable> selectPageGenTableList(GenTable genTable, PageQuery pageQuery);

    /**
     * Query list
     *
     * @param genTable info
     * @return data collection
     */
    TableDataInfo<GenTable> selectPageDbTableList(GenTable genTable, PageQuery pageQuery);

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
     * Query info
     *
     * @param id ID
     * @return info
     */
    GenTable selectGenTableById(Long id);

    /**
     * Update
     *
     * @param genTable info
     * @return
     */
    void updateGenTable(GenTable genTable);

    /**
     * Delete info
     *
     * @param tableIds need to Delete dataID
     * @return
     */
    void deleteGenTableByIds(Long[] tableIds);

    /**
     * Import
     *
     * @param tableList Import
     */
    void importGenTable(List<GenTable> tableList);

    /**
     *
     *
     * @param tableId
     * @return data
     */
    Map<String, String> previewCode(Long tableId);

    /**
     * Generate ( )
     *
     * @param tableName
     * @return data
     */
    byte[] downloadCode(String tableName);

    /**
     * Generate (Custom )
     *
     * @param tableName
     * @return data
     */
    void generatorCode(String tableName);

    /**
     * data
     *
     * @param tableName
     */
    void synchDb(String tableName);

    /**
     * Generate ( )
     *
     * @param tableNames array
     * @return data
     */
    byte[] downloadCode(String[] tableNames);

    /**
     * Update parameterValidate
     *
     * @param genTable info
     */
    void validateEdit(GenTable genTable);
}
