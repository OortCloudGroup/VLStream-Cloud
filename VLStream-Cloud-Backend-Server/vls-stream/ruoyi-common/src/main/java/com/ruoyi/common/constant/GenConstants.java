/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.constant;

/**
 * Generate
 *
 * @author ruoyi
 */
public interface GenConstants {
    /**
     * ( )
     */
    String TPL_CRUD = "crud";

    /**
     * ( )
     */
    String TPL_TREE = "tree";

    /**
     * main sub ( )
     */
    String TPL_SUB = "sub";

    /**
     * field
     */
    String TREE_CODE = "treeCode";

    /**
     * field
     */
    String TREE_PARENT_CODE = "treeParentCode";

    /**
     * field
     */
    String TREE_NAME = "treeName";

    /**
     * menu IDfield
     */
    String PARENT_MENU_ID = "parentMenuId";

    /**
     * menu namefield
     */
    String PARENT_MENU_NAME = "parentMenuName";

    /**
     * data
     */
    String[] COLUMNTYPE_STR = {"char", "varchar", "nvarchar", "varchar2"};

    /**
     * data
     */
    String[] COLUMNTYPE_TEXT = {"tinytext", "text", "mediumtext", "longtext"};

    /**
     * data
     */
    String[] COLUMNTYPE_TIME = {"datetime", "time", "date", "timestamp"};

    /**
     * data
     */
    String[] COLUMNTYPE_NUMBER = {"tinyint", "smallint", "mediumint", "int", "number", "integer",
        "bit", "bigint", "float", "double", "decimal"};

    /**
     * BOobject need to field
     */
    String[] COLUMNNAME_NOT_ADD = {"create_by", "create_time", "del_flag", "update_by",
        "update_time", "version"};

    /**
     * BOobject need to field
     */
    String[] COLUMNNAME_NOT_EDIT = {"create_by", "create_time", "del_flag", "update_by",
        "update_time", "version"};

    /**
     * VOobject need to field
     */
    String[] COLUMNNAME_NOT_LIST = {"create_by", "create_time", "del_flag", "update_by",
        "update_time", "version"};

    /**
     * BOobject need to Query field
     */
    String[] COLUMNNAME_NOT_QUERY = {"id", "create_by", "create_time", "del_flag", "update_by",
        "update_time", "remark", "version"};

    /**
     * Entity field
     */
    String[] BASE_ENTITY = {"createBy", "createTime", "updateBy", "updateTime"};

    /**
     * Tree field
     */
    String[] TREE_ENTITY = {"parentName", "parentId", "children"};

    /**
     *
     */
    String HTML_INPUT = "input";

    /**
     *
     */
    String HTML_TEXTAREA = "textarea";

    /**
     *
     */
    String HTML_SELECT = "select";

    /**
     *
     */
    String HTML_RADIO = "radio";

    /**
     *
     */
    String HTML_CHECKBOX = "checkbox";

    /**
     *
     */
    String HTML_DATETIME = "datetime";

    /**
     *
     */
    String HTML_IMAGE_UPLOAD = "imageUpload";

    /**
     *
     */
    String HTML_FILE_UPLOAD = "fileUpload";

    /**
     *
     */
    String HTML_EDITOR = "editor";

    /**
     *
     */
    String TYPE_STRING = "String";

    /**
     *
     */
    String TYPE_INTEGER = "Integer";

    /**
     *
     */
    String TYPE_LONG = "Long";

    /**
     *
     */
    String TYPE_DOUBLE = "Double";

    /**
     *
     */
    String TYPE_BIGDECIMAL = "BigDecimal";

    /**
     *
     */
    String TYPE_DATE = "Date";

    /**
     * Query
     */
    String QUERY_LIKE = "LIKE";

    /**
     * etc.Query
     */
    String QUERY_EQ = "EQ";

    /**
     * need to
     */
    String REQUIRE = "1";
}
