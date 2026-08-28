/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import cn.dev33.satoken.context.SaHolder;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.entity.SysDictType;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.DictService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.CacheUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.mapper.SysDictTypeMapper;
import com.ruoyi.system.service.ISysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * dict layer Process
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService, DictService {

    private final SysDictTypeMapper baseMapper;
    private final SysDictDataMapper dictDataMapper;

    @Override
    public TableDataInfo<SysDictType> selectPageDictTypeList(SysDictType dictType, PageQuery pageQuery) {
        Map<String, Object> params = dictType.getParams();
        LambdaQueryWrapper<SysDictType> lqw = new LambdaQueryWrapper<SysDictType>()
            .like(StringUtils.isNotBlank(dictType.getDictName()), SysDictType::getDictName, dictType.getDictName())
            .eq(StringUtils.isNotBlank(dictType.getStatus()), SysDictType::getStatus, dictType.getStatus())
            .like(StringUtils.isNotBlank(dictType.getDictType()), SysDictType::getDictType, dictType.getDictType())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysDictType::getCreateTime, params.get("beginTime"), params.get("endTime"));
        Page<SysDictType> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * Query dict type
     *
     * @param dictType dict typeinfo
     * @return dict typecollectioninfo
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        Map<String, Object> params = dictType.getParams();
        return baseMapper.selectList(new LambdaQueryWrapper<SysDictType>()
            .like(StringUtils.isNotBlank(dictType.getDictName()), SysDictType::getDictName, dictType.getDictName())
            .eq(StringUtils.isNotBlank(dictType.getStatus()), SysDictType::getStatus, dictType.getStatus())
            .like(StringUtils.isNotBlank(dictType.getDictType()), SysDictType::getDictType, dictType.getDictType())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysDictType::getCreateTime, params.get("beginTime"), params.get("endTime")));
    }

    /**
     * all dict type
     *
     * @return dict typecollectioninfo
     */
    @Override
    public List<SysDictType> selectDictTypeAll() {
        return baseMapper.selectList();
    }

    /**
     * dict typeQuery dictdata
     *
     * @param dictType dict type
     * @return dictdataset info
     */
