/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config.properties;

import com.ruoyi.common.enums.CaptchaCategory;
import com.ruoyi.common.enums.CaptchaType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * configurationproperty
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    /**
     *
     */
    private CaptchaType type;

    /**
     *
     */
    private CaptchaCategory category;

    /**
     *
     */
    private Integer numberLength;

    /**
     *
     */
    private Integer charLength;
}
