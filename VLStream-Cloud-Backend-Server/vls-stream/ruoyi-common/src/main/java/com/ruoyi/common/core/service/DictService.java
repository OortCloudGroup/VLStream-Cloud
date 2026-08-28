/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.service;

/**
 * dictservice
 *
 * @author Lion Li
 */
public interface DictService {

    /**
     *
     */
    String SEPARATOR = ",";

    /**
     * dict type and dict value Get dict
     *
     * @param dictType dict type
     * @param dictValue dict value
     * @return dict
     */
    default String getDictLabel(String dictType, String dictValue) {
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * dict type and dict Get dict value
     *
     * @param dictType dict type
     * @param dictLabel dict
     * @return dict value
     */
    default String getDictValue(String dictType, String dictLabel) {
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * dict type and dict value Get dict
     *
     * @param dictType dict type
     * @param dictValue dict value
     * @param separator
     * @return dict
     */
    String getDictLabel(String dictType, String dictValue, String separator);

    /**
     * dict type and dict Get dict value
     *
     * @param dictType dict type
     * @param dictLabel dict
     * @param separator
     * @return dict value
     */
    String getDictValue(String dictType, String dictLabel, String separator);

}
