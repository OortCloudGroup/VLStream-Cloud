/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.core;

import lombok.Data;

/**
 * 表单属性类（新表单）
 */
@Data
public class WFormConf {

    /**
     * 标题
     */
    private String title;
    /**
     * 表单名
     */
    private String formRef;
    /**
     * 表单模型
     */
    private String formModel;
    /**
     * 表单尺寸
     */
    private String size;
    /**
     * 标签对齐
     */
    private String labelPosition;
    /**
     * 标签宽度
     */
    private Integer labelWidth;
    /**
     * 校验模型
     */
    private String formRules;
    /**
     * 栅格间隔
     */
    private Integer gutter;
    /**
     * 禁用表单
     */
    private Boolean disabled = false;
    /**
     * 栅格占据的列数
     */
    private Integer span;
    /**
     * 表单按钮
     */
    private Boolean formBtns = true;

}
