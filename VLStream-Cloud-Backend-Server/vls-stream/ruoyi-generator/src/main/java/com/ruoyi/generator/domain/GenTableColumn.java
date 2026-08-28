/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.generator.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import javax.validation.constraints.NotBlank;

/**
 * Generate field gen_table_column
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gen_table_column")
public class GenTableColumn extends BaseEntity {

    /**
     *
     */
    @TableId(value = "column_id")
    private Long columnId;

    /**
     *
     */
    private Long tableId;

    /**
     *
     */
    private String columnName;

    /**
     *
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String columnComment;

    /**
     *
     */
    private String columnType;

    /**
     * JAVA
     */
    private String javaType;

    /**
     * JAVAfield
     */
    @NotBlank(message = "Java属性不能为空")
    private String javaField;

    /**
     * whether primary key (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isPk;

    /**
     * whether (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isIncrement;

    /**
     * whether (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isRequired;

    /**
     * whether to field (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isInsert;

    /**
     * whether field (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isEdit;

    /**
     * whether field (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isList;

    /**
     * whether Query field (1 is )
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String isQuery;

    /**
     * Query (EQ etc. 、NE etc. 、GT 、LT 、LIKE 、BETWEEN )
     */
    private String queryType;

    /**
     * (input 、textarea 、select 、checkbox 、radio 、datetime 、image 、upload 、editor )
     */
    private String htmlType;

    /**
     * dict type
     */
    private String dictType;

    /**
     *
     */
    private Integer sort;

    public String getCapJavaField() {
        return StringUtils.capitalize(javaField);
    }

    public boolean isPk() {
        return isPk(this.isPk);
    }

    public boolean isPk(String isPk) {
        return isPk != null && StringUtils.equals("1", isPk);
    }

    public boolean isIncrement() {
        return isIncrement(this.isIncrement);
    }

    public boolean isIncrement(String isIncrement) {
        return isIncrement != null && StringUtils.equals("1", isIncrement);
    }

    public boolean isRequired() {
        return isRequired(this.isRequired);
    }

    public boolean isRequired(String isRequired) {
        return isRequired != null && StringUtils.equals("1", isRequired);
    }

    public boolean isInsert() {
        return isInsert(this.isInsert);
    }

    public boolean isInsert(String isInsert) {
        return isInsert != null && StringUtils.equals("1", isInsert);
    }

    public boolean isEdit() {
        return isInsert(this.isEdit);
    }

    public boolean isEdit(String isEdit) {
        return isEdit != null && StringUtils.equals("1", isEdit);
    }

    public boolean isList() {
        return isList(this.isList);
    }

    public boolean isList(String isList) {
        return isList != null && StringUtils.equals("1", isList);
    }

    public boolean isQuery() {
        return isQuery(this.isQuery);
    }

    public boolean isQuery(String isQuery) {
        return isQuery != null && StringUtils.equals("1", isQuery);
    }

    public boolean isSuperColumn() {
        return isSuperColumn(this.javaField);
    }

    public static boolean isSuperColumn(String javaField) {
        return StringUtils.equalsAnyIgnoreCase(javaField,
            // BaseEntity
            "createBy", "createTime", "updateBy", "updateTime",
            // TreeEntity
            "parentName", "parentId");
    }

    public boolean isUsableColumn() {
        return isUsableColumn(javaField);
    }

    public static boolean isUsableColumn(String javaField) {
        // isSuperColumn() in Generate Domainproperty, property in Generate page need to can , in
        return StringUtils.equalsAnyIgnoreCase(javaField, "parentId", "orderNum", "remark");
    }

    public String readConverterExp() {
        String remarks = StringUtils.substringBetween(this.columnComment, "（", "）");
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotEmpty(remarks)) {
            for (String value : remarks.split(" ")) {
                if (StringUtils.isNotEmpty(value)) {
                    Object startStr = value.subSequence(0, 1);
                    String endStr = value.substring(1);
                    sb.append(StringUtils.EMPTY).append(startStr).append("=").append(endStr).append(StringUtils.SEPARATOR);
                }
            }
            return sb.deleteCharAt(sb.length() - 1).toString();
        } else {
            return this.columnComment;
        }
    }
}
