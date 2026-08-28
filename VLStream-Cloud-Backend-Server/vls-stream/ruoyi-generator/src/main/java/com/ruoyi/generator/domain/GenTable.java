/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.generator.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ArrayUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * gen_table
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_table")
public class GenTable extends BaseEntity {

    /**
     *
     */
    @TableId(value = "table_id")
    private Long tableId;

    /**
     *
     */
    @NotBlank(message = "表名称不能为空")
    private String tableName;

    /**
     *
     */
    @NotBlank(message = "表描述不能为空")
    private String tableComment;

    /**
     *
     */
    private String subTableName;

    /**
     *
     */
    private String subTableFkName;

    /**
     * ( )
     */
    @NotBlank(message = "实体类名称不能为空")
    private String className;

    /**
     * (crud operation tree operation sub main sub operation)
     */
    private String tplCategory;

    /**
     * Generate
     */
    @NotBlank(message = "生成包路径不能为空")
    private String packageName;

    /**
     * Generate
     */
    @NotBlank(message = "生成模块名不能为空")
    private String moduleName;

    /**
     * Generate
     */
    @NotBlank(message = "生成业务名不能为空")
    private String businessName;

    /**
     * Generate can
     */
    @NotBlank(message = "生成功能名不能为空")
    private String functionName;

    /**
     * Generate
     */
    @NotBlank(message = "作者不能为空")
    private String functionAuthor;

    /**
     * Generate (0zip 1Custom )
     */
    private String genType;

    /**
     * Generate ( item )
     */
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String genPath;

    /**
     * primary keyinfo
     */
    @TableField(exist = false)
    private GenTableColumn pkColumn;

    /**
     * sub info
     */
    @TableField(exist = false)
    private GenTable subTable;

    /**
     * info
     */
    @Valid
    @TableField(exist = false)
    private List<GenTableColumn> columns;

    /**
     * Generate item
     */
    private String options;

    /**
     * remark
     */
    private String remark;

    /**
     * field
     */
    @TableField(exist = false)
    private String treeCode;

    /**
     * field
     */
    @TableField(exist = false)
    private String treeParentCode;

    /**
     * field
     */
    @TableField(exist = false)
    private String treeName;

    /*
     * menuid
     */
    @TableField(exist = false)
    private List<Long> menuIds;

    /**
     * menu IDfield
     */
    @TableField(exist = false)
    private String parentMenuId;

    /**
     * menu namefield
     */
    @TableField(exist = false)
    private String parentMenuName;

    public boolean isSub() {
        return isSub(this.tplCategory);
    }

    public static boolean isSub(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_SUB, tplCategory);
    }

    public boolean isTree() {
        return isTree(this.tplCategory);
    }

    public static boolean isTree(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_TREE, tplCategory);
    }

    public boolean isCrud() {
        return isCrud(this.tplCategory);
    }

    public static boolean isCrud(String tplCategory) {
        return tplCategory != null && StringUtils.equals(GenConstants.TPL_CRUD, tplCategory);
    }

    public boolean isSuperColumn(String javaField) {
        return isSuperColumn(this.tplCategory, javaField);
    }

    public static boolean isSuperColumn(String tplCategory, String javaField) {
        if (isTree(tplCategory)) {
            return StringUtils.equalsAnyIgnoreCase(javaField,
                ArrayUtils.addAll(GenConstants.TREE_ENTITY, GenConstants.BASE_ENTITY));
        }
        return StringUtils.equalsAnyIgnoreCase(javaField, GenConstants.BASE_ENTITY);
    }
}
