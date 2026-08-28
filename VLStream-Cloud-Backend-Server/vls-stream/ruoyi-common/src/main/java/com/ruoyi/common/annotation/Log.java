/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.annotation;

import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.OperatorType;

import java.lang.annotation.*;

/**
 * Customoperationlogrecord
 *
 * @author ruoyi
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     *
     */
    String title() default "";

    /**
     * can
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * operation
     */
    OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * whether parameter
     */
    boolean isSaveRequestData() default true;

    /**
     * whether parameter
     */
    boolean isSaveResponseData() default true;

    /**
     * parameter
     */
    String[] excludeParamNames() default {};

}
