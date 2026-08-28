/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.enums;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum CaptchaCategory {

    /**
     *
     */
    LINE(LineCaptcha.class),

    /**
     *
     */
    CIRCLE(CircleCaptcha.class),

    /**
     *
     */
    SHEAR(ShearCaptcha.class);

    private final Class<? extends AbstractCaptcha> clazz;
}
