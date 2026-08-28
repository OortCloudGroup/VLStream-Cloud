/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package org.springblade.core.excel.util;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Excel export compatibility helper backed by RuoYi's EasyExcel integration.
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * Writes the supplied rows to the HTTP response using the requested filename.
     */
    public static <T> void export(HttpServletResponse response, String fileName, String sheetName,
                                  List<T> rows, Class<T> rowType) {
        com.ruoyi.common.utils.poi.ExcelUtil.exportExcel(rows, fileName, rowType, response);
    }
}
