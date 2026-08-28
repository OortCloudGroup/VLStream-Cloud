/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * userobjectExport VO
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
public class SysUserExportVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * user ID
     */
    @ExcelProperty(value = "用户序号")
    private Long userId;

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

    /**
     * after IP
     */
    @ExcelProperty(value = "最后登录IP")
    private String loginIp;

    /**
     * after
     */
    @ExcelProperty(value = "最后登录时间")
    private Date loginDate;

    /**
     * department name
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;

    /**
     *
     */
    @ExcelProperty(value = "部门负责人")
    private String leader;

}
