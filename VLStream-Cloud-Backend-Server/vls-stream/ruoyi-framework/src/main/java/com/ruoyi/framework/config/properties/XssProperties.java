/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * xss configurationproperty
 *
 * @author Lion Li
 */
@Data
@Component
@ConfigurationProperties(prefix = "xss")
public class XssProperties {

    /**
     *
     */
    private String enabled;

    /**
     * ( )
     */
    private String excludes;

    /**
     *
     */
    private String urlPatterns;

}
