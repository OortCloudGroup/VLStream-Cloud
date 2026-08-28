/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.translation;

/**
 * interface ( annotation {@link com.ruoyi.common.annotation.TranslationType} )
 *
 * @author Lion Li
 */
public interface TranslationInterface<T> {

    /**
     *
     *
     * @param key need to ( is empty)
     * @return value
     */
    T translation(Object key, String other);
}
