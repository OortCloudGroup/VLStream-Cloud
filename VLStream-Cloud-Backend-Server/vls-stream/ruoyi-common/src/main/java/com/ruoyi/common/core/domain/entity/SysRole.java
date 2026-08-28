/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.entity;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * role sys_role
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@ExcelIgnoreUnannotated
public class SysRole extends BaseEntity {

    /**
     * tenant ID.
     */
    private String tenantId;

    /**
     * role ID
     */
    @ExcelProperty(value = "角色序号")
    @TableId(value = "role_id")
    private String  roleId;

    /**
     * role name
     */
    @ExcelProperty(value = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 0, max = 30, message = "角色名称长度不能超过{max}个字符")
    private String roleName;

    /**
     * role
     */
    @ExcelProperty(value = "角色权限")
    @NotBlank(message = "权限字符不能为空")
    @Size(min = 0, max = 100, message = "权限字符长度不能超过{max}个字符")
    private String roleKey;

    /**
     * role
     */
    @ExcelProperty(value = "角色排序")
    @NotNull(message = "显示顺序不能为空")
    private Integer roleSort;

    /**
     * data (1: all data ; 2: Customdata ; 3: departmentdata ; 4: department data ; 5: data )
     */
    @ExcelProperty(value = "数据范围", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=所有数据权限,2=自定义数据权限,3=本部门数据权限,4=本部门及以下数据权限,5=仅本人数据权限")
    private String dataScope;

    /**
     * menu item whether ( 0: sub related 1: sub related )
     */
    private Boolean menuCheckStrictly;

    /**
     * department item whether (0: sub related 1: sub related )
     */
    private Boolean deptCheckStrictly;

    /**
     * roleStatus (0 normal 1 disabled)
     */
    @ExcelProperty(value = "角色状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * Delete (0represents in 2represents Delete )
     */
    @TableLogic
    private String delFlag;

    /**
     * remark
     */
    private String remark;

    /**
     * userwhether in role in
     */
    @TableField(exist = false)
    private boolean flag = false;

    /**
     * menu
     */
    @TableField(exist = false)
    private Long[] menuIds;

    /**
     * department (data )
     */
    @TableField(exist = false)
    private String[] deptIds;

    public SysRole(String  roleId) {
        this.roleId = roleId;
    }

    public boolean isAdmin() {
        return "1".equals(this.roleId);
    }
}
