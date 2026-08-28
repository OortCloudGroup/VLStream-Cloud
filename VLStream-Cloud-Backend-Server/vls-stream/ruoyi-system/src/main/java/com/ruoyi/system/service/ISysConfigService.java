/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysConfig;

import java.util.List;

/**
 * parameterconfiguration service layer
 *
 * @author Lion Li
 */
public interface ISysConfigService {


    TableDataInfo<SysConfig> selectPageConfigList(SysConfig config, PageQuery pageQuery);

    /**
     * Query parameterconfigurationinfo
     *
     * @param configId parameterconfigurationID
     * @return parameterconfigurationinfo
     */
    SysConfig selectConfigById(Long configId);

    /**
     * Query parameterconfigurationinfo
     *
     * @param configKey parameter key
     * @return parameter value
     */
    String selectConfigByKey(String configKey);

    /**
     * Get
     *
     * @return true , false
     */
    boolean selectCaptchaEnabled();

    /**
     * Query parameterconfiguration list
     *
     * @param config parameterconfigurationinfo
     * @return parameterconfigurationcollection
     */
    List<SysConfig> selectConfigList(SysConfig config);

    /**
     * Add parameterconfiguration
     *
     * @param config parameterconfigurationinfo
     * @return
     */
    String insertConfig(SysConfig config);

    /**
     * Update parameterconfiguration
     *
     * @param config parameterconfigurationinfo
     * @return
     */
    String updateConfig(SysConfig config);

    /**
     * Batch delete parameterinfo
     *
     * @param configIds need to Delete parameterID
     */
    void deleteConfigByIds(Long[] configIds);

    /**
     * Load parameter data
     */
    void loadingConfigCache();

    /**
     * null / empty parameter data
     */
    void clearConfigCache();

    /**
     * parameter data
     */
    void resetConfigCache();

    /**
     * Validate parameter keywhether
     *
     * @param config parameterinfo
     * @return
     */
    boolean checkConfigKeyUnique(SysConfig config);

}
