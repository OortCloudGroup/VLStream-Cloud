/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.generator.util;

import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.generator.config.GenConfig;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RegExUtils;

import java.util.Arrays;

/**
 * Generate
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenUtils {

    /**
     * Initialize info
     */
    public static void initTable(GenTable genTable, String operName) {
        genTable.setClassName(convertClassName(genTable.getTableName()));
        genTable.setPackageName(GenConfig.getPackageName());
        genTable.setModuleName(getModuleName(GenConfig.getPackageName()));
        genTable.setBusinessName(getBusinessName(genTable.getTableName()));
        genTable.setFunctionName(replaceText(genTable.getTableComment()));
        genTable.setFunctionAuthor(GenConfig.getAuthor());
        genTable.setCreateBy(operName);
    }

    /**
     * Initialize propertyfield
     */
    public static void initColumnField(GenTableColumn column, GenTable table) {
        String dataType = getDbType(column.getColumnType());
        String columnName = column.getColumnName();
        column.setTableId(table.getTableId());
        column.setCreateBy(table.getCreateBy());
        // Set javafield
        column.setJavaField(StringUtils.toCamelCase(columnName));
        // Set
        column.setJavaType(GenConstants.TYPE_STRING);
        column.setQueryType(GenConstants.QUERY_EQ);

        if (arraysContains(GenConstants.COLUMNTYPE_STR, dataType) || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType)) {
            // 500Set to
            Integer columnLength = getColumnLength(column.getColumnType());
            String htmlType = columnLength >= 500 || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType) ? GenConstants.HTML_TEXTAREA : GenConstants.HTML_INPUT;
            column.setHtmlType(htmlType);
        } else if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType)) {
            column.setJavaType(GenConstants.TYPE_DATE);
            column.setHtmlType(GenConstants.HTML_DATETIME);
        } else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType)) {
            column.setHtmlType(GenConstants.HTML_INPUT);

            // if is BigDecimal
            String[] str = StringUtils.split(StringUtils.substringBetween(column.getColumnType(), "(", ")"), StringUtils.SEPARATOR);
            if (str != null && str.length == 2 && Integer.parseInt(str[1]) > 0) {
                column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
            }
            // if is
            else if (str != null && str.length == 1 && Integer.parseInt(str[0]) <= 10) {
                column.setJavaType(GenConstants.TYPE_INTEGER);
            }
            //
            else {
                column.setJavaType(GenConstants.TYPE_LONG);
            }
        }

        // BOobject
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_ADD, columnName) && !column.isPk()) {
            column.setIsInsert(GenConstants.REQUIRE);
        }
        // BOobject
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName)) {
            column.setIsEdit(GenConstants.REQUIRE);
        }
        // BOobject whether
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName)) {
            column.setIsRequired(GenConstants.REQUIRE);
        }
        // VOobject
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_LIST, columnName)) {
            column.setIsList(GenConstants.REQUIRE);
        }
        // BOobject Query
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_QUERY, columnName) && !column.isPk()) {
            column.setIsQuery(GenConstants.REQUIRE);
        }

        // Query field
        if (StringUtils.endsWithIgnoreCase(columnName, "name")) {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        // fieldSet
        if (StringUtils.endsWithIgnoreCase(columnName, "status")) {
            column.setHtmlType(GenConstants.HTML_RADIO);
        }
        // & fieldSet
        else if (StringUtils.endsWithIgnoreCase(columnName, "type")
            || StringUtils.endsWithIgnoreCase(columnName, "sex")) {
            column.setHtmlType(GenConstants.HTML_SELECT);
        }
        // fieldSet
        else if (StringUtils.endsWithIgnoreCase(columnName, "image")) {
            column.setHtmlType(GenConstants.HTML_IMAGE_UPLOAD);
        }
        // fieldSet
        else if (StringUtils.endsWithIgnoreCase(columnName, "file")) {
            column.setHtmlType(GenConstants.HTML_FILE_UPLOAD);
        }
        // fieldSet
        else if (StringUtils.endsWithIgnoreCase(columnName, "content")) {
            column.setHtmlType(GenConstants.HTML_EDITOR);
        }
    }

    /**
     * Validate arraywhether value
     *
     * @param arr array
     * @param targetValue value
     * @return whether
     */
    public static boolean arraysContains(String[] arr, String targetValue) {
        return Arrays.asList(arr).contains(targetValue);
    }

    /**
     * Get
     *
     * @param packageName
     * @return
     */
    public static String getModuleName(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        int nameLength = packageName.length();
        return StringUtils.substring(packageName, lastIndex + 1, nameLength);
    }

    /**
     * Get
     *
     * @param tableName
     * @return
     */
    public static String getBusinessName(String tableName) {
        int firstIndex = tableName.indexOf("_");
        int nameLength = tableName.length();
        String businessName = StringUtils.substring(tableName, firstIndex + 1, nameLength);
        businessName = StringUtils.toCamelCase(businessName);
        return businessName;
    }

    /**
     * Convert Java
     *
     * @param tableName
     * @return
     */
    public static String convertClassName(String tableName) {
        boolean autoRemovePre = GenConfig.getAutoRemovePre();
        String tablePrefix = GenConfig.getTablePrefix();
        if (autoRemovePre && StringUtils.isNotEmpty(tablePrefix)) {
            String[] searchList = StringUtils.split(tablePrefix, StringUtils.SEPARATOR);
            tableName = replaceFirst(tableName, searchList);
        }
        return StringUtils.convertToCamelCase(tableName);
    }

    /**
     * Replace before
     *
     * @param replacementm Replace value
     * @param searchList Replace
     * @return
     */
    public static String replaceFirst(String replacementm, String[] searchList) {
        String text = replacementm;
        for (String searchString : searchList) {
            if (replacementm.startsWith(searchString)) {
                text = replacementm.replaceFirst(searchString, StringUtils.EMPTY);
                break;
            }
        }
        return text;
    }

    /**
     * Replace
     *
     * @param text need to Replace
     * @return Replace after
     */
    public static String replaceText(String text) {
        return RegExUtils.replaceAll(text, "(?:表|若依)", "");
    }

    /**
     * Get data field
     *
     * @param columnType
     * @return after
     */
    public static String getDbType(String columnType) {
        if (StringUtils.indexOf(columnType, '(') > 0) {
            return StringUtils.substringBefore(columnType, "(");
        } else {
            return columnType;
        }
    }

    /**
     * Get field
     *
     * @param columnType
     * @return after
     */
    public static Integer getColumnLength(String columnType) {
        if (StringUtils.indexOf(columnType, '(') > 0) {
            String length = StringUtils.substringBetween(columnType, "(", ")");
            return Integer.valueOf(length);
        } else {
            return 0;
        }
    }
}
