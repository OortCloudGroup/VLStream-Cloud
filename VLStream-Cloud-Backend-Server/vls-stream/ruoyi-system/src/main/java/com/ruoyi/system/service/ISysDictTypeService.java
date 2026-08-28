/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.entity.SysDictType;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.List;

/**
 * dict layer
 *
 * @author Lion Li
 */
public interface ISysDictTypeService {


    TableDataInfo<SysDictType> selectPageDictTypeList(SysDictType dictType, PageQuery pageQuery);

    /**
     * Query dict type
     *
     * @param dictType dict typeinfo
     * @return dict typecollectioninfo
     */
    List<SysDictType> selectDictTypeList(SysDictType dictType);

    /**
     * all dict type
     *
     * @return dict typecollectioninfo
     */
    List<SysDictType> selectDictTypeAll();

    /**
     * dict typeQuery dictdata
     *
     * @param dictType dict type
     * @return dictdataset info
     */
    List<SysDictData> selectDictDataByType(String dictType);

    /**
     * dict typeIDQuery info
     *
     * @param dictId dict typeID
     * @return dict type
     */
    SysDictType selectDictTypeById(Long dictId);

    /**
     * dict typeQuery info
     *
     * @param dictType dict type
     * @return dict type
     */
    SysDictType selectDictTypeByType(String dictType);

    /**
     * Batch delete dictinfo
     *
     * @param dictIds need to Delete dictID
     */
    void deleteDictTypeByIds(Long[] dictIds);

    /**
     * Load dict data
     */
    void loadingDictCache();

    /**
     * null / empty dict data
     */
    void clearDictCache();

    /**
     * dict data
     */
    void resetDictCache();

    /**
     * Add dict typeinfo
     *
     * @param dictType dict typeinfo
     * @return
     */
    List<SysDictData> insertDictType(SysDictType dictType);

    /**
     * Update dict typeinfo
     *
     * @param dictType dict typeinfo
     * @return
     */
    List<SysDictData> updateDictType(SysDictType dictType);

    /**
     * Validate dict type whether
     *
     * @param dictType dict type
     * @return
     */
    boolean checkDictTypeUnique(SysDictType dictType);
}
