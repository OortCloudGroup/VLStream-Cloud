/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.core;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * formproperty
 *
 * @author KonBAI
 * @createTime 2022/8/6 18:54
 */
@Data
public class FormConf {

    /**
     *
     */
    private String title;
    /**
     * form
     */
    private String formRef;
    /**
     * formmodel
     */
    private String formModel;
    /**
     * form
     */
    private String size;
    /**
     *
     */
    private String labelPosition;
    /**
     *
     */
    private Integer labelWidth;
    /**
     * Validate model
     */
    private String formRules;
    /**
     *
     */
    private Integer gutter;
    /**
     * form
     */
    private Boolean disabled = false;
    /**
     *
     */
    private Integer span;
    /**
     * formbutton
     */
    private Boolean formBtns = true;
    /**
     * form item
     */
    private List<Map<String, Object>> fields;

}
