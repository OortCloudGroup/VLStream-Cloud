/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.utils.ip;

import cn.hutool.core.net.NetUtil;
import cn.hutool.http.HtmlUtil;
import com.ruoyi.common.utils.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Get
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressUtils {

    // not
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        if (StringUtils.isBlank(ip)) {
            return UNKNOWN;
        }
        // Query
        ip = "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : HtmlUtil.cleanHtmlTag(ip);
        if (NetUtil.isInnerIP(ip)) {
            return "内网IP";
        }
        return RegionUtils.getCityInfo(ip);
    }
}
