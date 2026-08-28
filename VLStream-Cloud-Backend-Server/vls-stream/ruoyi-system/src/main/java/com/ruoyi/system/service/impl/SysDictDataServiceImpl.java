/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.CacheUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * dict layer Process
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    private final SysDictDataMapper baseMapper;

    @Override
    public TableDataInfo<SysDictData> selectPageDictDataList(SysDictData dictData, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDictData> lqw = new LambdaQueryWrapper<SysDictData>()
            .eq(StringUtils.isNotBlank(dictData.getDictType()), SysDictData::getDictType, dictData.getDictType())
            .like(StringUtils.isNotBlank(dictData.getDictLabel()), SysDictData::getDictLabel, dictData.getDictLabel())
            .eq(StringUtils.isNotBlank(dictData.getStatus()), SysDictData::getStatus, dictData.getStatus())
            .orderByAsc(SysDictData::getDictSort);
        Page<SysDictData> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * Query dictdata
     *
     * @param dictData dictdatainfo
     * @return dictdataset info
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysDictData>()
            .eq(StringUtils.isNotBlank(dictData.getDictType()), SysDictData::getDictType, dictData.getDictType())
            .like(StringUtils.isNotBlank(dictData.getDictLabel()), SysDictData::getDictLabel, dictData.getDictLabel())
            .eq(StringUtils.isNotBlank(dictData.getStatus()), SysDictData::getStatus, dictData.getStatus())
            .orderByAsc(SysDictData::getDictSort));
    }

    /**
     * dict type and dict value Query dictdatainfo
     *
     * @param dictType dict type
     * @param dictValue dict value
     * @return dict
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysDictData>()
                .select(SysDictData::getDictLabel)
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue))
            .getDictLabel();
    }

    /**
     * dictdataIDQuery info
     *
     * @param dictCode dictdataID
     * @return dictdata
     */
    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return baseMapper.selectById(dictCode);
    }

    /**
     * Batch delete dictdatainfo
     *
     * @param dictCodes need to Delete dictdataID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes) {
        for (Long dictCode : dictCodes) {
            SysDictData data = selectDictDataById(dictCode);
            baseMapper.deleteById(dictCode);
            CacheUtils.evict(CacheNames.SYS_DICT, data.getDictType());
        }
    }

    /**
     * Add dictdatainfo
     *
     * @param data dictdatainfo
     * @return
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#data.dictType")
    @Override
    public List<SysDictData> insertDictData(SysDictData data) {
        int row = baseMapper.insert(data);
        if (row > 0) {
            return baseMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * Update dictdatainfo
     *
     * @param data dictdatainfo
     * @return
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#data.dictType")
    @Override
    public List<SysDictData> updateDictData(SysDictData data) {
        int row = baseMapper.updateById(data);
        if (row > 0) {
            return baseMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

}
