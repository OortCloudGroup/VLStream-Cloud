/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.List;

/**
 * dict layer
 *
 * @author Lion Li
 */
public interface ISysDictDataService {


    TableDataInfo<SysDictData> selectPageDictDataList(SysDictData dictData, PageQuery pageQuery);

    /**
     * Query dictdata
     *
     * @param dictData dictdatainfo
     * @return dictdataset info
     */
    List<SysDictData> selectDictDataList(SysDictData dictData);

    /**
     * dict type and dict value Query dictdatainfo
     *
     * @param dictType dict type
     * @param dictValue dict value
     * @return dict
     */
    String selectDictLabel(String dictType, String dictValue);

    /**
     * dictdataIDQuery info
     *
     * @param dictCode dictdataID
     * @return dictdata
     */
    SysDictData selectDictDataById(Long dictCode);

    /**
     * Batch delete dictdatainfo
     *
     * @param dictCodes need to Delete dictdataID
     */
    void deleteDictDataByIds(Long[] dictCodes);

    /**
     * Add dictdatainfo
     *
     * @param dictData dictdatainfo
     * @return
     */
    List<SysDictData> insertDictData(SysDictData dictData);

    /**
     * Update dictdatainfo
     *
     * @param dictData dictdatainfo
     * @return
     */
    List<SysDictData> updateDictData(SysDictData dictData);
}
