/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.annotation;

import com.ruoyi.common.utils.StringUtils;

import java.lang.annotation.*;

/**
 * dictFormat
 *
 * @author Lion Li
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelDictFormat {

    /**
     * if is dict type, Set dict type value ( : sys_user_sex)
     */
    String dictType() default "";

    /**
     * ( : 0= ,1= ,2= not )
     */
    String readConverterExp() default "";

    /**
     * ,
     */
    String separator() default StringUtils.SEPARATOR;

}
