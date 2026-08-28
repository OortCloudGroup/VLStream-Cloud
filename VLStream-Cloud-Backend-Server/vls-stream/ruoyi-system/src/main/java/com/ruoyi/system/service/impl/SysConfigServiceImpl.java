/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.ConfigService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.CacheUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * parameterconfiguration service layer
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysConfigServiceImpl implements ISysConfigService, ConfigService {

    private final SysConfigMapper baseMapper;

    @Override
    public TableDataInfo<SysConfig> selectPageConfigList(SysConfig config, PageQuery pageQuery) {
        Map<String, Object> params = config.getParams();
        LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<SysConfig>()
            .like(StringUtils.isNotBlank(config.getConfigName()), SysConfig::getConfigName, config.getConfigName())
            .eq(StringUtils.isNotBlank(config.getConfigType()), SysConfig::getConfigType, config.getConfigType())
            .like(StringUtils.isNotBlank(config.getConfigKey()), SysConfig::getConfigKey, config.getConfigKey())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysConfig::getCreateTime, params.get("beginTime"), params.get("endTime"));
        Page<SysConfig> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * Query parameterconfigurationinfo
     *
     * @param configId parameterconfigurationID
     * @return parameterconfigurationinfo
     */
    @Override
    @DS("master")
    public SysConfig selectConfigById(Long configId) {
        return baseMapper.selectById(configId);
    }

    /**
     * Query parameterconfigurationinfo
     *
     * @param configKey parameterkey
     * @return parameter value
     */
    @Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "#configKey")
    @Override
    public String selectConfigByKey(String configKey) {
        SysConfig retConfig = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getConfigKey, configKey));
        if (ObjectUtil.isNotNull(retConfig)) {
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Get
     *
     * @return true , false
     */
    @Override
    public boolean selectCaptchaEnabled() {
        return false;
//        String captchaEnabled = SpringUtils.getAopProxy(this).selectConfigByKey("sys.account.captchaEnabled");
//        if (StringUtils.isEmpty(captchaEnabled)) {
//            return true;
//        }
//        return Convert.toBool(captchaEnabled);
    }

    /**
     * Query parameterconfiguration list
     *
     * @param config parameterconfigurationinfo
     * @return parameterconfigurationcollection
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config) {
        Map<String, Object> params = config.getParams();
        LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<SysConfig>()
            .like(StringUtils.isNotBlank(config.getConfigName()), SysConfig::getConfigName, config.getConfigName())
            .eq(StringUtils.isNotBlank(config.getConfigType()), SysConfig::getConfigType, config.getConfigType())
            .like(StringUtils.isNotBlank(config.getConfigKey()), SysConfig::getConfigKey, config.getConfigKey())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                SysConfig::getCreateTime, params.get("beginTime"), params.get("endTime"));
        return baseMapper.selectList(lqw);
    }

    /**
     * Add parameterconfiguration
     *
     * @param config parameterconfigurationinfo
     * @return
     */
    @CachePut(cacheNames = CacheNames.SYS_CONFIG, key = "#config.configKey")
    @Override
    public String insertConfig(SysConfig config) {
        int row = baseMapper.insert(config);
        if (row > 0) {
            return config.getConfigValue();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * Update parameterconfiguration
     *
     * @param config parameterconfigurationinfo
     * @return
     */
    @CachePut(cacheNames = CacheNames.SYS_CONFIG, key = "#config.configKey")
    @Override
    public String updateConfig(SysConfig config) {
        int row = 0;
        if (config.getConfigId() != null) {
            SysConfig temp = baseMapper.selectById(config.getConfigId());
            if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey())) {
                CacheUtils.evict(CacheNames.SYS_CONFIG, temp.getConfigKey());
            }
            row = baseMapper.updateById(config);
        } else {
            row = baseMapper.update(config, new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, config.getConfigKey()));
        }
        if (row > 0) {
            return config.getConfigValue();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * Batch delete parameterinfo
     *
     * @param configIds need to Delete parameterID
     */
    @Override
    public void deleteConfigByIds(Long[] configIds) {
        for (Long configId : configIds) {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType())) {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            CacheUtils.evict(CacheNames.SYS_CONFIG, config.getConfigKey());
        }
        baseMapper.deleteBatchIds(Arrays.asList(configIds));
    }

    /**
     * Load parameter data
     */
    @Override
    public void loadingConfigCache() {
        List<SysConfig> configsList = selectConfigList(new SysConfig());
        configsList.forEach(config ->
            CacheUtils.put(CacheNames.SYS_CONFIG, config.getConfigKey(), config.getConfigValue()));
    }

    /**
     * null / empty parameter data
     */
    @Override
    public void clearConfigCache() {
        CacheUtils.clear(CacheNames.SYS_CONFIG);
    }

    /**
     * parameter data
     */
    @Override
    public void resetConfigCache() {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * Validate parameter keywhether
     *
     * @param config parameterconfigurationinfo
     * @return
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config) {
        long configId = ObjectUtil.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, config.getConfigKey()));
        if (ObjectUtil.isNotNull(info) && info.getConfigId() != configId) {
            return false;
        }
        return true;
    }

    /**
     * parameter key Get parameter value
     *
     * @param configKey parameter key
     * @return parameter value
     */
    @Override
    public String getConfigValue(String configKey) {
        return SpringUtils.getAopProxy(this).selectConfigByKey(configKey);
    }

}
