/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ruoyi.common.translation.handler.TranslationHandler;

import java.lang.annotation.*;

/**
 *
 *
 * @author Lion Li
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = TranslationHandler.class)
public @interface Translation {

    /**
     * ( and {@link com.ruoyi.common.annotation.TranslationType} type )
     * <p>
     * current field value if Set @{@link Translation#mapper()} field value
     */
    String type();

    /**
     * field (if is empty field value )
     */
    String mapper() default "";

    /**
     * : dicttype(sys_user_sex)
     */
    String other() default "";

}
