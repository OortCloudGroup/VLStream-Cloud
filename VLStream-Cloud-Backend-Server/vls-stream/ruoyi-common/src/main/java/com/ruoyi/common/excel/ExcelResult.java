/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.excel;

import java.util.List;

/**
 * excel object
 *
 * @author Lion Li
 */
public interface ExcelResult<T> {

    /**
     * object
     */
    List<T> getList();

    /**
     *
     */
    List<String> getErrorList();

    /**
     * Import
     */
    String getAnalysis();
}
