/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain;

import com.ruoyi.common.utils.StringUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * info
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class SysCache {

    /**
     *
     */
    private String cacheName = "";

    /**
     *
     */
    private String cacheKey = "";

    /**
     *
     */
    private String cacheValue = "";

    /**
     * remark
     */
    private String remark = "";

    public SysCache(String cacheName, String remark) {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    public SysCache(String cacheName, String cacheKey, String cacheValue) {
        this.cacheName = StringUtils.replace(cacheName, ":", "");
        this.cacheKey = StringUtils.replace(cacheKey, cacheName, "");
        this.cacheValue = cacheValue;
    }

}
