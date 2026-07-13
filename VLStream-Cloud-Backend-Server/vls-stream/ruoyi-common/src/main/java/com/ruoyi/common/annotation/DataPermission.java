/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限组
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    DataColumn[] value();

}
