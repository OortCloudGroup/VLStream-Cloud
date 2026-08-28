/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package org.springblade.core.tool.utils;

import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * Spring bean lookup compatibility helper.
 */
public final class SpringUtil {

    private SpringUtil() {
    }

    /**
     * Resolves a bean from RuoYi's shared Spring application context.
     */
    public static <T> T getBean(Class<T> beanType) {
        return SpringUtils.getBean(beanType);
    }
}
