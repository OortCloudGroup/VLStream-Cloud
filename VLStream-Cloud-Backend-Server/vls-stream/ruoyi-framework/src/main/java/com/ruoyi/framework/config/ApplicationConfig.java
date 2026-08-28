/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * configuration
 *
 * @author Lion Li
 */
@Configuration
// aop object,AopContext can
@EnableAspectJAutoProxy(exposeProxy = true)
public class ApplicationConfig {

}
