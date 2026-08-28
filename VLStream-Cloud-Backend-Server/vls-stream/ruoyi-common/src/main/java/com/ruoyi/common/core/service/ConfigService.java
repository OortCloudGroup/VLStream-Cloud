/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.service;

/**
 * parameterconfigurationservice
 *
 * @author Lion Li
 */
public interface ConfigService {

    /**
     * parameter key Get parameter value
     *
     * @param configKey parameter key
     * @return parameter value
     */
    String getConfigValue(String configKey);

}
