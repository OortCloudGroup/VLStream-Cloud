/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * record sys_logininfor
 *
 * @author Lion Li
 */

@Data
@TableName("sys_logininfor")
@ExcelIgnoreUnannotated
public class SysLogininfor implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "序号")
    @TableId(value = "info_id")
    private Long infoId;

    /**
     * user
     */
    @ExcelProperty(value = "用户账号")
    private String userName;

    /**
     * 0successfully 1failed
     */
    @ExcelProperty(value = "登录状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private String status;

    /**
     * IP
     */
    @ExcelProperty(value = "登录地址")
    private String ipaddr;

    /**
     *
     */
    @ExcelProperty(value = "登录地点")
    private String loginLocation;

    /**
     *
     */
    @ExcelProperty(value = "浏览器")
    private String browser;

    /**
     * operation
     */
    @ExcelProperty(value = "操作系统")
    private String os;

    /**
     * prompt / tip
     */
    @ExcelProperty(value = "提示消息")
    private String msg;

    /**
     *
     */
    @ExcelProperty(value = "访问时间")
    private Date loginTime;

    /**
     * parameter
     */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

}
