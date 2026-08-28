/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * operationlogrecord oper_log
 *
 * @author Lion Li
 */

@Data
@TableName("sys_oper_log")
@ExcelIgnoreUnannotated
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * logprimary key
     */
    @ExcelProperty(value = "日志主键")
    @TableId(value = "oper_id")
    private Long operId;

    /**
     * operation
     */
    @ExcelProperty(value = "操作模块")
    private String title;

    /**
     * (0 1Add 2Update 3Delete )
     */
    @ExcelProperty(value = "业务类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_oper_type")
    private Integer businessType;

    /**
     * array
     */
    @TableField(exist = false)
    private Integer[] businessTypes;

    /**
     * method
     */
    @ExcelProperty(value = "请求方法")
    private String method;

    /**
     *
     */
    @ExcelProperty(value = "请求方式")
    private String requestMethod;

    /**
     * operation (0 1 after user 2 user)
     */
    @ExcelProperty(value = "操作类别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=其它,1=后台用户,2=手机端用户")
    private Integer operatorType;

    /**
     * operation
     */
    @ExcelProperty(value = "操作人员")
    private String operName;

    /**
     * department name
     */
    @ExcelProperty(value = "部门名称")
    private String deptName;

    /**
     * url
     */
    @ExcelProperty(value = "请求地址")
    private String operUrl;

    /**
     * operation
     */
    @ExcelProperty(value = "操作地址")
    private String operIp;

    /**
     * operation
     */
    @ExcelProperty(value = "操作地点")
    private String operLocation;

    /**
     * parameter
     */
    @ExcelProperty(value = "请求参数")
    private String operParam;

    /**
     * parameter
     */
    @ExcelProperty(value = "返回参数")
    private String jsonResult;

    /**
     * operation (0 1 )
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private Integer status;

    /**
     *
     */
    @ExcelProperty(value = "错误消息")
    private String errorMsg;

    /**
     * operation
     */
    @ExcelProperty(value = "操作时间")
    private Date operTime;

    /**
     * parameter
     */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

}
