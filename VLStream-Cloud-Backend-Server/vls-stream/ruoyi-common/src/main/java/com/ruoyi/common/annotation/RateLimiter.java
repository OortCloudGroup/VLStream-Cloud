/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.annotation;

import com.ruoyi.common.enums.LimitType;

import java.lang.annotation.*;

/**
 *
 *
 * @author Lion Li
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /**
     * key, Spring el Get method parameter value
     * #code.id #{#code}
     */
    String key() default "";

    /**
     * ,
     */
    int time() default 60;

    /**
     *
     */
    int count() default 100;

    /**
     *
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * prompt / tip to {code}
     */
    String message() default "{rate.limiter.message}";
}
