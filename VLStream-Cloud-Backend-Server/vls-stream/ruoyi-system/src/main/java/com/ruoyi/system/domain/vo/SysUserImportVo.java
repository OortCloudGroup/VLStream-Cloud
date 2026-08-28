/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * userobjectImport VO
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
// @Accessors(chain = true) // Import will set method
public class SysUserImportVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户序号")
    private Long userId;

    /**
     * department ID
     */
    @ExcelProperty(value = "部门编号")
    private String  deptId;

    /**
     * user
     */
    @ExcelProperty(value = "登录名称")
    private String userName;

    /**
     * user
     */
    @ExcelProperty(value = "用户名称")
    private String nickName;

    /**
     * user
     */
    @ExcelProperty(value = "用户邮箱")
    private String email;

    /**
     *
     */
    @ExcelProperty(value = "手机号码")
    private String phonenumber;

    /**
     * user
     */
    @ExcelProperty(value = "用户性别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_user_sex")
    private String sex;

    /**
     * Status (0 normal 1 disabled)
     */
    @ExcelProperty(value = "帐号状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

}
