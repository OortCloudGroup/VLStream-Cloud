/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.enums;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import com.ruoyi.common.captcha.UnsignedMathGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum CaptchaType {

    /**
     *
     */
    MATH(UnsignedMathGenerator.class),

    /**
     *
     */
    CHAR(RandomGenerator.class);

    private final Class<? extends CodeGenerator> clazz;
}