//    @Cacheable(cacheNames = CacheNames.SYS_DICT, key = "#dictType",unless = "#result == null")
    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dictType);
        if (CollUtil.isNotEmpty(dictDatas)) {
            return dictDatas;
        }
        return null;
    }

    /**
     * dict typeIDQuery info
     *
     * @param dictId dict typeID
     * @return dict type
     */
    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return baseMapper.selectById(dictId);
    }

    /**
     * dict typeQuery info
     *
     * @param dictType dict type
     * @return dict type
     */
    @Cacheable(cacheNames = CacheNames.SYS_DICT, key = "#dictType")
    @Override
    public SysDictType selectDictTypeByType(String dictType) {
        return baseMapper.selectById(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
    }

    /**
     * Batch delete dict typeinfo
     *
     * @param dictIds need to Delete dictID
     */
    @Override
    public void deleteDictTypeByIds(Long[] dictIds) {
        for (Long dictId : dictIds) {
            SysDictType dictType = selectDictTypeById(dictId);
            if (dictDataMapper.exists(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType.getDictType()))) {
                throw new ServiceException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            CacheUtils.evict(CacheNames.SYS_DICT, dictType.getDictType());
        }
        baseMapper.deleteBatchIds(Arrays.asList(dictIds));
    }

    /**
     * Load dict data
     */
    @Override
    public void loadingDictCache() {
        List<SysDictData> dictDataList = dictDataMapper.selectList(
            new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getStatus, UserConstants.DICT_NORMAL));
        Map<String, List<SysDictData>> dictDataMap = StreamUtils.groupByKey(dictDataList, SysDictData::getDictType);
        dictDataMap.forEach((k,v) -> {
            List<SysDictData> dictList = StreamUtils.sorted(v, Comparator.comparing(SysDictData::getDictSort));
            CacheUtils.put(CacheNames.SYS_DICT, k, dictList);
        });
    }

    /**
     * null / empty dict data
     */
    @Override
    public void clearDictCache() {
        CacheUtils.clear(CacheNames.SYS_DICT);
    }

    /**
     * dict data
     */
    @Override
    public void resetDictCache() {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * Add dict typeinfo
     *
     * @param dict dict typeinfo
     * @return
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#dict.dictType")
    @Override
    public List<SysDictData> insertDictType(SysDictType dict) {
        int row = baseMapper.insert(dict);
        if (row > 0) {
            return new ArrayList<>();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * Update dict typeinfo
     *
     * @param dict dict typeinfo
     * @return
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#dict.dictType")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysDictData> updateDictType(SysDictType dict) {
        SysDictType oldDict = baseMapper.selectById(dict.getDictId());
        dictDataMapper.update(null, new LambdaUpdateWrapper<SysDictData>()
            .set(SysDictData::getDictType, dict.getDictType())
            .eq(SysDictData::getDictType, oldDict.getDictType()));
        int row = baseMapper.updateById(dict);
        if (row > 0) {
            CacheUtils.evict(CacheNames.SYS_DICT, oldDict.getDictType());
            return dictDataMapper.selectDictDataByType(dict.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * Validate dict type whether
     *
     * @param dict dict type
     * @return
     */
    @Override
    public boolean checkDictTypeUnique(SysDictType dict) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysDictType>()
            .eq(SysDictType::getDictType, dict.getDictType())
            .ne(ObjectUtil.isNotNull(dict.getDictId()), SysDictType::getDictId, dict.getDictId()));
        return !exist;
    }

    /**
     * dict type and dict value Get dict
     *
     * @param dictType dict type
     * @param dictValue dict value
     * @param separator
     * @return dict
     */
    @SuppressWarnings("unchecked cast")
    @Override
    public String getDictLabel(String dictType, String dictValue, String separator) {
        // from Get
        List<SysDictData> datas = (List<SysDictData>) SaHolder.getStorage().get(CacheConstants.SYS_DICT_KEY + dictType);
        if (ObjectUtil.isNull(datas)) {
            datas = SpringUtils.getAopProxy(this).selectDictDataByType(dictType);
            SaHolder.getStorage().set(CacheConstants.SYS_DICT_KEY + dictType, datas);
        }

        Map<String, String> map = StreamUtils.toMap(datas, SysDictData::getDictValue, SysDictData::getDictLabel);
        if (StringUtils.containsAny(dictValue, separator)) {
            return Arrays.stream(dictValue.split(separator))
                .map(v -> map.getOrDefault(v, StringUtils.EMPTY))
                .collect(Collectors.joining(separator));
        } else {
            return map.getOrDefault(dictValue, StringUtils.EMPTY);
        }
    }

    /**
     * dict type and dict Get dict value
     *
     * @param dictType dict type
     * @param dictLabel dict
     * @param separator
     * @return dict value
     */
    @SuppressWarnings("unchecked cast")
    @Override
    public String getDictValue(String dictType, String dictLabel, String separator) {
        // from Get
        List<SysDictData> datas = (List<SysDictData>) SaHolder.getStorage().get(CacheConstants.SYS_DICT_KEY + dictType);
        if (ObjectUtil.isNull(datas)) {
            datas = SpringUtils.getAopProxy(this).selectDictDataByType(dictType);
            SaHolder.getStorage().set(CacheConstants.SYS_DICT_KEY + dictType, datas);
        }

        Map<String, String> map = StreamUtils.toMap(datas, SysDictData::getDictLabel, SysDictData::getDictValue);
        if (StringUtils.containsAny(dictLabel, separator)) {
            return Arrays.stream(dictLabel.split(separator))
                .map(l -> map.getOrDefault(l, StringUtils.EMPTY))
                .collect(Collectors.joining(separator));
        } else {
            return map.getOrDefault(dictLabel, StringUtils.EMPTY);
        }
    }

}
