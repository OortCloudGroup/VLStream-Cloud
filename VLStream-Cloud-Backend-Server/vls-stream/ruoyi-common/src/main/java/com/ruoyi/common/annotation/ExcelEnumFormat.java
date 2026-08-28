/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.annotation;

import java.lang.annotation.*;

/**
 * Format
 *
 * @author Liang
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelEnumFormat {

    /**
     * dict
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * dict in codeproperty , to code
     */
    String codeField() default "code";

    /**
     * dict in textproperty , to text
     */
    String textField() default "text";

}
